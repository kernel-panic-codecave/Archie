package com.withertech.archie.data.client.model

import com.google.common.base.Preconditions
import com.google.gson.JsonObject
import net.minecraft.resources.ResourceLocation

abstract class ACustomLoaderBuilder<T : AModelBuilder<T>> protected constructor(
	val loaderId: ResourceLocation,
	protected val parent: T,
	val allowInlineElements: Boolean
)
{
	protected val visibility: MutableMap<String, Boolean> = LinkedHashMap()
	private var optional = false

	/**
	 * @param loaderId           The ID of the associated [IGeometryLoader]
	 * @param parent             The parent [AModelBuilder]
	 */
	@Deprecated("Use {@link #CustomLoaderBuilder(ResourceLocation, ModelBuilder, boolean)}} instead")
	protected constructor(
		loaderId: ResourceLocation,
		parent: T,
	) : this(loaderId, parent, false)

	fun visibility(partName: String, show: Boolean): ACustomLoaderBuilder<T>
	{
		Preconditions.checkNotNull(partName, "partName must not be null")
		visibility[partName] = show
		return this
	}

	/**
	 * Mark the custom loader as optional for this model to allow it to be loaded through vanilla paths
	 * if the loader is not present
	 */
	fun optional(): ACustomLoaderBuilder<T>
	{
		Preconditions.checkState(
			allowInlineElements,
			"Only loaders with support for inline elements can be marked as optional"
		)
		this.optional = true
		return this
	}

	fun end(): T
	{
		return parent
	}

	open fun toJson(json: JsonObject): JsonObject
	{
		if (optional)
		{
			val loaderObj = JsonObject()
			loaderObj.addProperty("id", loaderId.toString())
			loaderObj.addProperty("optional", true)
			json.add("loader", loaderObj)
		} else
		{
			json.addProperty("loader", loaderId.toString())
		}

		if (visibility.isNotEmpty())
		{
			val visibilityObj = JsonObject()

			for ((key, value) in visibility)
			{
				visibilityObj.addProperty(key, value)
			}

			json.add("visibility", visibilityObj)
		}

		return json
	}
}