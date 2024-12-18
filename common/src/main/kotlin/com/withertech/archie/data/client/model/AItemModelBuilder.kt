package com.withertech.archie.data.client.model

import com.google.common.base.Preconditions
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.minecraft.resources.ResourceLocation

class AItemModelBuilder(
	outputLocation: ResourceLocation,
) : AModelBuilder<AItemModelBuilder>(outputLocation)
{
	protected var overrides: MutableList<OverrideBuilder> = ArrayList()

	fun override(block: OverrideBuilder.() -> Unit = {}): OverrideBuilder
	{
		val ret = OverrideBuilder().apply(block)
		overrides.add(ret)
		return ret
	}

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

	inner class OverrideBuilder
	{
		private var model: AModelFile? = null
		private val predicates: MutableMap<ResourceLocation, Float> = LinkedHashMap()

		fun model(model: AModelFile): OverrideBuilder
		{
			this.model = model
			return this
		}

		fun predicate(key: ResourceLocation, value: Float): OverrideBuilder
		{
			predicates[key] = value
			return this
		}

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