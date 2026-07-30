package net.kernelpanicsoft.archie.config.serializer

import net.kernelpanicsoft.archie.config.ConfigSpec
import net.kernelpanicsoft.archie.config.IConfigSerializer
import dev.architectury.platform.Platform
import net.peanuuutz.tomlkt.Toml
import net.peanuuutz.tomlkt.TomlIndentation
import java.nio.file.Path

/** [IConfigSerializer] for the TOML format. Archie's default on NeoForge. */
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