package net.kernelpanicsoft.archie.config.serializer

import net.kernelpanicsoft.archie.config.ConfigSpec
import net.kernelpanicsoft.archie.config.IConfigSerializer
import dev.architectury.platform.Platform
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import java.nio.file.Path

/** [IConfigSerializer] for plain JSON (no comments). Not used by default on any platform - opt in explicitly by overriding [ConfigSpec.fileSerializer]. */
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