package net.kernelpanicsoft.archie.config

import dev.architectury.event.events.client.ClientLifecycleEvent
import dev.architectury.event.events.client.ClientPlayerEvent
import dev.architectury.event.events.common.LifecycleEvent
import dev.architectury.event.events.common.PlayerEvent
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
import net.kernelpanicsoft.archie.config.ConfigSpec.Server.Companion.CONFIG_DIR
import net.kernelpanicsoft.archie.networking.NetworkChannel
import net.kernelpanicsoft.archie.serialization.SerializationManager
import net.kernelpanicsoft.archie.util.foldEnv
import net.kernelpanicsoft.archie.util.isClient
import net.kernelpanicsoft.archie.util.rem
import net.minecraft.network.chat.Component
import net.minecraft.world.level.storage.LevelResource
import java.nio.file.Path
import java.util.function.Predicate
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

/**
 * The root of a mod's config. [ConfigSpec] is sealed - declare one or more nested singleton
 * `object`s subclassing [Common], [Client], [Server], or [Startup] (nested inside a
 * [ConfigContainer]) depending on when the config should load and whether it should sync.
 * [categories] are discovered automatically from nested [CategorySpec] objects - no need to
 * override anything:
 * ```kotlin
 * object Config : ConfigContainer(MyMod.MOD) {
 *     object MyConfig : ConfigSpec.Common(MyMod.MOD, Component.literal("My Config")) {
 *         object General : CategorySpec(Component.literal("General"), "general") { ... }
 *         object Advanced : CategorySpec(Component.literal("Advanced"), "advanced") { ... }
 *     }
 * }
 * ```
 * Call [ConfigContainer.init] once during common mod init (on both physical sides); it loads (or
 * creates) the config file(s) and, on the client, also registers the Cloth Config UI screen(s).
 * There is no separate client-side init step to call.
 *
 * @param mod The owning mod, used to derive the default [filename] and locate the config
 * directory.
 * @param title Display title of the config, shown as the Cloth Config screen title.
 * @param id Unique identifier for this config, used to derive the default [filename] and
 * register the network channel. Defaults to the snake-cased [title].
 */
@Suppress("unused")
sealed class ConfigSpec(val type: Type, val mod: Mod, val title: Component, val id: String = title.string.toSnakeCase())
{
	internal val channel = NetworkChannel(mod % id)
	/** Client-side mirror of this spec, used to build the Cloth Config UI screen. */
	internal val client by lazy { ClientConfigSpec(this) }

	open val synchronized = false

	/** Top-level sections of this config. */
	val categories: List<CategorySpec>
		get() = this::class.nestedClasses
			.filterIsInstance<KClass<out CategorySpec>>()
			.filter { it.isSubclassOf(CategorySpec::class) }
			.mapNotNull { klass -> klass.objectInstance }

	/** [categories] indexed by [DataSpec.id]. */
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
	open val filename: String = "${mod.modId}/${id}"

	/** Whether [load] has run at least once. */
	var isLoaded: Boolean = false
		protected set

	var configFolder: Path = Platform.getConfigFolder()
		protected set

	private var isEventsRegistered: Boolean = false

	abstract val predicate: () -> Boolean

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
	open fun init()
	{
		SerializationManager {
			module {
				contextual(this@ConfigSpec::class) {
					serializer
				}
			}
		}
		categoriesMap.values.forEach { cat ->
			cat.init()
		}
		if (synchronized && !isEventsRegistered)
		{
			channel.configServerbound(this)
			channel.configClientbound(this)
			channel.register()
			foldEnv(
				client = {
					ClientPlayerEvent.CLIENT_PLAYER_QUIT.register {
						this.isLoaded = false
					}
				},
				server = {
					PlayerEvent.PLAYER_JOIN.register { player ->
						if (!player.server.isSingleplayer)
							channel.toPlayer(player, this)

					}
				}
			)
		}
		isEventsRegistered = true
	}


	/** Reads the config file via [fileSerializer], creating it with defaults if absent, and marks [isLoaded]. */
	fun load() = fileSerializer.load(this, configFolder).also { isLoaded = true }

	/** Writes the current values of every field in [categories] to the config file via [fileSerializer]. */
	fun save() = fileSerializer.save(this, configFolder)

	/** Serializes/deserializes a [ConfigSpec] by delegating each entry of [categoriesMap] to its own [DataSpec.serializer]. */
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

	enum class Type
	{
		COMMON,
		CLIENT,
		SERVER,
		STARTUP
	}

	abstract class Common(mod: Mod, title: Component = Component.literal("Common"), id: String = title.string.toSnakeCase()) : ConfigSpec(
		Type.COMMON,
		mod,
		title,
		id,
	)
	{
		private var isEventsRegistered: Boolean = false

		override fun init()
		{
			super.init()
			if (!isEventsRegistered)
			{
				LifecycleEvent.SETUP.register {
					load()
				}
			}
			isEventsRegistered = true
		}

		override val predicate: () -> Boolean = { isLoaded }
	}

	abstract class Client(mod: Mod, title: Component = Component.literal("Client"), id: String = title.string.toSnakeCase()) : ConfigSpec(
		Type.CLIENT,
		mod,
		title,
		id,
	)
	{
		private var isEventsRegistered: Boolean = false

		override fun init()
		{
			super.init()
			if (!isEventsRegistered)
			{
				onClient {
					ClientLifecycleEvent.CLIENT_SETUP.register {
						load()
					}
				}
			}
			isEventsRegistered = true
		}

		override val predicate: () -> Boolean = { isLoaded && isClient }
	}

	abstract class Server(mod: Mod, title: Component = Component.literal("Server"), id: String = title.string.toSnakeCase()) : ConfigSpec(
		Type.SERVER,
		mod,
		title,
		id,
	) {
		private var isEventsRegistered: Boolean = false

		override val synchronized: Boolean = true

		override fun init()
		{
			super.init()
			if (!isEventsRegistered)
			{
				LifecycleEvent.SERVER_BEFORE_START.register { server ->
					configFolder = server.getWorldPath(CONFIG_DIR)
					load()
				}
			}
			isEventsRegistered = true
		}

		override val predicate: () -> Boolean = { isLoaded }

		companion object
		{
			private val CONFIG_DIR = LevelResource("serverconfig")
		}
	}

	abstract class Startup(mod: Mod, title: Component = Component.literal("Startup"), id: String = title.string.toSnakeCase()) : ConfigSpec(
		Type.STARTUP,
		mod,
		title,
		id,
	)
	{
		override fun init()
		{
			super.init()
			load()
		}

		override val predicate: () -> Boolean = { isLoaded }
	}

	/** [KSerializer] for this spec, used by [fileSerializer] to read/write the config file. */
	internal val serializer by lazy { ConfigSerializer { this } }
}