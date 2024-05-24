package com.withertech.archie.config.serializer

import com.withertech.archie.config.ConfigSpec
import com.withertech.archie.config.IConfigSerializer
import java.nio.file.Path

object NullConfigSerializer : IConfigSerializer
{
	override fun configPath(config: ConfigSpec): Path
	{
		throw UnsupportedOperationException()
	}

	override fun loadString(config: ConfigSpec, string: String)
	{
		throw UnsupportedOperationException()
	}

	override fun saveString(config: ConfigSpec): String
	{
		throw UnsupportedOperationException()
	}

	override fun load(config: ConfigSpec) = Unit

	override fun save(config: ConfigSpec) = Unit
}