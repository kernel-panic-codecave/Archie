package com.withertech.archie.data.client.model

import com.google.common.base.Preconditions
import com.google.gson.*
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.datafixers.util.Either
import com.mojang.math.Transformation
import com.mojang.serialization.Codec
import com.mojang.serialization.JsonOps
import com.mojang.serialization.codecs.RecordCodecBuilder
import com.withertech.archie.data.util.TransformationHelper
import dev.architectury.platform.Platform
import net.minecraft.client.renderer.block.model.*
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.ExtraCodecs
import net.minecraft.util.GsonHelper
import net.minecraft.util.Mth
import net.minecraft.world.item.ItemDisplayContext
import org.joml.Quaternionf
import org.joml.Vector3f
import java.lang.reflect.Type
import java.util.*
import java.util.function.*
import java.util.function.Function
import java.util.stream.Collectors
import kotlin.Any
import kotlin.Boolean
import kotlin.Char
import kotlin.Float
import kotlin.FloatArray
import kotlin.IllegalArgumentException
import kotlin.Int
import kotlin.NullPointerException
import kotlin.Number
import kotlin.String
import kotlin.Suppress
import kotlin.Throws
import kotlin.Unit
import kotlin.apply
import kotlin.checkNotNull
import kotlin.floatArrayOf
import kotlin.toString

@Suppress("unused")
open class ModelBuilder<T : ModelBuilder<T>>(location: ResourceLocation) : ModelFile(location)
{
	protected var parent: ModelFile? = null
	protected val textures: MutableMap<String, String> = linkedMapOf()
	protected val transforms: TransformsBuilder = TransformsBuilder()

	protected var renderType: String? = null
	protected var ambientOcclusion: Boolean = true
	protected var guiLight: BlockModel.GuiLight? = null

	protected val elements: MutableList<ElementBuilder> = mutableListOf()

	protected var customLoader: CustomLoaderBuilder<T>? = null

	private val rootTransforms: RootTransformsBuilder = RootTransformsBuilder()

	@Suppress("UNCHECKED_CAST")
	private val self: T
		get() = this as T

	operator fun invoke(block: ModelBuilder<T>.() -> Unit) = apply(block)

	fun parent(parent: ModelFile): T
	{
		Preconditions.checkNotNull(parent, "Parent must not be null")
		this.parent = parent
		return self
	}

	fun texture(key: String, texture: String): T
	{
		Preconditions.checkNotNull(key, "Key must not be null")
		Preconditions.checkNotNull(texture, "Texture must not be null")
		if (texture[0] == '#')
		{
			textures[key] = texture
			return self
		} else
		{
			val asLoc: ResourceLocation = if (texture.contains(":"))
			{
				ResourceLocation(texture)
			} else
			{
				ResourceLocation(location.namespace, texture)
			}
			return texture(key, asLoc)
		}
	}

	fun texture(key: String, texture: ResourceLocation): T
	{
		Preconditions.checkNotNull(key, "Key must not be null")
		Preconditions.checkNotNull(texture, "Texture must not be null")
		textures[key] = texture.toString()
		return self
	}

	fun renderType(renderType: String): T
	{
		Preconditions.checkNotNull(renderType, "Render type must not be null")
		return renderType(ResourceLocation(renderType))
	}

	fun renderType(renderType: ResourceLocation): T
	{
		Preconditions.checkNotNull(renderType, "Render type must not be null")
		this.renderType = renderType.toString()
		return self
	}

	fun transforms(block: TransformsBuilder.() -> Unit = {}): TransformsBuilder
	{
		return transforms.apply(block)
	}

	fun ao(ao: Boolean): T
	{
		this.ambientOcclusion = ao
		return self
	}

	fun guiLight(light: BlockModel.GuiLight?): T
	{
		this.guiLight = light
		return self
	}

	fun element(block: ElementBuilder.() -> Unit = {}): ElementBuilder
	{
		Preconditions.checkState(
			customLoader == null || customLoader!!.allowInlineElements,
			"Custom model loader %s does not support inline elements",
			customLoader?.loaderId
		)
		val ret = ElementBuilder().apply(block)
		elements.add(ret)
		return ret
	}

	fun element(index: Int, block: ElementBuilder.() -> Unit = {}): ElementBuilder
	{
		Preconditions.checkState(
			customLoader == null || customLoader!!.allowInlineElements,
			"Custom model loader %s does not support inline elements",
			customLoader?.loaderId
		)
		Preconditions.checkElementIndex(index, elements.size, "Element index")
		return elements[index].apply(block)
	}

	/**
	 * {@return the number of elements in this model builder}
	 */
	fun getElementCount(): Int
	{
		return elements.size
	}

