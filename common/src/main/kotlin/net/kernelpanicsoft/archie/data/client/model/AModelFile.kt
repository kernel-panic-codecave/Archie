package net.kernelpanicsoft.archie.data.client.model

import net.minecraft.resources.ResourceLocation

open class AModelFile(val location: ResourceLocation)
{
	constructor(location: String) : this(ResourceLocation.parse(location))

	override fun toString(): String
	{
		return location.toString()
	}

}