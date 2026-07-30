package net.kernelpanicsoft.archie.config

import net.kernelpanicsoft.archie.config.serializer.Json5ConfigSerializer
import net.kernelpanicsoft.archie.config.serializer.TomlConfigSerializer
import net.kernelpanicsoft.archie.APlatform
import net.kernelpanicsoft.archie.util.onClient
import dev.architectury.platform.Mod
import dev.architectury.platform.Platform
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.*
import me.shedaniel.clothconfig2.api.ConfigBuilder
import net.minecraft.network.chat.Component

/**
 * The root of a mod's config, declared by subclassing this as a singleton `object` and listing
 * its top-level [categories]:
 * ```kotlin
 * object MyConfig : ConfigSpec(MyMod.MOD, Component.literal("My Config")) {
 *     override val categories = listOf(General, Advanced)
 *     object General : CategorySpec(Component.literal("General"), "general") { ... }
 *     object Advanced : CategorySpec(Component.literal("Advanced"), "advanced") { ... }
 * }
 * ```
 * Call [init] once during common mod init (on both physical sides); it loads (or creates) the
 * config file and, on the client, also registers the Cloth Config UI screen. There is no separate
 * client-side init step to call.
 *
 * @param mod The owning mod, used to derive the default [filename] and locate the config
 * directory.
 * @param title Display title of the config, shown as the Cloth Config screen title.
 */
@Suppress("unused")
abstract class ConfigSpec(val mod: Mod, val title: Component)
{
	/** Client-side mirror of this spec, used to build the Cloth Config UI screen. */
	internal val client by lazy { ClientConfigSpec(this) }

	/** Top-level sections of this config. */
	protected abstract val categories: List<CategorySpec>

	/** [categories] indexed by [CategorySpec.id]. */
	internal val categoriesMap: Map<String, CategorySpec> by lazy {
		categories.associateBy { it.id }
	}

	/**
	 * Serializer used to read/write the config file. Defaults per-platform: JSON5 on Fabric,
	 * TOML on NeoForge. Override to force a specific format regardless of platform.
	 */
	protected open val fileSerializer: IConfigSerializer = when (val platform = APlatform.platform)
	{
		"fabric" -> Json5ConfigSerializer
		"neoforge" -> TomlConfigSerializer
		else -> throw UnsupportedOperationException("Unsupported platform: $platform")
	}

	/** Path of the config file, relative to the game's config directory, without extension. */
	open val filename: String = "${mod.modId}/${title.string.toSnakeCase()}"

	/** Whether [load] has run at least once. */
	val isLoaded: Boolean get() = _isLoaded
	private var _isLoaded: Boolean = false

	/**
	 * Registers all [categories] (and their subcategories) and [load]s the config file, then, on
	 * the client, builds and registers the Cloth Config UI screen via [initClient]. Call once
	 * during common mod init, on both physical sides.
	 *
	 * [initClient] is invoked from here - synchronously, during the common `main` entrypoint -
	 * rather than being left for callers to invoke from their own client entrypoint. Fabric loader
	 * runs every mod's `main` entrypoint before any mod's `client` entrypoint, so this guarantees
	 * the screen is registered with [AConfigPlatform] before Catalogue's own client entrypoint
	 * takes its one-time snapshot of `configFactory` providers. Registering later (e.g. from a
	 * `client` entrypoint) races that snapshot: depending on unrelated mods' load order, the
	 * config button would intermittently be missing from Catalogue's mod list.
	 */
	fun init()
	{
		categoriesMap.values.forEach { cat ->
			cat.init()
		}
		load()
		onClient { initClient() }
	}

	/** Builds the Cloth Config UI screen for this spec, if Cloth Config is present. */
	internal fun initClient()
	{
		// Cloth Config's mod id differs by loader: Fabric allows hyphens ("cloth-config"), while
		// NeoForge's mod id charset doesn't, so its variant registers as "cloth_config" instead.
		val clothConfigModId = when (val platform = APlatform.platform)
		{
			"fabric" -> "cloth-config"
			"neoforge" -> "cloth_config"
			else -> throw UnsupportedOperationException("Unsupported platform: $platform")
		}
		if (Platform.isModLoaded(clothConfigModId))
			client.initClient()
	}

	/** Reads the config file via [fileSerializer], creating it with defaults if absent, and marks [isLoaded]. */
	@Suppress("MemberVisibilityCanBePrivate")
	fun load() = fileSerializer.load(this).also { _isLoaded = true }

	/** Writes the current values of every field in [categories] to the config file via [fileSerializer]. */
	@Suppress("MemberVisibilityCanBePrivate")
	fun save() = fileSerializer.save(this)

	/** Serializes/deserializes a [ConfigSpec] by delegating each entry of [categoriesMap] to its own [CategorySpec.serializer]. */
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


	/** [KSerializer] for this spec, used by [fileSerializer] to read/write the config file. */
	internal val serializer by lazy { ConfigSerializer { this } }
}