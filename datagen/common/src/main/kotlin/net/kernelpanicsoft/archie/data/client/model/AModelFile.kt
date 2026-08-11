package net.kernelpanicsoft.archie.data.client.model

import net.minecraft.resources.ResourceLocation

/** A reference to a model JSON file by [location], usable as another model's `parent` or a variant's model. */
open class AModelFile(val location: ResourceLocation)
{
	constructor(location: String) : this(ResourceLocation.parse(location))

	override fun toString(): String
	{
		return location.toString()
	}

}