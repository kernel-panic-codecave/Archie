package net.kernelpanicsoft.archie.config.serializer

import net.kernelpanicsoft.archie.config.ConfigSpec
import net.kernelpanicsoft.archie.config.IConfigSerializer
import io.github.xn32.json5k.Json5
import java.nio.file.Path


/** [IConfigSerializer] for the JSON5 format (JSON with comments). Archie's default on Fabric. */
object Json5ConfigSerializer : IConfigSerializer
{
	private val json5 = Json5 {
		prettyPrint = true
		quoteMemberNames = true
		encodeDefaults = true
	}
	override fun configPath(config: ConfigSpec, configFolder: Path): Path
	{
		return configFolder.resolve("${config.filename}.json5")
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