	fun <L : CustomLoaderBuilder<T>?> customLoader(customLoaderFactory: Function<T, L>): L
	{
		check(Platform.isForgeLike()) { "Custom Loader only supported on forge like loaders" }
		val customLoader = customLoaderFactory.apply(self)!!
		Preconditions.checkState(
			customLoader.allowInlineElements || elements.isEmpty(),
			"Custom model loader %s does not support inline elements",
			customLoader.loaderId
		)
		this.customLoader = customLoader
		return customLoader
	}

	fun rootTransforms(block: RootTransformsBuilder.() -> Unit = {}): RootTransformsBuilder
	{
		return rootTransforms.apply(block)
	}

	private fun BlockElement.uvsByFace(face: Direction): FloatArray
	{
		when (face)
		{
			Direction.DOWN ->
			{
				return floatArrayOf(this.from.x(), 16.0f - this.to.z(), this.to.x(), 16.0f - this.from.z())
			}

			Direction.UP ->
			{
				return floatArrayOf(this.from.x(), this.from.z(), this.to.x(), this.to.z())
			}

			Direction.SOUTH ->
			{
				return floatArrayOf(this.from.x(), 16.0f - this.to.y(), this.to.x(), 16.0f - this.from.y())
			}

			Direction.WEST ->
			{
				return floatArrayOf(this.from.z(), 16.0f - this.to.y(), this.to.z(), 16.0f - this.from.y())
			}

			Direction.EAST ->
			{
			}

			else ->
			{
				return floatArrayOf(
					16.0f - this.to.x(),
					16.0f - this.to.y(),
					16.0f - this.from.x(),
					16.0f - this.from.y()
				)
			}
		}
		return floatArrayOf(16.0f - this.to.z(), 16.0f - this.to.y(), 16.0f - this.from.z(), 16.0f - this.from.y())
	}

	open fun toJson(): JsonObject
	{
		val root = JsonObject()

		if (this.parent != null)
		{
			root.addProperty("parent", parent.toString())
		}

		if (!this.ambientOcclusion)
		{
			root.addProperty("ambientocclusion", this.ambientOcclusion)
		}

		if (this.guiLight != null)
		{
			root.addProperty("gui_light", this.guiLight!!.name)
		}

		if (this.renderType != null)
		{
			root.addProperty("render_type", this.renderType)
		}

		val transforms: Map<ItemDisplayContext, PlatformItemTransform> =
			this.transforms.build()
		if (!transforms.isEmpty())
		{
			val display = JsonObject()
			for ((key, vec) in transforms)
			{
				val transform = JsonObject()
				if (vec == PlatformItemTransform.NO_TRANSFORM) continue
				val hasRightRotation: Boolean =
					vec.rightRotation!! != PlatformItemTransform.Deserializer.DEFAULT_ROTATION
				if (vec.translation != PlatformItemTransform.Deserializer.DEFAULT_TRANSLATION)
				{
					transform.add("translation", serializeVector3f(vec.translation))
				}
				if (vec.rotation != PlatformItemTransform.Deserializer.DEFAULT_ROTATION)
				{
					transform.add(
						if (hasRightRotation) "left_rotation" else "rotation",
						serializeVector3f(vec.rotation)
					)
				}
				if (vec.scale != PlatformItemTransform.Deserializer.DEFAULT_SCALE)
				{
					transform.add("scale", serializeVector3f(vec.scale))
				}
				if (hasRightRotation)
				{
					transform.add("right_rotation", serializeVector3f(vec.rightRotation!!))
				}
				display.add(key.serializedName, transform)
			}
			root.add("display", display)
		}

		if (textures.isNotEmpty())
		{
			val textures = JsonObject()
			for ((key, value) in this.textures)
			{
				textures.addProperty(key, serializeLocOrKey(value))
			}
			root.add("textures", textures)
		}

		if (this.elements.isNotEmpty())
		{
			val elements = JsonArray()
			this.elements.stream()
				.map { obj: ElementBuilder -> obj.build() }
				.forEach { part: BlockElement ->
					val partObj = JsonObject()
					partObj.add("from", serializeVector3f(part.from))
					partObj.add("to", serializeVector3f(part.to))

					if (part.rotation != null)
					{
						val rotation = JsonObject()
						rotation.add("origin", serializeVector3f(part.rotation.origin()))
						rotation.addProperty("axis", part.rotation.axis().serializedName)
						rotation.addProperty("angle", part.rotation.angle())
						if (part.rotation.rescale())
						{
							rotation.addProperty("rescale", part.rotation.rescale())
						}
						partObj.add("rotation", rotation)
					}

					if (!part.shade)
					{
						partObj.addProperty("shade", part.shade)
					}

					if (part is PlatformBlockElement)
					{
						if (part.faceData is PlatformFaceData.ExtraFaceData && part.faceData != PlatformFaceData.ExtraFaceData.DEFAULT)
						{
							partObj.add(
								"neoforge_data",
								PlatformFaceData.ExtraFaceData.CODEC.encodeStart(
									JsonOps.INSTANCE,
									part.faceData
								).result().get()
							)
						}
					}

					val faces = JsonObject()
					for (dir in Direction.entries)
					{
						val face = part.faces[dir] ?: continue

						val faceObj = JsonObject()
						faceObj.addProperty("texture", serializeLocOrKey(face.texture))
						if (!face.uv.uvs.contentEquals(part.uvsByFace(dir)))
						{
							faceObj.add("uv", Gson().toJsonTree(face.uv.uvs))
						}
						if (face.cullForDirection != null)
						{
							faceObj.addProperty("cullface", face.cullForDirection.serializedName)
						}
						if (face.uv.rotation != 0)
						{
							faceObj.addProperty("rotation", face.uv.rotation)
						}
						if (face.tintIndex != -1)
						{
							faceObj.addProperty("tintindex", face.tintIndex)
						}
						if (face is PlatformBlockElementFace)
						{
							when
							{
								face.faceData is PlatformFaceData.ExtraFaceData && face.faceData != PlatformFaceData.ExtraFaceData.DEFAULT ->
								{
									faceObj.add(
										"neoforge_data",
										PlatformFaceData.ExtraFaceData.CODEC.encodeStart(
											JsonOps.INSTANCE,
											face.faceData
										).result().get()
									)
								}

								face.faceData is PlatformFaceData.ForgeFaceData && face.faceData != PlatformFaceData.ForgeFaceData.DEFAULT ->
								{
									faceObj.add(
										"forge_data",
										PlatformFaceData.ForgeFaceData.CODEC.encodeStart(
											JsonOps.INSTANCE,
											face.faceData
										).result().get()
									)
								}
							}
						}

						faces.add(dir.serializedName, faceObj)
					}
					if (!part.faces.isEmpty())
					{
						partObj.add("faces", faces)
					}
					elements.add(partObj)
				}
			root.add("elements", elements)
		}

		// If there were any transform properties set, add them to the output.
		val transform: JsonObject = rootTransforms.toJson()
		if (transform.size() > 0)
		{
			root.add("transform", transform)
		}

		return customLoader?.toJson(root) ?: root
	}

