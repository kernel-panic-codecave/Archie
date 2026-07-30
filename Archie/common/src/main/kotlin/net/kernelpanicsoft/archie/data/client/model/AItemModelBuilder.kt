package net.kernelpanicsoft.archie.data.client.model

import com.google.common.base.Preconditions
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.minecraft.resources.ResourceLocation

/** [AModelBuilder] for an item model at [outputLocation], adding support for `overrides` entries. */
class AItemModelBuilder(
	outputLocation: ResourceLocation,
) : AModelBuilder<AItemModelBuilder>(outputLocation)
{
	protected var overrides: MutableList<OverrideBuilder> = ArrayList()

	/** Adds a new override entry, configured by [block]. */
	fun override(block: OverrideBuilder.() -> Unit = {}): OverrideBuilder
	{
		val ret = OverrideBuilder().apply(block)
		overrides.add(ret)
		return ret
	}

	/** Reconfigures the existing override at [index] with [block]. */
	fun override(index: Int, block: OverrideBuilder.() -> Unit = {}): OverrideBuilder
	{
		Preconditions.checkElementIndex(index, overrides.size, "override")
		return overrides[index].apply(block)
	}

	override fun toJson(): JsonObject
	{
		val root: JsonObject = super.toJson()
		if (overrides.isNotEmpty())
		{
			val overridesJson = JsonArray()
			overrides.stream().map { obj: OverrideBuilder -> obj.toJson() }
				.forEach { element: JsonObject? ->
					overridesJson.add(
						element
					)
				}
			root.add("overrides", overridesJson)
		}
		return root
	}

	/** Builder for a single `overrides` entry: a [model] shown when its [predicate]s are all satisfied. */
	inner class OverrideBuilder
	{
		private var model: AModelFile? = null
		private val predicates: MutableMap<ResourceLocation, Float> = LinkedHashMap()

		/** Sets the model to use when this override's predicates match. */
		fun model(model: AModelFile): OverrideBuilder
		{
			this.model = model
			return this
		}

		/** Requires item property [key] to be at least [value] for this override to apply. */
		fun predicate(key: ResourceLocation, value: Float): OverrideBuilder
		{
			predicates[key] = value
			return this
		}

		/** Returns to the enclosing [AItemModelBuilder]. */
		fun end(): AItemModelBuilder
		{
			return this@AItemModelBuilder
		}

		fun toJson(): JsonObject
		{
			val ret = JsonObject()
			val predicatesJson = JsonObject()
			predicates.forEach { (key: ResourceLocation, `val`: Float?) ->
				predicatesJson.addProperty(
					key.toString(),
					`val`
				)
			}
			ret.add("predicate", predicatesJson)
			ret.addProperty("model", model?.location.toString())
			return ret
		}
	}
}