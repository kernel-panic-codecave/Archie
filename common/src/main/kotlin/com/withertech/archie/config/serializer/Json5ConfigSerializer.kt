package com.withertech.archie.config.serializer

import com.withertech.archie.config.ConfigSpec
import com.withertech.archie.config.IConfigSerializer
import dev.architectury.platform.Platform
import io.github.xn32.json5k.Json5
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import java.nio.file.Path


object Json5ConfigSerializer : IConfigSerializer
{
	private val json5 = Json5 {
		prettyPrint = true
		quoteMemberNames = true
		encodeDefaults = true
	}
	override fun configPath(config: ConfigSpec): Path
	{
		return Platform.getConfigFolder().resolve("${config.filename}.json5")
	}

	override fun loadString(config: ConfigSpec, string: String)
	{
		json5.decodeFromString(config.serializer, string)
	}

	override fun saveString(config: ConfigSpec): String
	{
		return json5.encodeToString(config.serializer, config)
	}
}