	private fun serializeLocOrKey(tex: String): String
	{
		if (tex[0] == '#')
		{
			return tex
		}
		return ResourceLocation(tex).toString()
	}

	private fun serializeVector3f(vec: Vector3f): JsonArray
	{
		val ret = JsonArray()
		ret.add(serializeFloat(vec.x()))
		ret.add(serializeFloat(vec.y()))
		ret.add(serializeFloat(vec.z()))
		return ret
	}

	private fun serializeFloat(f: Float): Number
	{
		if (f.toInt().toFloat() == f)
		{
			return f.toInt()
		}
		return f
	}

	inner class ElementBuilder
	{
		private var from = Vector3f()
		private var to = Vector3f(16f, 16f, 16f)
		private val faces: MutableMap<Direction, FaceBuilder> = LinkedHashMap()
		private var rotation: RotationBuilder? = null
		private var shade = true
		private var color = -0x1
		private var blockLight = 0
		private var skyLight = 0
		private var hasAmbientOcclusion = true

		private fun validateCoordinate(coord: Float, name: Char)
		{
			Preconditions.checkArgument(
				!(coord < -16.0f) && !(coord > 32.0f),
				"Position $name out of range, must be within [-16, 32]. Found: %d", coord
			)
		}

		private fun validatePosition(pos: Vector3f)
		{
			validateCoordinate(pos.x(), 'x')
			validateCoordinate(pos.y(), 'y')
			validateCoordinate(pos.z(), 'z')
		}

		fun from(x: Float, y: Float, z: Float): ElementBuilder
		{
			this.from = Vector3f(x, y, z)
			validatePosition(this.from)
			return this
		}

		fun to(x: Float, y: Float, z: Float): ElementBuilder
		{
			this.to = Vector3f(x, y, z)
			validatePosition(this.to)
			return this
		}

		fun face(dir: Direction, block: FaceBuilder.() -> Unit = {}): FaceBuilder
		{
			Preconditions.checkNotNull(dir, "Direction must not be null")
			return faces.computeIfAbsent(
				dir
			) { dir: Direction ->
				FaceBuilder(
					dir
				)
			}.apply(block)
		}

		fun rotation(block: RotationBuilder.() -> Unit = {}): RotationBuilder
		{
			if (this.rotation == null)
			{
				this.rotation = RotationBuilder()
			}
			return this.rotation!!.apply(block)
		}

		fun shade(shade: Boolean): ElementBuilder
		{
			this.shade = shade
			return this
		}

		fun allFaces(action: BiConsumer<Direction, FaceBuilder>): ElementBuilder
		{
			Arrays.stream(Direction.entries.toTypedArray())
				.forEach { d: Direction ->
					action.accept(
						d,
						face(d)
					)
				}
			return this
		}

		fun faces(action: BiConsumer<Direction, FaceBuilder>): ElementBuilder
		{
			faces.entries.stream()
				.forEach { e: Map.Entry<Direction, FaceBuilder> ->
					action.accept(
						e.key,
						e.value
					)
				}
			return this
		}

		fun textureAll(texture: String): ElementBuilder
		{
			return allFaces(addTexture(texture))
		}

		fun texture(texture: String): ElementBuilder
		{
			return faces(addTexture(texture))
		}

		fun cube(texture: String): ElementBuilder
		{
			return allFaces(addTexture(texture).andThen { dir: Direction?, f: FaceBuilder ->
				f.cullface(
					dir
				)
			})
		}

		fun emissivity(blockLight: Int, skyLight: Int): ElementBuilder
		{
			this.blockLight = blockLight
			this.skyLight = skyLight
			return this
		}

		fun color(color: Int): ElementBuilder
		{
			this.color = color
			return this
		}

		fun ao(ao: Boolean): ElementBuilder
		{
			this.hasAmbientOcclusion = ao
			return this
		}

		private fun addTexture(texture: String): BiConsumer<Direction, FaceBuilder>
		{
			return BiConsumer { `$`: Direction?, f: FaceBuilder ->
				f.texture(
					texture
				)
			}
		}

		fun build(): BlockElement
		{
			val faces: Map<Direction, BlockElementFace> =
				faces.entries.stream()
					.collect(
						Collectors.toMap(
							{ it.key },
							{ e: Map.Entry<Direction?, FaceBuilder> -> e.value.build() },
							{ k1: BlockElementFace?, k2: BlockElementFace? ->
								throw IllegalArgumentException()
							},
							{ LinkedHashMap() })
					)
			return PlatformBlockElement(
				from, to, faces, if (rotation == null) null else rotation!!.build(), shade, when
				{
					Platform.isNeoForge() ->
						PlatformFaceData.ExtraFaceData(
							color = color,
							blockLight = blockLight,
							skyLight = skyLight,
							ambientOcclusion = hasAmbientOcclusion
						)

					Platform.isMinecraftForge() ->
						PlatformFaceData.ForgeFaceData(
							color = color,
							blockLight = blockLight,
							skyLight = skyLight,
							ambientOcclusion = hasAmbientOcclusion
						)

					else ->
						PlatformFaceData.None
				}
			)
		}


		fun end(): T
		{
			return self
		}

		inner class FaceBuilder internal constructor(dir: Direction)
		{
			private var cullface: Direction? = null
			private var tintindex = -1
			private var texture: String? = MissingTextureAtlasSprite.getLocation().toString()
			private lateinit var uvs: FloatArray
			private var rotation: FaceRotation = FaceRotation.ZERO
			private var color = -0x1
			private var blockLight = 0
			private var skyLight = 0
			private var hasAmbientOcclusion = true

			fun cullface(dir: Direction?): FaceBuilder
			{
				this.cullface = dir
				return this
			}

			fun tintindex(index: Int): FaceBuilder
			{
				this.tintindex = index
				return this
			}

			fun texture(texture: String): FaceBuilder
			{
				Preconditions.checkNotNull(texture, "Texture must not be null")
				this.texture = texture
				return this
			}

			fun uvs(u1: Float, v1: Float, u2: Float, v2: Float): FaceBuilder
			{
				this.uvs = floatArrayOf(u1, v1, u2, v2)
				return this
			}

			fun rotation(rot: FaceRotation): FaceBuilder
			{
				Preconditions.checkNotNull(rot, "Rotation must not be null")
				this.rotation = rot
				return this
			}

			fun emissivity(
				blockLight: Int,
				skyLight: Int
			): FaceBuilder
			{
				this.blockLight = blockLight
				this.skyLight = skyLight
				return this
			}

			fun color(color: Int): FaceBuilder
			{
				this.color = color
				return this
			}

			fun ao(ao: Boolean): FaceBuilder
			{
				this.hasAmbientOcclusion = ao
				return this
			}

			fun build(): BlockElementFace
			{
				checkNotNull(this.texture) { "A model face must have a texture" }
				return PlatformBlockElementFace(
					cullface, tintindex, texture!!, BlockFaceUV(uvs, rotation.rotation), when
					{
						Platform.isNeoForge() ->
							PlatformFaceData.ExtraFaceData(
								color = color,
								blockLight = blockLight,
								skyLight = skyLight,
								ambientOcclusion = hasAmbientOcclusion
							)

						Platform.isMinecraftForge() ->
							PlatformFaceData.ForgeFaceData(
								color = color,
								blockLight = blockLight,
								skyLight = skyLight,
								ambientOcclusion = hasAmbientOcclusion
							)

						else ->
							PlatformFaceData.None
					}
				)
			}

			fun end(): ElementBuilder
			{
				return this@ElementBuilder
			}
		}

		inner class RotationBuilder
		{
			private lateinit var origin: Vector3f
			private lateinit var axis: Direction.Axis
			private var angle = 0f
			private var rescale = false

			fun origin(x: Float, y: Float, z: Float): RotationBuilder
			{
				this.origin = Vector3f(x, y, z)
				return this
			}

			/**
			 * @param axis the axis of rotation
			 * @return this builder
			 * @throws NullPointerException if `axis` is `null`
			 */
			fun axis(axis: Direction.Axis): RotationBuilder
			{
				Preconditions.checkNotNull(axis, "Axis must not be null")
				this.axis = axis
				return this
			}

			/**
			 * @param angle the rotation angle
			 * @return this builder
			 * @throws IllegalArgumentException if `angle` is invalid (not one of 0, +/-22.5, +/-45)
			 */
			fun angle(angle: Float): RotationBuilder
			{
				// Same logic from BlockPart.Deserializer#parseAngle
				Preconditions.checkArgument(
					angle == 0.0f || Mth.abs(angle) == 22.5f || Mth.abs(
						angle
					) == 45.0f, "Invalid rotation %f found, only -45/-22.5/0/22.5/45 allowed", angle
				)
				this.angle = angle
				return this
			}

			fun rescale(rescale: Boolean): RotationBuilder
			{
				this.rescale = rescale
				return this
			}

			fun build(): BlockElementRotation
			{
				return BlockElementRotation(origin, axis, angle, rescale)
			}

			fun end(): ElementBuilder
			{
				return this@ElementBuilder
			}
		}
	}

