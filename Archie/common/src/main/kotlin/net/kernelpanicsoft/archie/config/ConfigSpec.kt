package net.kernelpanicsoft.archie.config

import net.kernelpanicsoft.archie.config.serializer.Json5ConfigSerializer
import net.kernelpanicsoft.archie.config.serializer.TomlConfigSerializer
import net.kernelpanicsoft.archie.APlatform
import dev.architectury.platform.Mod
import dev.architectury.platform.Platform
import dev.architectury.utils.Env
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.*
import me.shedaniel.clothconfig2.api.ConfigBuilder
import net.minecraft.network.chat.Component

@Suppress("unused")
abstract class ConfigSpec(val mod: Mod, val title: Component)
{
	internal val client by lazy { ClientConfigSpec(this) }

	protected abstract val categories: List<CategorySpec>

	internal val categoriesMap: Map<String, CategorySpec> by lazy {
		categories.associateBy { it.id }
	}

	protected open val fileSerializer: IConfigSerializer = when (val platform = APlatform.platform)
	{
		"fabric" -> Json5ConfigSerializer
		"neoforge" -> TomlConfigSerializer
		else -> throw UnsupportedOperationException("Unsupported platform: $platform")
	}

	open val filename: String = "${mod.modId}/${title.string.toSnakeCase()}"

	val isLoaded: Boolean get() = _isLoaded
	private var _isLoaded: Boolean = false

	fun init()
	{
		categoriesMap.values.forEach { cat ->
			cat.init()
		}
		load()
	}

	fun initClient()
	{
		if (Platform.isModLoaded("cloth_config"))
			client.initClient()
	}

	@Suppress("MemberVisibilityCanBePrivate")
	fun load() = fileSerializer.load(this).also { _isLoaded = true }

	@Suppress("MemberVisibilityCanBePrivate")
	fun save() = fileSerializer.save(this)

	internal class ConfigSerializer(val factory: () -> ConfigSpec) : KSerializer<ConfigSpec>
	{
		override val descriptor: SerialDescriptor by lazy {
			with(factory())
			{
				buildClassSerialDescriptor(title.string)
				{
					categoriesMap.forEach { (key, value) ->
						element(key, value.serializer.descriptor)
					}
				}
			}
		}

		override fun deserialize(decoder: Decoder): ConfigSpec
		{
			return decoder.decodeStructure(descriptor)
			{
				val spec = factory()
				with(spec)
				{
					while (true)
					{
						when (val index = decodeElementIndex(descriptor))
						{
							in categoriesMap.entries.indices ->
							{
								val (_, value) = categoriesMap.entries.toList()[index]
								decodeSerializableElement(descriptor, index, value.serializer)
							}

							CompositeDecoder.DECODE_DONE -> break
							else -> error("Unexpected index: $index")
						}
					}
				}
				spec
			}
		}

		override fun serialize(encoder: Encoder, value: ConfigSpec)
		{
			encoder.encodeStructure(descriptor)
			{
				value.categoriesMap.entries.forEachIndexed { index, (_, value) ->
					encodeSerializableElement(descriptor, index, value.serializer, value)
				}
			}
		}
	}


	internal val serializer by lazy { ConfigSerializer { this } }
}