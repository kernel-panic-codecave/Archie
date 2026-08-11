package net.kernelpanicsoft.archie.data.client.model

import com.google.common.base.Preconditions
import com.google.gson.JsonObject
import net.minecraft.resources.ResourceLocation

/**
 * Base for a custom geometry loader's model JSON, embedded in an [AModelBuilder] via
 * [AModelBuilder.customLoader]. Subclasses add their loader's own fields by overriding
 * [toJson]; this base handles the common `loader`/`visibility`/`optional` fields.
 */
abstract class ACustomLoaderBuilder<T : AModelBuilder<T>> protected constructor(
	/** The id of the associated geometry loader. */
	val loaderId: ResourceLocation,
	/** The [AModelBuilder] this loader is being configured on; returned by [end]. */
	protected val parent: T,
	val allowInlineElements: Boolean
)
{
	protected val visibility: MutableMap<String, Boolean> = LinkedHashMap()
	private var optional = false

	@Deprecated("Use the (loaderId, parent, allowInlineElements) constructor instead")
	protected constructor(
		loaderId: ResourceLocation,
		parent: T,
	) : this(loaderId, parent, false)

	/** Sets whether the model part named [partName] is initially visible. */
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

	/** Returns to the enclosing [AModelBuilder] this loader was configured on. */
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