	enum class FaceRotation(val rotation: Int)
	{
		ZERO(0),
		CLOCKWISE_90(90),
		UPSIDE_DOWN(180),
		COUNTERCLOCKWISE_90(270),
	}

	class PlatformBlockElement(
		from: Vector3f, to: Vector3f, faces: Map<Direction, BlockElementFace>,
		rotation: BlockElementRotation?, shade: Boolean, val faceData: PlatformFaceData
	) : BlockElement(
		from, to,
		faces, rotation, shade
	)

	class PlatformBlockElementFace(
		cullForDirection: Direction?,
		tintIndex: Int, texture: String, uv: BlockFaceUV, val faceData: PlatformFaceData
	) : BlockElementFace(cullForDirection, tintIndex, texture, uv)

	sealed class PlatformFaceData
	{
		data object None : PlatformFaceData()
		data class ExtraFaceData(
			val color: Int,
			val blockLight: Int,
			val skyLight: Int,
			val ambientOcclusion: Boolean
		) : PlatformFaceData()
		{
			companion object
			{
				val DEFAULT: ExtraFaceData = ExtraFaceData(-0x1, 0, 0, true)

				val COLOR: Codec<Int> = Codec.either(Codec.INT, Codec.STRING).xmap(
					{ either: Either<Int, String> ->
						either.map(
							Function.identity()
						) { str: String ->
							str.toLong(16).toInt()
						}
					},
					{ color: Int? ->
						Either.right(
							Integer.toHexString(
								color!!
							)
						)
					})

				val CODEC: Codec<ExtraFaceData> =
					RecordCodecBuilder.create { builder: RecordCodecBuilder.Instance<ExtraFaceData> ->
						builder
							.group(
								COLOR.optionalFieldOf("color", -0x1).forGetter(ExtraFaceData::color),
								Codec.intRange(0, 15).optionalFieldOf("block_light", 0)
									.forGetter(ExtraFaceData::blockLight),
								Codec.intRange(0, 15).optionalFieldOf("sky_light", 0)
									.forGetter(ExtraFaceData::skyLight),
								Codec.BOOL.optionalFieldOf("ambient_occlusion", true)
									.forGetter(ExtraFaceData::ambientOcclusion)
							)
							.apply(
								builder
							) { color: Int, blockLight: Int, skyLight: Int, ambientOcclusion: Boolean ->
								ExtraFaceData(
									color,
									blockLight,
									skyLight,
									ambientOcclusion
								)
							}
					}
			}
		}

