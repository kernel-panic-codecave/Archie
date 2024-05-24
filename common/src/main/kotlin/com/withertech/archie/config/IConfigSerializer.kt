package com.withertech.archie.config

import java.nio.file.Files
import java.nio.file.Path

interface IConfigSerializer
{
	fun configPath(config: ConfigSpec): Path

	fun load(config: ConfigSpec)
	{
		val path = configPath(config)
		if (Files.exists(path))
		{
			try
			{
				val string = Files.readString(path)
				loadString(config, string)
			}
			catch (e: Throwable)
			{
				throw SerializationException(e)
			}
		}

		save(config)
	}
	fun loadString(config: ConfigSpec, string: String)

	fun save(config: ConfigSpec)
	{
		val path = configPath(config)
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
	fun saveString(config: ConfigSpec): String

	class SerializationException(cause: Throwable) : Exception(cause)
}