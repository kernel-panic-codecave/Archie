package com.withertech.archie.config.serializer

import com.withertech.archie.config.ConfigSpec
import com.withertech.archie.config.IConfigSerializer
import dev.architectury.platform.Platform
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import java.nio.file.Path

object JsonConfigSerializer : IConfigSerializer
{
	@OptIn(ExperimentalSerializationApi::class)
	private val json = Json {
		prettyPrint = true
		prettyPrintIndent = "\t"
		ignoreUnknownKeys = true
	}
	override fun configPath(config: ConfigSpec): Path
	{
		return Platform.getConfigFolder().resolve("${config.filename}.json")
	}

	override fun loadString(config: ConfigSpec, string: String)
	{
		json.decodeFromString(config.serializer, string)
	}

	override fun saveString(config: ConfigSpec): String
	{
		return json.encodeToString(config.serializer, config)
	}
}