		data class ForgeFaceData(
			val color: Int,
			val blockLight: Int,
			val skyLight: Int,
			val ambientOcclusion: Boolean,
			val calculateNormals: Boolean
		) : PlatformFaceData()
		{
			constructor(color: Int, blockLight: Int, skyLight: Int, ambientOcclusion: Boolean) : this(
				color,
				blockLight,
				skyLight,
				ambientOcclusion,
				false
			)

			companion object
			{
				val DEFAULT: ForgeFaceData = ForgeFaceData(-0x1, 0, 0, true, false)

				val COLOR: Codec<Int> = Codec.either(Codec.INT, Codec.STRING).xmap(
					{ either: Either<Int, String> ->
						either.map(
							Function.identity()
						) { str: String ->
							str.toLong(16).toInt()
						}
					},
					{ color: Int? ->
						Either.right(
							Integer.toHexString(
								color!!
							)
						)
					})

				val CODEC: Codec<ForgeFaceData> =
					RecordCodecBuilder.create { builder: RecordCodecBuilder.Instance<ForgeFaceData> ->
						builder.group(
							COLOR.optionalFieldOf("color", -0x1).forGetter(ForgeFaceData::color),
							Codec.intRange(0, 15).optionalFieldOf("block_light", 0)
								.forGetter(ForgeFaceData::blockLight),
							Codec.intRange(0, 15).optionalFieldOf("sky_light", 0)
								.forGetter(ForgeFaceData::skyLight),
							Codec.BOOL.optionalFieldOf("ambient_occlusion", true)
								.forGetter(ForgeFaceData::ambientOcclusion),
							Codec.BOOL.optionalFieldOf("calculate_normals", false)
								.forGetter(ForgeFaceData::calculateNormals)
						)
							.apply(
								builder
							) { color: Int, blockLight: Int, skyLight: Int, ambientOcclusion: Boolean, calculateNormals: Boolean ->
								ForgeFaceData(
									color,
									blockLight,
									skyLight,
									ambientOcclusion,
									calculateNormals
								)
							}
					}
			}
		}
	}

