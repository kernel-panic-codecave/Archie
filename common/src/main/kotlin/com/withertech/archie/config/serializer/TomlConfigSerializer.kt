package com.withertech.archie.config.serializer

import com.withertech.archie.config.ConfigSpec
import com.withertech.archie.config.IConfigSerializer
import dev.architectury.platform.Platform
import net.peanuuutz.tomlkt.Toml
import net.peanuuutz.tomlkt.TomlIndentation
import java.nio.file.Path

object TomlConfigSerializer : IConfigSerializer
{
	private val toml = Toml {
		ignoreUnknownKeys = true
		indentation = TomlIndentation.Tab
	}

	override fun configPath(config: ConfigSpec): Path
	{
		return Platform.getConfigFolder().resolve("${config.filename}.toml")
	}

	override fun loadString(config: ConfigSpec, string: String)
	{
		toml.decodeFromString(config.serializer, string)
	}

	override fun saveString(config: ConfigSpec): String
	{
		return toml.encodeToString(config.serializer, config)
	}

}