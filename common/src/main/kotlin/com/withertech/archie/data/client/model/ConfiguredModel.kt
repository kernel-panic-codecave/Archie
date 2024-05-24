package com.withertech.archie.data.client.model

import com.google.common.base.Preconditions
import com.google.common.collect.ImmutableList
import com.google.common.collect.ObjectArrays
import com.google.gson.JsonObject
import net.minecraft.client.resources.model.BlockModelRotation
import java.util.*
import java.util.function.Function
import java.util.stream.Collectors
import java.util.stream.IntStream

class ConfiguredModel @JvmOverloads constructor(
	model: ModelFile,
	rotationX: Int = 0,
	rotationY: Int = 0,
	uvLock: Boolean = false,
	weight: Int = DEFAULT_WEIGHT
)
{
	val model: ModelFile
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
	 * A builder for [ConfiguredModel]s, which can contain a callback for
	 * processing the finished result. If no callback is available (e.g. in the case
	 * of [ConfiguredModel.builder]), some methods will not be available.
	 *
	 *
	 * Multiple models can be configured at once through the use of
	 * [.nextModel].
	 *
	 * @param <T> the type of the owning builder, which supplied the callback, and
	 * will be returned upon completion.
	</T> */
	class Builder<T> @JvmOverloads internal constructor(
		private val callback: Function<Array<ConfiguredModel>, T>? = null,
		private var otherModels: List<ConfiguredModel> = listOf()
	)
	{
		private var model: ModelFile? = null
		private var rotationX = 0
		private var rotationY = 0
		private var uvLock = false
		private var weight = DEFAULT_WEIGHT

		fun modelFile(model: ModelFile): Builder<T>
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

		fun buildLast(): ConfiguredModel
		{
			return ConfiguredModel(model!!, rotationX, rotationY, uvLock, weight)
		}

		fun build(): Array<ConfiguredModel>
		{
			return ObjectArrays.concat(otherModels.toTypedArray(), buildLast())
		}

		fun addModel(): T
		{
			Preconditions.checkNotNull(callback, "Cannot use addModel() without an owning builder present")
			return callback!!.apply(build())
		}

		fun nextModel(): Builder<T>
		{
			return Builder(callback, build().toList())
		}

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

		@JvmOverloads
		fun allYRotations(
			model: ModelFile,
			x: Int,
			uvlock: Boolean,
			weight: Int = DEFAULT_WEIGHT
		): Array<ConfiguredModel>
		{
			return validRotations()
				.mapToObj { y: Int -> ConfiguredModel(model, x, y, uvlock, weight) }
				.collect(Collectors.toList()).toTypedArray()
		}

		@JvmOverloads
		fun allRotations(
			model: ModelFile,
			uvlock: Boolean,
			weight: Int = DEFAULT_WEIGHT
		): Array<ConfiguredModel>
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
				.flatMap { array: Array<ConfiguredModel> ->
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

		fun builder(block: Builder<*>.() -> Unit = {}): Builder<*>
		{
			return Builder<Any>().apply(block)
		}

		fun builder(
			outer: VariantBlockStateBuilder,
			state: VariantBlockStateBuilder.PartialBlockstate
		): Builder<VariantBlockStateBuilder>
		{
			return Builder({ models: Array<ConfiguredModel> ->
				outer.setModels(
					state,
					*models
				)
			}, ImmutableList.of())
		}

		fun builder(outer: MultiPartBlockStateBuilder): Builder<MultiPartBlockStateBuilder.PartBuilder>
		{
			return Builder(
				{ models: Array<ConfiguredModel> ->
					val ret: MultiPartBlockStateBuilder.PartBuilder =
						outer.PartBuilder(
							ArchieBlockStateProvider.ConfiguredModelList(*models)
						)
					outer.addPart(ret)
					ret
				}, ImmutableList.of()
			)
		}
	}
}