	inner class TransformsBuilder
	{
		private val transforms: MutableMap<ItemDisplayContext, TransformVecBuilder> = LinkedHashMap()

		/**
		 * Begin building a new transform for the given perspective.
		 *
		 * @param type the perspective to create or return the builder for
		 * @return the builder for the given perspective
		 * @throws NullPointerException if `type` is `null`
		 */
		fun transform(type: ItemDisplayContext): TransformVecBuilder
		{
			Preconditions.checkNotNull(type, "Perspective cannot be null")
			return transforms.computeIfAbsent(
				type
			) { type: ItemDisplayContext? ->
				TransformVecBuilder(
					type
				)
			}
		}

		fun build(): Map<ItemDisplayContext, PlatformItemTransform>
		{
			return transforms.entries.stream()
				.collect(
					Collectors.toMap(
						{ it.key },
						{ e: Map.Entry<ItemDisplayContext?, TransformVecBuilder> -> e.value.build() },
						{ k1: PlatformItemTransform?, k2: PlatformItemTransform? ->
							throw java.lang.IllegalArgumentException()
						},
						{ LinkedHashMap() })
				)
		}

		fun end(): T
		{
			return self
		}

		inner class TransformVecBuilder internal constructor(type: ItemDisplayContext?)
		{
			private var rotation = Vector3f(PlatformItemTransform.Deserializer.DEFAULT_ROTATION)
			private var translation = Vector3f(PlatformItemTransform.Deserializer.DEFAULT_TRANSLATION)
			private var scale = Vector3f(PlatformItemTransform.Deserializer.DEFAULT_SCALE)
			private var rightRotation = Vector3f(PlatformItemTransform.Deserializer.DEFAULT_ROTATION)

			fun rotation(x: Float, y: Float, z: Float): TransformVecBuilder
			{
				this.rotation = Vector3f(x, y, z)
				return this
			}

			fun leftRotation(x: Float, y: Float, z: Float): TransformVecBuilder
			{
				return rotation(x, y, z)
			}

			fun translation(x: Float, y: Float, z: Float): TransformVecBuilder
			{
				this.translation = Vector3f(x, y, z)
				return this
			}

			fun scale(sc: Float): TransformVecBuilder
			{
				return scale(sc, sc, sc)
			}

			fun scale(x: Float, y: Float, z: Float): TransformVecBuilder
			{
				this.scale = Vector3f(x, y, z)
				return this
			}

			fun rightRotation(
				x: Float,
				y: Float,
				z: Float
			): TransformVecBuilder
			{
				this.rightRotation = Vector3f(x, y, z)
				return this
			}

			fun build(): PlatformItemTransform
			{
				return PlatformItemTransform(rotation, translation, scale, rightRotation)
			}

			fun end(): TransformsBuilder
			{
				return this@TransformsBuilder
			}
		}
	}

	class PlatformItemTransform(rotation: Vector3f, translation: Vector3f, scale: Vector3f, rightRotation: Vector3f)
	{
		val rotation: Vector3f = Vector3f(rotation)
		val translation: Vector3f = Vector3f(translation)
		val scale: Vector3f = Vector3f(scale)
		val rightRotation: Vector3f = Vector3f(rightRotation)

		constructor(rotation: Vector3f, translation: Vector3f, scale: Vector3f) : this(
			rotation,
			translation,
			scale,
			Vector3f()
		)

