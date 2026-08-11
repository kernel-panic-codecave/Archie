package net.kernelpanicsoft.archie.data.client.model

import com.google.common.base.Preconditions
import com.google.common.collect.ImmutableList
import com.google.common.collect.ObjectArrays
import com.google.gson.JsonObject
import net.minecraft.client.resources.model.BlockModelRotation
import java.util.*
import java.util.function.Function
import java.util.stream.Collectors
import java.util.stream.IntStream

/** One weighted, rotated variant entry pointing at a [model], as used in blockstate `variants`. Build via [builder]. */
class AConfiguredModel @JvmOverloads constructor(
	model: AModelFile,
	rotationX: Int = 0,
	rotationY: Int = 0,
	uvLock: Boolean = false,
	weight: Int = DEFAULT_WEIGHT
)
{
	val model: AModelFile
	val rotationX: Int
	val rotationY: Int
	val uvLock: Boolean
	val weight: Int

	init
	{
		Preconditions.checkNotNull(model)
		this.model = model
		checkRotation(rotationX, rotationY)
		this.rotationX = rotationX
		this.rotationY = rotationY
		this.uvLock = uvLock
		checkWeight(weight)
		this.weight = weight
	}

	fun toJSON(includeWeight: Boolean): JsonObject
	{
		val modelJson = JsonObject()
		modelJson.addProperty("model", model.location.toString())
		if (rotationX != 0) modelJson.addProperty("x", rotationX)
		if (rotationY != 0) modelJson.addProperty("y", rotationY)
		if (uvLock) modelJson.addProperty("uvlock", uvLock)
		if (includeWeight && weight != DEFAULT_WEIGHT) modelJson.addProperty("weight", weight)
		return modelJson
	}

	/**
	 * A builder for one or more [AConfiguredModel]s, optionally backed by a callback that
	 * consumes the finished result and returns [T] (the owning builder, e.g. an
	 * [AVariantBlockStateBuilder.PartialBlockstate]). Without a callback (as from the standalone
	 * [AConfiguredModel.builder]), [addModel] is unavailable; use [build]/[buildLast] instead.
	 *
	 * Multiple weighted variants can be configured at once through [nextModel]/[model].
	 */
	class Builder<T> @JvmOverloads internal constructor(
		private val callback: Function<Array<AConfiguredModel>, T>? = null,
		private var otherModels: List<AConfiguredModel> = listOf()
	)
	{
		private var model: AModelFile? = null
		private var rotationX = 0
		private var rotationY = 0
		private var uvLock = false
		private var weight = DEFAULT_WEIGHT

		fun modelFile(model: AModelFile): Builder<T>
		{
			Preconditions.checkNotNull(
				model,
				"Model must not be null"
			)
			this.model = model
			return this
		}

		fun rotationX(value: Int): Builder<T>
		{
			checkRotation(value, rotationY)
			rotationX = value
			return this
		}

		fun rotationY(value: Int): Builder<T>
		{
			checkRotation(rotationX, value)
			rotationY = value
			return this
		}

		fun uvLock(value: Boolean): Builder<T>
		{
			uvLock = value
			return this
		}

		fun weight(value: Int): Builder<T>
		{
			checkWeight(value)
			weight = value
			return this
		}

		/** Builds only the currently-configured [AConfiguredModel], discarding [otherModels]. */
		fun buildLast(): AConfiguredModel
		{
			return AConfiguredModel(model!!, rotationX, rotationY, uvLock, weight)
		}

		/** Builds every configured model, including any queued via [nextModel]. */
		fun build(): Array<AConfiguredModel>
		{
			return ObjectArrays.concat(otherModels.toTypedArray(), buildLast())
		}

		/** Finalizes [build] and hands the result to the owning builder's callback, returning [T]. */
		fun addModel(): T
		{
			Preconditions.checkNotNull(callback, "Cannot use addModel() without an owning builder present")
			return callback!!.apply(build())
		}

		/** Starts configuring another weighted variant, keeping models built so far. */
		fun nextModel(): Builder<T>
		{
			return Builder(callback, build().toList())
		}

		/** Configures the current (or, once already configured, the next) variant with [block]. */
		fun model(block: Builder<T>.() -> Unit): Builder<T>
		{
			if (otherModels.isEmpty())
				block()
			else
				otherModels = nextModel().apply(block).otherModels
			return this
		}
	}

	companion object
	{
		const val DEFAULT_WEIGHT: Int = 1

		private fun validRotations(): IntStream
		{
			return IntStream.range(0, 4).map { i: Int -> i * 90 }
		}

		/** Builds one [AConfiguredModel] of [model] per valid Y rotation (0/90/180/270), all at fixed X rotation [x]. */
		@JvmOverloads
		fun allYRotations(
			model: AModelFile,
			x: Int,
			uvlock: Boolean,
			weight: Int = DEFAULT_WEIGHT
		): Array<AConfiguredModel>
		{
			return validRotations()
				.mapToObj { y: Int -> AConfiguredModel(model, x, y, uvlock, weight) }
				.collect(Collectors.toList()).toTypedArray()
		}

		/** Builds one [AConfiguredModel] of [model] for every valid X/Y rotation combination. */
		@JvmOverloads
		fun allRotations(
			model: AModelFile,
			uvlock: Boolean,
			weight: Int = DEFAULT_WEIGHT
		): Array<AConfiguredModel>
		{
			return validRotations()
				.mapToObj { x: Int ->
					allYRotations(
						model,
						x,
						uvlock,
						weight
					)
				}
				.flatMap { array: Array<AConfiguredModel> ->
					Arrays.stream(
						array
					)
				}.collect(Collectors.toList()).toTypedArray()
		}

		fun checkRotation(rotationX: Int, rotationY: Int)
		{
			Preconditions.checkArgument(
				BlockModelRotation.by(rotationX, rotationY) != null,
				"Invalid model rotation x=%d, y=%d",
				rotationX,
				rotationY
			)
		}

		fun checkWeight(weight: Int)
		{
			Preconditions.checkArgument(
				weight >= 1,
				"Model weight must be greater than or equal to 1. Found: %d",
				weight
			)
		}

		/** Creates a standalone [Builder] with no owning-builder callback; use [Builder.build]/[Builder.buildLast]. */
		fun builder(block: Builder<*>.() -> Unit = {}): Builder<*>
		{
			return Builder<Any>().apply(block)
		}

		fun builder(
			outer: AVariantBlockStateBuilder,
			state: AVariantBlockStateBuilder.PartialBlockstate
		): Builder<AVariantBlockStateBuilder>
		{
			return Builder({ models: Array<AConfiguredModel> ->
				outer.setModels(
					state,
					*models
				)
			}, ImmutableList.of())
		}

		fun builder(outer: AMultiPartBlockStateBuilder): Builder<AMultiPartBlockStateBuilder.PartBuilder>
		{
			return Builder(
				{ models: Array<AConfiguredModel> ->
					val ret: AMultiPartBlockStateBuilder.PartBuilder =
						outer.PartBuilder(
							ABlockStateProvider.ConfiguredModelList(*models)
						)
					outer.addPart(ret)
					ret
				}, ImmutableList.of()
			)
		}
	}
}