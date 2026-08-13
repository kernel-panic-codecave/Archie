package net.kernelpanicsoft.archie.config

import dev.architectury.platform.Platform
import net.kernelpanicsoft.archie.Archie
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/**
 * Reads and writes a [ConfigSpec] to/from a specific file format (JSON, JSON5, TOML, ...). Built-in
 * implementations live in `net.kernelpanicsoft.archie.config.serializer`; [ConfigSpec.fileSerializer]
 * picks one per-platform by default.
 *
 * [configPath], [load], and [save] all take a `configFolder` that defaults to the platform's shared
 * config folder ([Platform.getConfigFolder]); [ConfigSpec] passes its own [ConfigSpec.configFolder]
 * instead, which a [ConfigSpec.Server] repoints at the current world's per-save `serverconfig/` folder.
 */
interface IConfigSerializer
{
	/** File the given [config] is read from and written to, resolved under [configFolder]. */
	fun configPath(config: ConfigSpec, configFolder: Path = Platform.getConfigFolder()): Path
	/**
	 * Reads [config]'s file if present via [loadString], then always [save]s it back out - this
	 * both formats a freshly-created file with defaults and rewrites an existing one with any
	 * newly-added fields. If the existing file fails to parse, it's logged and renamed to
	 * `<file>.corrupted` rather than deleted, and loading falls through to writing fresh defaults
	 * so startup isn't blocked.
	 */
	fun load(config: ConfigSpec, configFolder: Path = Platform.getConfigFolder())
	{
		val path = configPath(config, configFolder)
		if (Files.exists(path))
		{
			try
			{
				val string = Files.readString(path)
				loadString(config, string)
			}
			catch (e: Throwable)
			{
				// A malformed/corrupt file must not permanently block startup. Back the bad
				// file up rather than deleting it, log it, and fall through to save(config)
				// below so a fresh default file gets written and the mod still loads.
				Archie.LOGGER.error("Failed to load config at $path, resetting to defaults. The invalid file was backed up.", e)
				runCatching {
					Files.move(path, path.resolveSibling("${path.fileName}.corrupted"), StandardCopyOption.REPLACE_EXISTING)
				}
			}
		}

		save(config, configFolder)
	}

	/** Parses [string] and populates [config]'s fields from it. Implemented per-format. */
	fun loadString(config: ConfigSpec, string: String)

	/** Writes [config]'s current field values to [configPath], creating parent directories as needed. */
	fun save(config: ConfigSpec, configFolder: Path = Platform.getConfigFolder())
	{
		val path = configPath(config, configFolder)
		try
		{
			Files.createDirectories(path.parent)
			Files.writeString(path, saveString(config))
		}
		catch (e: Throwable)
		{
			throw SerializationException(e)
		}
	}

	/** Renders [config]'s current field values as a file-format string. Implemented per-format. */
	fun saveString(config: ConfigSpec): String

	/** Thrown when writing a config file fails (e.g. an I/O error). */
	class SerializationException(cause: Throwable) : Exception(cause)
}