		fun apply(leftHand: Boolean, poseStack: PoseStack)
		{
			if (this !== NO_TRANSFORM)
			{
				val f = rotation.x()
				var f1 = rotation.y()
				var f2 = rotation.z()
				if (leftHand)
				{
					f1 = -f1
					f2 = -f2
				}
				val i = if (leftHand) -1 else 1
				poseStack.translate(
					i.toFloat() * translation.x(),
					translation.y(), translation.z()
				)
				poseStack.mulPose(
					Quaternionf().rotationXYZ(
						f * (Math.PI.toFloat() / 180),
						f1 * (Math.PI.toFloat() / 180),
						f2 * (Math.PI.toFloat() / 180)
					)
				)
				poseStack.scale(scale.x(), scale.y(), scale.z())
				poseStack.mulPose(
					TransformationHelper.quatFromXYZ(
						rightRotation.x(),
						rightRotation.y() * (if (leftHand) -1 else 1).toFloat(),
						rightRotation.z() * (if (leftHand) -1 else 1).toFloat(), true
					)
				)
			}
		}

		override fun equals(`object`: Any?): Boolean
		{
			if (this === `object`)
			{
				return true
			}
			if (this.javaClass != `object`?.javaClass)
			{
				return false
			}
			val itemtransform = `object` as PlatformItemTransform
			return this.rotation == itemtransform.rotation && (this.scale == itemtransform.scale) && (this.translation == itemtransform.translation)
		}

		override fun hashCode(): Int
		{
			var i = rotation.hashCode()
			i = 31 * i + translation.hashCode()
			return 31 * i + scale.hashCode()
		}

		class Deserializer protected constructor() : JsonDeserializer<PlatformItemTransform>
		{
			@Throws(JsonParseException::class)
			override fun deserialize(
				json: JsonElement,
				type: Type,
				context: JsonDeserializationContext
			): PlatformItemTransform
			{
				val jsonObject = json.asJsonObject
				val vector3f = this.getVector3f(jsonObject, "rotation", DEFAULT_ROTATION)
				val vector3f2 = this.getVector3f(jsonObject, "translation", DEFAULT_TRANSLATION)
				vector3f2.mul(0.0625f)
				vector3f2[Mth.clamp(vector3f2.x, -5.0f, 5.0f), Mth.clamp(vector3f2.y, -5.0f, 5.0f)] =
					Mth.clamp(vector3f2.z, -5.0f, 5.0f)
				val vector3f3 = this.getVector3f(jsonObject, "scale", DEFAULT_SCALE)
				vector3f3[Mth.clamp(vector3f3.x, -4.0f, 4.0f), Mth.clamp(vector3f3.y, -4.0f, 4.0f)] =
					Mth.clamp(vector3f3.z, -4.0f, 4.0f)
				val rightRotation =
					this.getVector3f(jsonObject, "right_rotation", DEFAULT_ROTATION)
				return PlatformItemTransform(vector3f, vector3f2, vector3f3, rightRotation)
			}

			private fun getVector3f(json: JsonObject, key: String, fallback: Vector3f): Vector3f
			{
				if (!json.has(key))
				{
					return fallback
				}
				val jsonArray = GsonHelper.getAsJsonArray(json, key)
				if (jsonArray.size() != 3)
				{
					throw JsonParseException("Expected 3 " + key + " values, found: " + jsonArray.size())
				}
				val fs = FloatArray(3)
				for (i in fs.indices)
				{
					fs[i] = GsonHelper.convertToFloat(jsonArray[i], "$key[$i]")
				}
				return Vector3f(fs[0], fs[1], fs[2])
			}

			companion object
			{
				val DEFAULT_ROTATION: Vector3f = Vector3f(0.0f, 0.0f, 0.0f)
				val DEFAULT_TRANSLATION: Vector3f = Vector3f(0.0f, 0.0f, 0.0f)
				val DEFAULT_SCALE: Vector3f = Vector3f(1.0f, 1.0f, 1.0f)
				const val MAX_TRANSLATION: Float = 5.0f
				const val MAX_SCALE: Float = 4.0f
			}
		}

		companion object
		{
			val NO_TRANSFORM: PlatformItemTransform =
				PlatformItemTransform(Vector3f(), Vector3f(), Vector3f(1.0f, 1.0f, 1.0f))
		}
	}

