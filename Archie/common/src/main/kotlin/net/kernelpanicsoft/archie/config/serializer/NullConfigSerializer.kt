package net.kernelpanicsoft.archie.config.serializer

import net.kernelpanicsoft.archie.config.ConfigSpec
import net.kernelpanicsoft.archie.config.IConfigSerializer
import java.nio.file.Path

/**
 * No-op [IConfigSerializer]: [load] and [save] do nothing, and the string-based methods all throw.
 * Useful as a [ConfigSpec.fileSerializer] override for a spec that should never persist to disk
 * (e.g. an in-memory-only or test config).
 */
object NullConfigSerializer : IConfigSerializer
{
	override fun configPath(config: ConfigSpec, configFolder: Path): Path
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

	override fun load(config: ConfigSpec, configFolder: Path) = Unit

	override fun save(config: ConfigSpec, configFolder: Path) = Unit
}