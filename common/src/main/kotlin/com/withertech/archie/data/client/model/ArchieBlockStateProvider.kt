package com.withertech.archie.data.client.model

import com.google.common.base.Preconditions
import com.google.gson.*
import com.withertech.archie.Archie
import com.withertech.archie.data.IArchieDataProvider
import dev.architectury.platform.Mod
import net.minecraft.core.Direction
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.data.CachedOutput
import net.minecraft.data.DataProvider
import net.minecraft.data.PackOutput
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.*
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.jetbrains.annotations.VisibleForTesting
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer
import java.util.function.Function
import kotlin.system.exitProcess

@Suppress("MemberVisibilityCanBePrivate", "unused")
abstract class ArchieBlockStateProvider(
	final override val output: PackOutput,
	final override val mod: Mod,
	final override val exitOnError: Boolean
) : IArchieDataProvider
{
	@VisibleForTesting
	protected val registeredBlocks: MutableMap<Block, IGeneratedBlockState> =
		LinkedHashMap<Block, IGeneratedBlockState>()

	private val blockModels: ArchieBlockModelProvider =
		object : ArchieBlockModelProvider(
			output,
			mod,
			exitOnError
		)
		{
			override fun run(cache: CachedOutput): CompletableFuture<*>
			{
				return CompletableFuture.allOf()
			}

			override fun generate() = Unit
		}
	private val itemModels: ArchieItemModelProvider =
		object : ArchieItemModelProvider(
			output,
			mod,
			exitOnError
		)
		{
			override fun run(cache: CachedOutput): CompletableFuture<*>
			{
				return CompletableFuture.allOf()
			}

			override fun generate() = Unit
		}

	override fun run(cache: CachedOutput): CompletableFuture<*>
	{
		blockModels().clear()
		itemModels().clear()
		registeredBlocks.clear()
		runCatching {
			generate()
		}.onFailure {
			Archie.LOGGER.error(
				"Data Provider $name failed with exception: ${it.message}\n" +
						"Stacktrace: ${it.stackTraceToString()}"
			)
			if (exitOnError) exitProcess(-1)
		}
		val futures: Array<CompletableFuture<*>?> = arrayOfNulls(2 + registeredBlocks.size)
		var i = 0
		futures[i++] = blockModels().generateAll(cache)
		futures[i++] = itemModels().generateAll(cache)
		for ((key, value) in registeredBlocks)
		{
			futures[i++] = saveBlockState(cache, value.toJson(), key)
		}
		return CompletableFuture.allOf(*futures)
	}

	protected abstract fun generate()

	fun getVariantBuilder(b: Block, block: VariantBlockStateBuilder.() -> Unit = {}): VariantBlockStateBuilder
	{
		if (registeredBlocks.containsKey(b))
		{
			val old: IGeneratedBlockState? = registeredBlocks[b]
			Preconditions.checkState(old is VariantBlockStateBuilder)
			return (old as VariantBlockStateBuilder).apply(block)
		} else
		{
			val ret = VariantBlockStateBuilder(b).apply(block)
			registeredBlocks[b] = ret
			return ret
		}
	}

	fun getMultipartBuilder(b: Block, block: MultiPartBlockStateBuilder.() -> Unit = {}): MultiPartBlockStateBuilder
	{
		if (registeredBlocks.containsKey(b))
		{
			val old: IGeneratedBlockState? = registeredBlocks[b]
			Preconditions.checkState(old is MultiPartBlockStateBuilder)
			return (old as MultiPartBlockStateBuilder).apply(block)
		} else
		{
			val ret = MultiPartBlockStateBuilder(b).apply(block)
			registeredBlocks[b] = ret
			return ret
		}
	}

	fun blockModels(block: ArchieBlockModelProvider.() -> Unit = {}): ArchieBlockModelProvider
	{
		return runCatching {
			blockModels.apply(block)
		}.onFailure {
			Archie.LOGGER.error(
				"Data Provider $name failed with exception: ${it.message}\n" +
						"Stacktrace: ${it.stackTraceToString()}"
			)
			if (exitOnError) exitProcess(-1)
		}.getOrElse {
			blockModels.clear()
			blockModels
		}
	}

	fun itemModels(block: ArchieItemModelProvider.() -> Unit = {}): ArchieItemModelProvider
	{
		return runCatching {
			itemModels.apply(block)
		}.onFailure {
			Archie.LOGGER.error(
				"Data Provider $name failed with exception: ${it.message}\n" +
						"Stacktrace: ${it.stackTraceToString()}"
			)
			if (exitOnError) exitProcess(-1)
		}.getOrElse {
			itemModels.clear()
			itemModels
		}
	}

	private fun key(block: Block): ResourceLocation
	{
		return BuiltInRegistries.BLOCK.getKey(block)
	}

	private fun name(block: Block): String
	{
		return key(block).path
	}

	fun blockTexture(block: Block): ResourceLocation
	{
		val name = key(block)
		return ResourceLocation(
			name.namespace,
			ArchieModelProvider.BLOCK_FOLDER + "/" + name.path
		)
	}

	private fun extend(rl: ResourceLocation, suffix: String): ResourceLocation
	{
		return ResourceLocation(rl.namespace, rl.path + suffix)
	}

	fun cubeAll(block: Block): ModelFile
	{
		return blockModels().cubeAll(name(block), blockTexture(block))
	}

	fun simpleBlock(
		block: Block,
		expander: Function<ModelFile, Array<ConfiguredModel>>
	)
	{
		simpleBlock(block, *expander.apply(cubeAll(block)))
	}

	@JvmOverloads
	fun simpleBlock(block: Block, model: ModelFile = cubeAll(block))
	{
		simpleBlock(block, ConfiguredModel(model))
	}

	fun simpleBlockItem(block: Block, model: ModelFile)
	{
		itemModels().getBuilder(key(block).path).parent(model)
	}

	fun simpleBlockWithItem(block: Block, model: ModelFile = cubeAll(block))
	{
		simpleBlock(block, model)
		simpleBlockItem(block, model)
	}

	fun simpleBlock(block: Block, vararg models: ConfiguredModel)
	{
		getVariantBuilder(block)
			.partialState().setModels(*models)
	}

	fun logBlock(block: RotatedPillarBlock)
	{
		axisBlock(block, blockTexture(block), extend(blockTexture(block), "_top"))
	}

	@JvmOverloads
	fun axisBlock(block: RotatedPillarBlock, baseName: ResourceLocation = blockTexture(block))
	{
		axisBlock(block, extend(baseName, "_side"), extend(baseName, "_end"))
	}

	fun axisBlock(block: RotatedPillarBlock, side: ResourceLocation, end: ResourceLocation)
	{
		axisBlock(
			block,
			blockModels().cubeColumn(name(block), side, end),
			blockModels().cubeColumnHorizontal(name(block) + "_horizontal", side, end)
		)
	}

	fun axisBlockWithRenderType(block: RotatedPillarBlock, renderType: String)
	{
		axisBlockWithRenderType(block, blockTexture(block), renderType)
	}

	fun logBlockWithRenderType(block: RotatedPillarBlock, renderType: String)
	{
		axisBlockWithRenderType(block, blockTexture(block), extend(blockTexture(block), "_top"), renderType)
	}

	fun axisBlockWithRenderType(block: RotatedPillarBlock, baseName: ResourceLocation, renderType: String)
	{
		axisBlockWithRenderType(block, extend(baseName, "_side"), extend(baseName, "_end"), renderType)
	}

	fun axisBlockWithRenderType(
		block: RotatedPillarBlock,
		side: ResourceLocation,
		end: ResourceLocation,
		renderType: String
	)
	{
		axisBlock(
			block,
			blockModels().cubeColumn(name(block), side, end).renderType(renderType),
			blockModels().cubeColumnHorizontal(name(block) + "_horizontal", side, end).renderType(renderType)
		)
	}

	fun axisBlockWithRenderType(block: RotatedPillarBlock, renderType: ResourceLocation)
	{
		axisBlockWithRenderType(block, blockTexture(block), renderType)
	}

	fun logBlockWithRenderType(block: RotatedPillarBlock, renderType: ResourceLocation)
	{
		axisBlockWithRenderType(block, blockTexture(block), extend(blockTexture(block), "_top"), renderType)
	}

	fun axisBlockWithRenderType(block: RotatedPillarBlock, baseName: ResourceLocation, renderType: ResourceLocation)
	{
		axisBlockWithRenderType(block, extend(baseName, "_side"), extend(baseName, "_end"), renderType)
	}

	fun axisBlockWithRenderType(
		block: RotatedPillarBlock,
		side: ResourceLocation,
		end: ResourceLocation,
		renderType: ResourceLocation
	)
	{
		axisBlock(
			block,
			blockModels().cubeColumn(name(block), side, end).renderType(renderType),
			blockModels().cubeColumnHorizontal(name(block) + "_horizontal", side, end).renderType(renderType)
		)
	}

	fun axisBlock(
		block: RotatedPillarBlock,
		vertical: ModelFile,
		horizontal: ModelFile
	)
	{
		getVariantBuilder(block)
			.partialState().with<Direction.Axis>(RotatedPillarBlock.AXIS, Direction.Axis.Y)
			.modelForState().modelFile(vertical).addModel()
			.partialState().with<Direction.Axis>(RotatedPillarBlock.AXIS, Direction.Axis.Z)
			.modelForState().modelFile(horizontal).rotationX(90).addModel()
			.partialState().with<Direction.Axis>(RotatedPillarBlock.AXIS, Direction.Axis.X)
			.modelForState().modelFile(horizontal).rotationX(90).rotationY(90).addModel()
	}

	fun horizontalBlock(block: Block, side: ResourceLocation, front: ResourceLocation, top: ResourceLocation)
	{
		horizontalBlock(block, blockModels().orientable(name(block), side, front, top))
	}

	@JvmOverloads
	fun horizontalBlock(
		block: Block,
		model: ModelFile,
		angleOffset: Int = DEFAULT_ANGLE_OFFSET
	)
	{
		horizontalBlock(
			block,
			{ model },
			angleOffset
		)
	}

	@JvmOverloads
	fun horizontalBlock(
		block: Block,
		modelFunc: Function<BlockState, ModelFile>,
		angleOffset: Int = DEFAULT_ANGLE_OFFSET
	)
	{
		getVariantBuilder(block)
			.forAllStates { state: BlockState ->
				ConfiguredModel.builder()
					.modelFile(modelFunc.apply(state))
					.rotationY(
						(state.getValue(BlockStateProperties.HORIZONTAL_FACING)
							.toYRot().toInt() + angleOffset) % 360
					)
					.build()
			}
	}

	@JvmOverloads
	fun horizontalFaceBlock(
		block: Block,
		model: ModelFile,
		angleOffset: Int = DEFAULT_ANGLE_OFFSET
	)
	{
		horizontalFaceBlock(
			block,
			{ model },
			angleOffset
		)
	}

	@JvmOverloads
	fun horizontalFaceBlock(
		block: Block,
		modelFunc: Function<BlockState, ModelFile>,
		angleOffset: Int = DEFAULT_ANGLE_OFFSET
	)
	{
		getVariantBuilder(block)
			.forAllStates { state: BlockState ->
				ConfiguredModel.builder()
					.modelFile(modelFunc.apply(state))
					.rotationX(state.getValue(BlockStateProperties.ATTACH_FACE).ordinal * 90)
					.rotationY(
						((state.getValue(BlockStateProperties.HORIZONTAL_FACING)
							.toYRot()
							.toInt() + angleOffset) + (if (state.getValue(
								BlockStateProperties.ATTACH_FACE
							) == AttachFace.CEILING
						) 180 else 0)) % 360
					)
					.build()
			}
	}

	@JvmOverloads
	fun directionalBlock(
		block: Block,
		model: ModelFile,
		angleOffset: Int = DEFAULT_ANGLE_OFFSET
	)
	{
		directionalBlock(
			block,
			{ model },
			angleOffset
		)
	}

	@JvmOverloads
	fun directionalBlock(
		block: Block,
		modelFunc: Function<BlockState, ModelFile>,
		angleOffset: Int = DEFAULT_ANGLE_OFFSET
	)
	{
		getVariantBuilder(block)
			.forAllStates { state: BlockState ->
				val dir =
					state.getValue(BlockStateProperties.FACING)
				ConfiguredModel.builder()
					.modelFile(modelFunc.apply(state))
					.rotationX(
						if (dir == Direction.DOWN) 180 else if (dir.axis.isHorizontal) 90 else 0
					)
					.rotationY(
						if (dir.axis.isVertical) 0 else ((dir.toYRot()
							.toInt()) + angleOffset) % 360
					)
					.build()
			}
	}

	fun stairsBlock(block: StairBlock, texture: ResourceLocation)
	{
		stairsBlock(block, texture, texture, texture)
	}

	fun stairsBlock(block: StairBlock, name: String, texture: ResourceLocation)
	{
		stairsBlock(block, name, texture, texture, texture)
	}

	fun stairsBlock(block: StairBlock, side: ResourceLocation, bottom: ResourceLocation, top: ResourceLocation)
	{
		stairsBlockInternal(block, key(block).toString(), side, bottom, top)
	}

	fun stairsBlock(
		block: StairBlock,
		name: String,
		side: ResourceLocation,
		bottom: ResourceLocation,
		top: ResourceLocation
	)
	{
		stairsBlockInternal(block, name + "_stairs", side, bottom, top)
	}

	fun stairsBlockWithRenderType(block: StairBlock, texture: ResourceLocation, renderType: String)
	{
		stairsBlockWithRenderType(block, texture, texture, texture, renderType)
	}

	fun stairsBlockWithRenderType(block: StairBlock, name: String, texture: ResourceLocation, renderType: String)
	{
		stairsBlockWithRenderType(block, name, texture, texture, texture, renderType)
	}

	fun stairsBlockWithRenderType(
		block: StairBlock,
		side: ResourceLocation,
		bottom: ResourceLocation,
		top: ResourceLocation,
		renderType: String
	)
	{
		stairsBlockInternalWithRenderType(
			block,
			key(block).toString(),
			side,
			bottom,
			top,
			ResourceLocation(renderType)
		)
	}

	fun stairsBlockWithRenderType(
		block: StairBlock,
		name: String,
		side: ResourceLocation,
		bottom: ResourceLocation,
		top: ResourceLocation,
		renderType: String
	)
	{
		stairsBlockInternalWithRenderType(
			block,
			name + "_stairs",
			side,
			bottom,
			top,
			ResourceLocation(renderType)
		)
	}

	fun stairsBlockWithRenderType(block: StairBlock, texture: ResourceLocation, renderType: ResourceLocation)
	{
		stairsBlockWithRenderType(block, texture, texture, texture, renderType)
	}

	fun stairsBlockWithRenderType(
		block: StairBlock,
		name: String,
		texture: ResourceLocation,
		renderType: ResourceLocation
	)
	{
		stairsBlockWithRenderType(block, name, texture, texture, texture, renderType)
	}

	fun stairsBlockWithRenderType(
		block: StairBlock,
		side: ResourceLocation,
		bottom: ResourceLocation,
		top: ResourceLocation,
		renderType: ResourceLocation
	)
	{
		stairsBlockInternalWithRenderType(block, key(block).toString(), side, bottom, top, renderType)
	}

	fun stairsBlockWithRenderType(
		block: StairBlock,
		name: String,
		side: ResourceLocation,
		bottom: ResourceLocation,
		top: ResourceLocation,
		renderType: ResourceLocation
	)
	{
		stairsBlockInternalWithRenderType(block, name + "_stairs", side, bottom, top, renderType)
	}

	private fun stairsBlockInternal(
		block: StairBlock,
		baseName: String,
		side: ResourceLocation,
		bottom: ResourceLocation,
		top: ResourceLocation
	)
	{
		val stairs: ModelFile =
			blockModels().stairs(baseName, side, bottom, top)
		val stairsInner: ModelFile =
			blockModels().stairsInner(baseName + "_inner", side, bottom, top)
		val stairsOuter: ModelFile =
			blockModels().stairsOuter(baseName + "_outer", side, bottom, top)
		stairsBlock(block, stairs, stairsInner, stairsOuter)
	}

	private fun stairsBlockInternalWithRenderType(
		block: StairBlock,
		baseName: String,
		side: ResourceLocation,
		bottom: ResourceLocation,
		top: ResourceLocation,
		renderType: ResourceLocation
	)
	{
		val stairs: ModelFile =
			blockModels().stairs(baseName, side, bottom, top).renderType(renderType)
		val stairsInner: ModelFile =
			blockModels().stairsInner(baseName + "_inner", side, bottom, top).renderType(renderType)
		val stairsOuter: ModelFile =
			blockModels().stairsOuter(baseName + "_outer", side, bottom, top).renderType(renderType)
		stairsBlock(block, stairs, stairsInner, stairsOuter)
	}

	fun stairsBlock(
		block: StairBlock,
		stairs: ModelFile,
		stairsInner: ModelFile,
		stairsOuter: ModelFile
	)
	{
		getVariantBuilder(block)
			.forAllStatesExcept({ state: BlockState ->
				val facing =
					state.getValue(StairBlock.FACING)
				val half =
					state.getValue(StairBlock.HALF)
				val shape =
					state.getValue(StairBlock.SHAPE)
				var yRot =
					facing.clockWise.toYRot().toInt() // Stairs model is rotated 90 degrees clockwise for some reason
				if (shape == StairsShape.INNER_LEFT || shape == StairsShape.OUTER_LEFT)
				{
					yRot += 270 // Left facing stairs are rotated 90 degrees clockwise
				}
				if (shape != StairsShape.STRAIGHT && half == Half.TOP)
				{
					yRot += 90 // Top stairs are rotated 90 degrees clockwise
				}
				yRot %= 360
				val uvlock =
					yRot != 0 || half == Half.TOP // Don't set uvlock for states that have no rotation
				ConfiguredModel.builder()
					.modelFile(if (shape == StairsShape.STRAIGHT) stairs else if (shape == StairsShape.INNER_LEFT || shape == StairsShape.INNER_RIGHT) stairsInner else stairsOuter)
					.rotationX(if (half == Half.BOTTOM) 0 else 180)
					.rotationY(yRot)
					.uvLock(uvlock)
					.build()
			}, StairBlock.WATERLOGGED)
	}

	fun slabBlock(block: SlabBlock, doubleslab: ResourceLocation, texture: ResourceLocation)
	{
		slabBlock(block, doubleslab, texture, texture, texture)
	}

	fun slabBlock(
		block: SlabBlock,
		doubleslab: ResourceLocation,
		side: ResourceLocation,
		bottom: ResourceLocation,
		top: ResourceLocation
	)
	{
		slabBlock(
			block,
			blockModels().slab(name(block), side, bottom, top),
			blockModels().slabTop(name(block) + "_top", side, bottom, top),
			blockModels().getExistingFile(doubleslab)
		)
	}

	fun slabBlock(
		block: SlabBlock,
		bottom: ModelFile,
		top: ModelFile,
		doubleslab: ModelFile
	)
	{
		getVariantBuilder(block)
			.partialState().with<SlabType>(SlabBlock.TYPE, SlabType.BOTTOM)
			.addModels(ConfiguredModel(bottom))
			.partialState().with<SlabType>(SlabBlock.TYPE, SlabType.TOP)
			.addModels(ConfiguredModel(top))
			.partialState().with<SlabType>(SlabBlock.TYPE, SlabType.DOUBLE)
			.addModels(ConfiguredModel(doubleslab))
	}

	fun buttonBlock(block: ButtonBlock, texture: ResourceLocation)
	{
		val button: ModelFile = blockModels().button(name(block), texture)
		val buttonPressed: ModelFile =
			blockModels().buttonPressed(name(block) + "_pressed", texture)
		buttonBlock(block, button, buttonPressed)
	}

	fun buttonBlock(
		block: ButtonBlock,
		button: ModelFile,
		buttonPressed: ModelFile
	)
	{
		getVariantBuilder(block).forAllStates(Function<BlockState, Array<ConfiguredModel>> { state: BlockState ->
			val facing =
				state.getValue(ButtonBlock.FACING)
			val face =
				state.getValue(ButtonBlock.FACE)
			val powered =
				state.getValue(ButtonBlock.POWERED)
			ConfiguredModel.builder()
				.modelFile(if (powered) buttonPressed else button)
				.rotationX(if (face == AttachFace.FLOOR) 0 else if (face == AttachFace.WALL) 90 else 180)
				.rotationY(
					(if (face == AttachFace.CEILING) facing else facing.opposite).toYRot()
						.toInt()
				)
				.uvLock(face == AttachFace.WALL)
				.build()
		})
	}

	fun pressurePlateBlock(block: PressurePlateBlock, texture: ResourceLocation)
	{
		val pressurePlate: ModelFile =
			blockModels().pressurePlate(name(block), texture)
		val pressurePlateDown: ModelFile =
			blockModels().pressurePlateDown(name(block) + "_down", texture)
		pressurePlateBlock(block, pressurePlate, pressurePlateDown)
	}

	fun pressurePlateBlock(
		block: PressurePlateBlock,
		pressurePlate: ModelFile,
		pressurePlateDown: ModelFile
	)
	{
		getVariantBuilder(block)
			.partialState().with<Boolean>(PressurePlateBlock.POWERED, true)
			.addModels(ConfiguredModel(pressurePlateDown))
			.partialState().with<Boolean>(PressurePlateBlock.POWERED, false)
			.addModels(ConfiguredModel(pressurePlate))
	}

	fun signBlock(signBlock: StandingSignBlock, wallSignBlock: WallSignBlock, texture: ResourceLocation)
	{
		val sign: ModelFile = blockModels().sign(name(signBlock), texture)
		signBlock(signBlock, wallSignBlock, sign)
	}

	fun signBlock(
		signBlock: StandingSignBlock,
		wallSignBlock: WallSignBlock,
		sign: ModelFile
	)
	{
		simpleBlock(signBlock, sign)
		simpleBlock(wallSignBlock, sign)
	}

	fun fourWayBlock(
		block: CrossCollisionBlock,
		post: ModelFile,
		side: ModelFile
	)
	{
		val builder: MultiPartBlockStateBuilder =
			getMultipartBuilder(block)
				.part().modelFile(post).addModel().end()
		fourWayMultipart(builder, side)
	}

	fun fourWayMultipart(
		builder: MultiPartBlockStateBuilder,
		side: ModelFile
	)
	{
		PipeBlock.PROPERTY_BY_DIRECTION.entries.forEach(Consumer<Map.Entry<Direction, BooleanProperty>> { e: Map.Entry<Direction, BooleanProperty> ->
			val dir = e.key
			if (dir.axis.isHorizontal)
			{
				builder.part().modelFile(side)
					.rotationY(((dir.toYRot().toInt()) + 180) % 360).uvLock(true).addModel()
					.condition<Boolean>(e.value, true)
			}
		})
	}

	fun fenceBlock(block: FenceBlock, texture: ResourceLocation)
	{
		val baseName = key(block).toString()
		fourWayBlock(
			block,
			blockModels().fencePost(baseName + "_post", texture),
			blockModels().fenceSide(baseName + "_side", texture)
		)
	}

	fun fenceBlock(block: FenceBlock, name: String, texture: ResourceLocation)
	{
		fourWayBlock(
			block,
			blockModels().fencePost(name + "_fence_post", texture),
			blockModels().fenceSide(name + "_fence_side", texture)
		)
	}

	fun fenceBlockWithRenderType(block: FenceBlock, texture: ResourceLocation, renderType: String)
	{
		val baseName = key(block).toString()
		fourWayBlock(
			block,
			blockModels().fencePost(baseName + "_post", texture).renderType(renderType),
			blockModels().fenceSide(baseName + "_side", texture).renderType(renderType)
		)
	}

	fun fenceBlockWithRenderType(block: FenceBlock, name: String, texture: ResourceLocation, renderType: String)
	{
		fourWayBlock(
			block,
			blockModels().fencePost(name + "_fence_post", texture).renderType(renderType),
			blockModels().fenceSide(name + "_fence_side", texture).renderType(renderType)
		)
	}

	fun fenceBlockWithRenderType(block: FenceBlock, texture: ResourceLocation, renderType: ResourceLocation)
	{
		val baseName = key(block).toString()
		fourWayBlock(
			block,
			blockModels().fencePost(baseName + "_post", texture).renderType(renderType),
			blockModels().fenceSide(baseName + "_side", texture).renderType(renderType)
		)
	}

	fun fenceBlockWithRenderType(
		block: FenceBlock,
		name: String,
		texture: ResourceLocation,
		renderType: ResourceLocation
	)
	{
		fourWayBlock(
			block,
			blockModels().fencePost(name + "_fence_post", texture).renderType(renderType),
			blockModels().fenceSide(name + "_fence_side", texture).renderType(renderType)
		)
	}

	fun fenceGateBlock(block: FenceGateBlock, texture: ResourceLocation)
	{
		fenceGateBlockInternal(block, key(block).toString(), texture)
	}

	fun fenceGateBlock(block: FenceGateBlock, name: String, texture: ResourceLocation)
	{
		fenceGateBlockInternal(block, name + "_fence_gate", texture)
	}

	fun fenceGateBlockWithRenderType(block: FenceGateBlock, texture: ResourceLocation, renderType: String)
	{
		fenceGateBlockInternalWithRenderType(
			block,
			key(block).toString(),
			texture,
			ResourceLocation(renderType)
		)
	}

	fun fenceGateBlockWithRenderType(
		block: FenceGateBlock,
		name: String,
		texture: ResourceLocation,
		renderType: String
	)
	{
		fenceGateBlockInternalWithRenderType(
			block,
			name + "_fence_gate",
			texture,
			ResourceLocation(renderType)
		)
	}

	fun fenceGateBlockWithRenderType(block: FenceGateBlock, texture: ResourceLocation, renderType: ResourceLocation)
	{
		fenceGateBlockInternalWithRenderType(block, key(block).toString(), texture, renderType)
	}

	fun fenceGateBlockWithRenderType(
		block: FenceGateBlock,
		name: String,
		texture: ResourceLocation,
		renderType: ResourceLocation
	)
	{
		fenceGateBlockInternalWithRenderType(block, name + "_fence_gate", texture, renderType)
	}

	private fun fenceGateBlockInternal(block: FenceGateBlock, baseName: String, texture: ResourceLocation)
	{
		val gate: ModelFile = blockModels().fenceGate(baseName, texture)
		val gateOpen: ModelFile =
			blockModels().fenceGateOpen(baseName + "_open", texture)
		val gateWall: ModelFile =
			blockModels().fenceGateWall(baseName + "_wall", texture)
		val gateWallOpen: ModelFile =
			blockModels().fenceGateWallOpen(baseName + "_wall_open", texture)
		fenceGateBlock(block, gate, gateOpen, gateWall, gateWallOpen)
	}

	private fun fenceGateBlockInternalWithRenderType(
		block: FenceGateBlock,
		baseName: String,
		texture: ResourceLocation,
		renderType: ResourceLocation
	)
	{
		val gate: ModelFile =
			blockModels().fenceGate(baseName, texture).renderType(renderType)
		val gateOpen: ModelFile =
			blockModels().fenceGateOpen(baseName + "_open", texture).renderType(renderType)
		val gateWall: ModelFile =
			blockModels().fenceGateWall(baseName + "_wall", texture).renderType(renderType)
		val gateWallOpen: ModelFile =
			blockModels().fenceGateWallOpen(baseName + "_wall_open", texture).renderType(renderType)
		fenceGateBlock(block, gate, gateOpen, gateWall, gateWallOpen)
	}

	fun fenceGateBlock(
		block: FenceGateBlock,
		gate: ModelFile,
		gateOpen: ModelFile,
		gateWall: ModelFile,
		gateWallOpen: ModelFile
	)
	{
		getVariantBuilder(block).forAllStatesExcept({ state: BlockState ->
			var model: ModelFile = gate
			if (state.getValue(FenceGateBlock.IN_WALL))
			{
				model = gateWall
			}
			if (state.getValue(FenceGateBlock.OPEN))
			{
				model = if (model === gateWall) gateWallOpen else gateOpen
			}
			ConfiguredModel.builder()
				.modelFile(model)
				.rotationY(
					state.getValue(FenceGateBlock.FACING)
						.toYRot().toInt()
				)
				.uvLock(true)
				.build()
		}, FenceGateBlock.POWERED)
	}

	fun wallBlock(block: WallBlock, texture: ResourceLocation)
	{
		wallBlockInternal(block, key(block).toString(), texture)
	}

	fun wallBlock(block: WallBlock, name: String, texture: ResourceLocation)
	{
		wallBlockInternal(block, name + "_wall", texture)
	}

	fun wallBlockWithRenderType(block: WallBlock, texture: ResourceLocation, renderType: String)
	{
		wallBlockInternalWithRenderType(block, key(block).toString(), texture, ResourceLocation(renderType))
	}

	fun wallBlockWithRenderType(block: WallBlock, name: String, texture: ResourceLocation, renderType: String)
	{
		wallBlockInternalWithRenderType(block, name + "_wall", texture, ResourceLocation(renderType))
	}

	fun wallBlockWithRenderType(block: WallBlock, texture: ResourceLocation, renderType: ResourceLocation)
	{
		wallBlockInternalWithRenderType(block, key(block).toString(), texture, renderType)
	}

	fun wallBlockWithRenderType(
		block: WallBlock,
		name: String,
		texture: ResourceLocation,
		renderType: ResourceLocation
	)
	{
		wallBlockInternalWithRenderType(block, name + "_wall", texture, renderType)
	}

	private fun wallBlockInternal(block: WallBlock, baseName: String, texture: ResourceLocation)
	{
		wallBlock(
			block, blockModels().wallPost(baseName + "_post", texture),
			blockModels().wallSide(baseName + "_side", texture),
			blockModels().wallSideTall(baseName + "_side_tall", texture)
		)
	}

	private fun wallBlockInternalWithRenderType(
		block: WallBlock,
		baseName: String,
		texture: ResourceLocation,
		renderType: ResourceLocation
	)
	{
		wallBlock(
			block, blockModels().wallPost(baseName + "_post", texture).renderType(renderType),
			blockModels().wallSide(baseName + "_side", texture).renderType(renderType),
			blockModels().wallSideTall(baseName + "_side_tall", texture).renderType(renderType)
		)
	}

	fun wallBlock(
		block: WallBlock,
		post: ModelFile,
		side: ModelFile,
		sideTall: ModelFile
	)
	{
		val builder: MultiPartBlockStateBuilder =
			getMultipartBuilder(block)
				.part().modelFile(post).addModel()
				.condition<Boolean>(WallBlock.UP, true).end()
		WALL_PROPS.entries.stream()
			.filter { e: Map.Entry<Direction, Property<WallSide>> ->
				e.key.axis.isHorizontal
			}
			.forEach { e: Map.Entry<Direction, Property<WallSide>> ->
				wallSidePart(builder, side, e, WallSide.LOW)
				wallSidePart(builder, sideTall, e, WallSide.TALL)
			}
	}

	private fun wallSidePart(
		builder: MultiPartBlockStateBuilder,
		model: ModelFile,
		entry: Map.Entry<Direction, Property<WallSide>>,
		height: WallSide
	)
	{
		builder.part()
			.modelFile(model)
			.rotationY(((entry.key.toYRot().toInt()) + 180) % 360)
			.uvLock(true)
			.addModel()
			.condition(entry.value, height)
	}

	fun paneBlock(block: IronBarsBlock, pane: ResourceLocation, edge: ResourceLocation)
	{
		paneBlockInternal(block, key(block).toString(), pane, edge)
	}

	fun paneBlock(block: IronBarsBlock, name: String, pane: ResourceLocation, edge: ResourceLocation)
	{
		paneBlockInternal(block, name + "_pane", pane, edge)
	}

	fun paneBlockWithRenderType(
		block: IronBarsBlock,
		pane: ResourceLocation,
		edge: ResourceLocation,
		renderType: String
	)
	{
		paneBlockInternalWithRenderType(block, key(block).toString(), pane, edge, ResourceLocation(renderType))
	}

	fun paneBlockWithRenderType(
		block: IronBarsBlock,
		name: String,
		pane: ResourceLocation,
		edge: ResourceLocation,
		renderType: String
	)
	{
		paneBlockInternalWithRenderType(block, name + "_pane", pane, edge, ResourceLocation(renderType))
	}

	fun paneBlockWithRenderType(
		block: IronBarsBlock,
		pane: ResourceLocation,
		edge: ResourceLocation,
		renderType: ResourceLocation
	)
	{
		paneBlockInternalWithRenderType(block, key(block).toString(), pane, edge, renderType)
	}

	fun paneBlockWithRenderType(
		block: IronBarsBlock,
		name: String,
		pane: ResourceLocation,
		edge: ResourceLocation,
		renderType: ResourceLocation
	)
	{
		paneBlockInternalWithRenderType(block, name + "_pane", pane, edge, renderType)
	}

	private fun paneBlockInternal(
		block: IronBarsBlock,
		baseName: String,
		pane: ResourceLocation,
		edge: ResourceLocation
	)
	{
		val post: ModelFile =
			blockModels().panePost(baseName + "_post", pane, edge)
		val side: ModelFile =
			blockModels().paneSide(baseName + "_side", pane, edge)
		val sideAlt: ModelFile =
			blockModels().paneSideAlt(baseName + "_side_alt", pane, edge)
		val noSide: ModelFile =
			blockModels().paneNoSide(baseName + "_noside", pane)
		val noSideAlt: ModelFile =
			blockModels().paneNoSideAlt(baseName + "_noside_alt", pane)
		paneBlock(block, post, side, sideAlt, noSide, noSideAlt)
	}

	private fun paneBlockInternalWithRenderType(
		block: IronBarsBlock,
		baseName: String,
		pane: ResourceLocation,
		edge: ResourceLocation,
		renderType: ResourceLocation
	)
	{
		val post: ModelFile =
			blockModels().panePost(baseName + "_post", pane, edge).renderType(renderType)
		val side: ModelFile =
			blockModels().paneSide(baseName + "_side", pane, edge).renderType(renderType)
		val sideAlt: ModelFile =
			blockModels().paneSideAlt(baseName + "_side_alt", pane, edge).renderType(renderType)
		val noSide: ModelFile =
			blockModels().paneNoSide(baseName + "_noside", pane).renderType(renderType)
		val noSideAlt: ModelFile =
			blockModels().paneNoSideAlt(baseName + "_noside_alt", pane).renderType(renderType)
		paneBlock(block, post, side, sideAlt, noSide, noSideAlt)
	}

	fun paneBlock(
		block: IronBarsBlock,
		post: ModelFile,
		side: ModelFile,
		sideAlt: ModelFile,
		noSide: ModelFile,
		noSideAlt: ModelFile
	)
	{
		val builder: MultiPartBlockStateBuilder =
			getMultipartBuilder(block)
				.part().modelFile(post).addModel().end()
		PipeBlock.PROPERTY_BY_DIRECTION.entries.forEach(Consumer<Map.Entry<Direction, BooleanProperty>> { e: Map.Entry<Direction, BooleanProperty> ->
			val dir = e.key
			if (dir.axis.isHorizontal)
			{
				val alt = dir == Direction.SOUTH
				builder.part().modelFile(if (alt || dir == Direction.WEST) sideAlt else side)
					.rotationY(if (dir.axis === Direction.Axis.X) 90 else 0).addModel()
					.condition<Boolean>(e.value, true).end()
					.part().modelFile(if (alt || dir == Direction.EAST) noSideAlt else noSide)
					.rotationY(if (dir == Direction.WEST) 270 else if (dir == Direction.SOUTH) 90 else 0)
					.addModel()
					.condition<Boolean>(e.value, false)
			}
		})
	}

	fun doorBlock(block: DoorBlock, bottom: ResourceLocation, top: ResourceLocation)
	{
		doorBlockInternal(block, key(block).toString(), bottom, top)
	}

	fun doorBlock(block: DoorBlock, name: String, bottom: ResourceLocation, top: ResourceLocation)
	{
		doorBlockInternal(block, name + "_door", bottom, top)
	}

	fun doorBlockWithRenderType(block: DoorBlock, bottom: ResourceLocation, top: ResourceLocation, renderType: String)
	{
		doorBlockInternalWithRenderType(
			block,
			key(block).toString(),
			bottom,
			top,
			ResourceLocation(renderType)
		)
	}

	fun doorBlockWithRenderType(
		block: DoorBlock,
		name: String,
		bottom: ResourceLocation,
		top: ResourceLocation,
		renderType: String
	)
	{
		doorBlockInternalWithRenderType(block, name + "_door", bottom, top, ResourceLocation(renderType))
	}

	fun doorBlockWithRenderType(
		block: DoorBlock,
		bottom: ResourceLocation,
		top: ResourceLocation,
		renderType: ResourceLocation
	)
	{
		doorBlockInternalWithRenderType(block, key(block).toString(), bottom, top, renderType)
	}

	fun doorBlockWithRenderType(
		block: DoorBlock,
		name: String,
		bottom: ResourceLocation,
		top: ResourceLocation,
		renderType: ResourceLocation
	)
	{
		doorBlockInternalWithRenderType(block, name + "_door", bottom, top, renderType)
	}

	private fun doorBlockInternal(block: DoorBlock, baseName: String, bottom: ResourceLocation, top: ResourceLocation)
	{
		val bottomLeft: ModelFile =
			blockModels().doorBottomLeft(baseName + "_bottom_left", bottom, top)
		val bottomLeftOpen: ModelFile =
			blockModels().doorBottomLeftOpen(baseName + "_bottom_left_open", bottom, top)
		val bottomRight: ModelFile =
			blockModels().doorBottomRight(baseName + "_bottom_right", bottom, top)
		val bottomRightOpen: ModelFile =
			blockModels().doorBottomRightOpen(baseName + "_bottom_right_open", bottom, top)
		val topLeft: ModelFile =
			blockModels().doorTopLeft(baseName + "_top_left", bottom, top)
		val topLeftOpen: ModelFile =
			blockModels().doorTopLeftOpen(baseName + "_top_left_open", bottom, top)
		val topRight: ModelFile =
			blockModels().doorTopRight(baseName + "_top_right", bottom, top)
		val topRightOpen: ModelFile =
			blockModels().doorTopRightOpen(baseName + "_top_right_open", bottom, top)
		doorBlock(
			block,
			bottomLeft,
			bottomLeftOpen,
			bottomRight,
			bottomRightOpen,
			topLeft,
			topLeftOpen,
			topRight,
			topRightOpen
		)
	}

	private fun doorBlockInternalWithRenderType(
		block: DoorBlock,
		baseName: String,
		bottom: ResourceLocation,
		top: ResourceLocation,
		renderType: ResourceLocation
	)
	{
		val bottomLeft: ModelFile =
			blockModels().doorBottomLeft(baseName + "_bottom_left", bottom, top).renderType(renderType)
		val bottomLeftOpen: ModelFile =
			blockModels().doorBottomLeftOpen(baseName + "_bottom_left_open", bottom, top).renderType(renderType)
		val bottomRight: ModelFile =
			blockModels().doorBottomRight(baseName + "_bottom_right", bottom, top).renderType(renderType)
		val bottomRightOpen: ModelFile =
			blockModels().doorBottomRightOpen(baseName + "_bottom_right_open", bottom, top).renderType(renderType)
		val topLeft: ModelFile =
			blockModels().doorTopLeft(baseName + "_top_left", bottom, top).renderType(renderType)
		val topLeftOpen: ModelFile =
			blockModels().doorTopLeftOpen(baseName + "_top_left_open", bottom, top).renderType(renderType)
		val topRight: ModelFile =
			blockModels().doorTopRight(baseName + "_top_right", bottom, top).renderType(renderType)
		val topRightOpen: ModelFile =
			blockModels().doorTopRightOpen(baseName + "_top_right_open", bottom, top).renderType(renderType)
		doorBlock(
			block,
			bottomLeft,
			bottomLeftOpen,
			bottomRight,
			bottomRightOpen,
			topLeft,
			topLeftOpen,
			topRight,
			topRightOpen
		)
	}

	fun doorBlock(
		block: DoorBlock,
		bottomLeft: ModelFile,
		bottomLeftOpen: ModelFile,
		bottomRight: ModelFile,
		bottomRightOpen: ModelFile,
		topLeft: ModelFile,
		topLeftOpen: ModelFile,
		topRight: ModelFile,
		topRightOpen: ModelFile
	)
	{
		getVariantBuilder(block).forAllStatesExcept({ state: BlockState ->
			var yRot =
				state.getValue(DoorBlock.FACING).toYRot()
					.toInt() + 90
			val right =
				state.getValue(DoorBlock.HINGE) == DoorHingeSide.RIGHT
			val open = state.getValue(DoorBlock.OPEN)
			val lower =
				state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER
			if (open)
			{
				yRot += 90
			}
			if (right && open)
			{
				yRot += 180
			}
			yRot %= 360

			val model: ModelFile = when
			{
				lower && right && open ->
				{
					bottomRightOpen
				}

				lower && !right && open ->
				{
					bottomLeftOpen
				}

				lower && right && !open ->
				{
					bottomRight
				}

				lower && !right && !open ->
				{
					bottomLeft
				}

				!lower && right && open ->
				{
					topRightOpen
				}

				!lower && !right && open ->
				{
					topLeftOpen
				}

				!lower && right && !open ->
				{
					topRight
				}

				!lower && !right && !open ->
				{
					topLeft
				}

				else -> null
			}!!
			ConfiguredModel.builder().modelFile(model)
				.rotationY(yRot)
				.build()
		}, DoorBlock.POWERED)
	}

	fun trapdoorBlock(block: TrapDoorBlock, texture: ResourceLocation, orientable: Boolean)
	{
		trapdoorBlockInternal(block, key(block).toString(), texture, orientable)
	}

	fun trapdoorBlock(block: TrapDoorBlock, name: String, texture: ResourceLocation, orientable: Boolean)
	{
		trapdoorBlockInternal(block, name + "_trapdoor", texture, orientable)
	}

	fun trapdoorBlockWithRenderType(
		block: TrapDoorBlock,
		texture: ResourceLocation,
		orientable: Boolean,
		renderType: String
	)
	{
		trapdoorBlockInternalWithRenderType(
			block,
			key(block).toString(),
			texture,
			orientable,
			ResourceLocation(renderType)
		)
	}

	fun trapdoorBlockWithRenderType(
		block: TrapDoorBlock,
		name: String,
		texture: ResourceLocation,
		orientable: Boolean,
		renderType: String
	)
	{
		trapdoorBlockInternalWithRenderType(
			block,
			name + "_trapdoor",
			texture,
			orientable,
			ResourceLocation(renderType)
		)
	}

	fun trapdoorBlockWithRenderType(
		block: TrapDoorBlock,
		texture: ResourceLocation,
		orientable: Boolean,
		renderType: ResourceLocation
	)
	{
		trapdoorBlockInternalWithRenderType(block, key(block).toString(), texture, orientable, renderType)
	}

	fun trapdoorBlockWithRenderType(
		block: TrapDoorBlock,
		name: String,
		texture: ResourceLocation,
		orientable: Boolean,
		renderType: ResourceLocation
	)
	{
		trapdoorBlockInternalWithRenderType(block, name + "_trapdoor", texture, orientable, renderType)
	}

	private fun trapdoorBlockInternal(
		block: TrapDoorBlock,
		baseName: String,
		texture: ResourceLocation,
		orientable: Boolean
	)
	{
		val bottom: ModelFile =
			if (orientable) blockModels().trapdoorOrientableBottom(
				baseName + "_bottom",
				texture
			) else blockModels().trapdoorBottom(baseName + "_bottom", texture)
		val top: ModelFile =
			if (orientable) blockModels().trapdoorOrientableTop(
				baseName + "_top",
				texture
			) else blockModels().trapdoorTop(
				baseName + "_top",
				texture
			)
		val open: ModelFile =
			if (orientable) blockModels().trapdoorOrientableOpen(
				baseName + "_open",
				texture
			) else blockModels().trapdoorOpen(
				baseName + "_open",
				texture
			)
		trapdoorBlock(block, bottom, top, open, orientable)
	}

	private fun trapdoorBlockInternalWithRenderType(
		block: TrapDoorBlock,
		baseName: String,
		texture: ResourceLocation,
		orientable: Boolean,
		renderType: ResourceLocation
	)
	{
		val bottom: ModelFile =
			if (orientable) blockModels().trapdoorOrientableBottom(baseName + "_bottom", texture)
				.renderType(renderType) else blockModels().trapdoorBottom(baseName + "_bottom", texture)
				.renderType(renderType)
		val top: ModelFile =
			if (orientable) blockModels().trapdoorOrientableTop(baseName + "_top", texture)
				.renderType(renderType) else blockModels().trapdoorTop(baseName + "_top", texture)
				.renderType(renderType)
		val open: ModelFile =
			if (orientable) blockModels().trapdoorOrientableOpen(baseName + "_open", texture)
				.renderType(renderType) else blockModels().trapdoorOpen(baseName + "_open", texture)
				.renderType(renderType)
		trapdoorBlock(block, bottom, top, open, orientable)
	}

	fun trapdoorBlock(
		block: TrapDoorBlock,
		bottom: ModelFile,
		top: ModelFile,
		open: ModelFile,
		orientable: Boolean
	)
	{
		getVariantBuilder(block).forAllStatesExcept({ state: BlockState ->
			var xRot = 0
			var yRot =
				state.getValue(TrapDoorBlock.FACING)
					.toYRot().toInt() + 180
			val isOpen =
				state.getValue(TrapDoorBlock.OPEN)
			if (orientable && isOpen && state.getValue(TrapDoorBlock.HALF) == Half.TOP)
			{
				xRot += 180
				yRot += 180
			}
			if (!orientable && !isOpen)
			{
				yRot = 0
			}
			yRot %= 360
			ConfiguredModel.builder().modelFile(
				if (isOpen) open else if (state.getValue(TrapDoorBlock.HALF) == Half.TOP) top else bottom
			)
				.rotationX(xRot)
				.rotationY(yRot)
				.build()
		}, TrapDoorBlock.POWERED, TrapDoorBlock.WATERLOGGED)
	}

	private fun saveBlockState(cache: CachedOutput, stateJson: JsonObject, owner: Block): CompletableFuture<*>
	{
		val blockName = Preconditions.checkNotNull(key(owner))
		val outputPath = output.getOutputFolder(PackOutput.Target.RESOURCE_PACK)
			.resolve(blockName.namespace).resolve("blockstates").resolve(blockName.path + ".json")
		return DataProvider.saveStable(cache, stateJson, outputPath)
	}

	override fun getName(): String
	{
		return format("Block States")
	}

	class ConfiguredModelList private constructor(models: List<ConfiguredModel>)
	{
		private val models: List<ConfiguredModel>

		init
		{
			Preconditions.checkArgument(models.isNotEmpty())
			this.models = models
		}

		constructor(vararg models: ConfiguredModel) : this(
			listOf<ConfiguredModel>(
				*models
			)
		)

		fun toJSON(): JsonElement
		{
			if (models.size == 1)
			{
				return models[0].toJSON(false)
			} else
			{
				val ret = JsonArray()
				for (m in models)
				{
					ret.add(m.toJSON(true))
				}
				return ret
			}
		}

		fun append(vararg models: ConfiguredModel): ConfiguredModelList
		{
			return ConfiguredModelList(
				buildList {
					addAll(this@ConfiguredModelList.models)
					addAll(models)
				}
			)
		}
	}

	companion object
	{
		private val LOGGER: Logger = LogManager.getLogger()
		private val GSON: Gson =
			GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

		private const val DEFAULT_ANGLE_OFFSET = 180

		val WALL_PROPS: Map<Direction, Property<WallSide>> =
			buildMap {
				put(Direction.EAST, BlockStateProperties.EAST_WALL)
				put(Direction.NORTH, BlockStateProperties.NORTH_WALL)
				put(Direction.SOUTH, BlockStateProperties.SOUTH_WALL)
				put(Direction.WEST, BlockStateProperties.WEST_WALL)
			}

	}
}