	inner class RootTransformsBuilder internal constructor()
	{
		private var translation = Vector3f()
		private var leftRotation = Quaternionf()
		private var rightRotation = Quaternionf()
		private var scale = ONE

		private var origin: TransformationHelper.TransformOrigin? = null
		private var originVec: Vector3f? = null

		/**
		 * Sets the translation of the root transform.
		 *
		 * @param translation the translation
		 * @return this builder
		 * @throws NullPointerException if `translation` is `null`
		 */
		fun translation(translation: Vector3f?): RootTransformsBuilder
		{
			this.translation = Preconditions.checkNotNull(translation, "Translation must not be null")
			return this
		}

		/**
		 * Sets the translation of the root transform.
		 *
		 * @param x x translation
		 * @param y y translation
		 * @param z z translation
		 * @return this builder
		 */
		fun translation(x: Float, y: Float, z: Float): RootTransformsBuilder
		{
			return translation(Vector3f(x, y, z))
		}

		/**
		 * Sets the left rotation of the root transform.
		 *
		 * @param rotation the left rotation
		 * @return this builder
		 * @throws NullPointerException if `rotation` is `null`
		 */
		fun rotation(rotation: Quaternionf?): RootTransformsBuilder
		{
			this.leftRotation = Preconditions.checkNotNull(rotation, "Rotation must not be null")
			return this
		}

		/**
		 * Sets the left rotation of the root transform.
		 *
		 * @param x         x rotation
		 * @param y         y rotation
		 * @param z         z rotation
		 * @param isDegrees whether the rotation is in degrees or radians
		 * @return this builder
		 */
		fun rotation(x: Float, y: Float, z: Float, isDegrees: Boolean): RootTransformsBuilder
		{
			return rotation(TransformationHelper.quatFromXYZ(x, y, z, isDegrees))
		}

		/**
		 * Sets the left rotation of the root transform.
		 *
		 * @param leftRotation the left rotation
		 * @return this builder
		 * @throws NullPointerException if `leftRotation` is `null`
		 */
		fun leftRotation(leftRotation: Quaternionf?): RootTransformsBuilder
		{
			return rotation(leftRotation)
		}

		fun leftRotation(x: Float, y: Float, z: Float, isDegrees: Boolean): RootTransformsBuilder
		{
			return leftRotation(TransformationHelper.quatFromXYZ(x, y, z, isDegrees))
		}

		fun rightRotation(rightRotation: Quaternionf?): RootTransformsBuilder
		{
			this.rightRotation = Preconditions.checkNotNull(rightRotation, "Rotation must not be null")
			return this
		}

		fun rightRotation(x: Float, y: Float, z: Float, isDegrees: Boolean): RootTransformsBuilder
		{
			return rightRotation(
				TransformationHelper.quatFromXYZ(
					x,
					y,
					z,
					isDegrees
				)
			)
		}

		fun postRotation(postRotation: Quaternionf?): RootTransformsBuilder
		{
			return rightRotation(postRotation)
		}

		fun postRotation(x: Float, y: Float, z: Float, isDegrees: Boolean): RootTransformsBuilder
		{
			return postRotation(TransformationHelper.quatFromXYZ(x, y, z, isDegrees))
		}

		fun scale(scale: Float): RootTransformsBuilder
		{
			return scale(Vector3f(scale, scale, scale))
		}

		fun scale(xScale: Float, yScale: Float, zScale: Float): RootTransformsBuilder
		{
			return scale(Vector3f(xScale, yScale, zScale))
		}

		fun scale(scale: Vector3f?): RootTransformsBuilder
		{
			this.scale = Preconditions.checkNotNull(scale, "Scale must not be null")
			return this
		}

		fun transform(transformation: Transformation): RootTransformsBuilder
		{
			Preconditions.checkNotNull(transformation, "Transformation must not be null")
			this.translation = transformation.translation
			this.leftRotation = transformation.leftRotation
			this.rightRotation = transformation.rightRotation
			this.scale = transformation.scale
			return this
		}

		fun origin(origin: Vector3f?): RootTransformsBuilder
		{
			this.originVec = Preconditions.checkNotNull(origin, "Origin must not be null")
			this.origin = null
			return this
		}

		fun origin(origin: TransformationHelper.TransformOrigin?): RootTransformsBuilder
		{
			this.origin =
				Preconditions.checkNotNull<TransformationHelper.TransformOrigin>(
					origin,
					"Origin must not be null"
				)
			this.originVec = null
			return this
		}

		fun end(): ModelBuilder<T>
		{
			return this@ModelBuilder
		}

		fun toJson(): JsonObject
		{
			// Write the transform to an object
			val transform = JsonObject()

			if (!translation.equals(0f, 0f, 0f))
			{
				transform.add("translation", writeVec3(translation))
			}

			if (scale != ONE)
			{
				transform.add("scale", writeVec3(scale))
			}

			if (!leftRotation.equals(0f, 0f, 0f, 1f))
			{
				transform.add("rotation", writeQuaternion(leftRotation))
			}

			if (!rightRotation.equals(0f, 0f, 0f, 1f))
			{
				transform.add("post_rotation", writeQuaternion(rightRotation))
			}

			if (origin != null)
			{
				transform.addProperty("origin", origin!!.getSerializedName())
			} else if (originVec != null && !originVec!!.equals(0f, 0f, 0f))
			{
				transform.add("origin", writeVec3(originVec!!))
			}

			return transform
		}
	}

	companion object
	{
		private val ONE = Vector3f(1f, 1f, 1f)

		private fun writeVec3(vector: Vector3f): JsonArray
		{
			val array = JsonArray()
			array.add(vector.x())
			array.add(vector.y())
			array.add(vector.z())
			return array
		}

		private fun writeQuaternion(quaternion: Quaternionf): JsonArray
		{
			val array = JsonArray()
			array.add(quaternion.x())
			array.add(quaternion.y())
			array.add(quaternion.z())
			array.add(quaternion.w())
			return array
		}
	}


}