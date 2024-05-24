package com.withertech.archie.data.client.model

import net.minecraft.resources.ResourceLocation

open class ModelFile(val location: ResourceLocation)
{
	constructor(location: String) : this(ResourceLocation(location))

	override fun toString(): String
	{
		return location.toString()
	}

}