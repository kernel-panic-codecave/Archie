package com.withertech.archie.data

import dev.architectury.injectables.annotations.ExpectPlatform

object ArchieDataGeneratorPlatform
{
	@get:ExpectPlatform
	@JvmStatic
	val isDataGen: Boolean
		get()
		{
			throw IllegalStateException()
		}
}