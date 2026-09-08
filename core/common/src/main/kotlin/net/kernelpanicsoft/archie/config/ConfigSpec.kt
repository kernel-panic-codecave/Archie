package net.kernelpanicsoft.archie.config

import dev.architectury.event.events.client.ClientLifecycleEvent
import dev.architectury.event.events.client.ClientPlayerEvent
import dev.architectury.event.events.common.LifecycleEvent
import dev.architectury.event.events.common.PlayerEvent
import net.kernelpanicsoft.archie.config.serializer.Json5ConfigSerializer
import net.kernelpanicsoft.archie.config.serializer.TomlConfigSerializer
import net.kernelpanicsoft.archie.APlatform
import net.kernelpanicsoft.archie.util.onClient
import dev.architectury.utils.GameInstance
import dev.architectury.platform.Mod
import dev.architectury.platform.Platform
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.*
import net.kernelpanicsoft.archie.config.ConfigSpec.Server.Companion.CONFIG_DIR
import net.kernelpanicsoft.archie.gui.util.KColor
import net.kernelpanicsoft.archie.networking.NetworkChannel
import net.kernelpanicsoft.archie.serialization.SerializationManager
import net.kernelpanicsoft.archie.util.foldEnv
import net.kernelpanicsoft.archie.util.isClient
import net.kernelpanicsoft.archie.util.minecraftServer
import net.kernelpanicsoft.archie.util.rem
import net.kernelpanicsoft.archie.util.sendSystemMessage
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.storage.LevelResource
import java.nio.file.Files
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
sealed class ConfigSpec(val type: Type, val mod: Mod, override val title: Component, override val id: String = title.string.toSnakeCase()) : ConfigNode
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

	/**
	 * Path segments this spec is nested under, assigned top-down when a [ConfigGroup]/[ConfigContainer]
	 * discovers it. Left empty for an entry of a [ConfigSpecCollection], whose [configFolder] is
	 * pointed directly at its resolved directory instead (see [filenamePrefixed]).
	 */
	internal var pathSegments: List<String> = emptyList()

	/** When `false`, [filename] is just [id] with no [mod]/[pathSegments] prefix - set by [ConfigSpecCollection] on its entries. */
	internal var filenamePrefixed: Boolean = true

	/**
	 * Set by [ConfigSpecCollection.createEntry] on an entry it creates - tells [init] to skip this
	 * instance's own standalone [registerSync]/[NetworkChannel.register] (a new payload type per
	 * entry can't be negotiated after the server has already accepted connections), since a
	 * synchronized collection instead syncs all its entries over one shared channel.
	 */
	internal var partOfCollection: Boolean = false

	/**
	 * Set by [ConfigSpecCollection.createEntry] on a synchronized collection's entry: routes this
	 * entry's client -> server save through the collection's shared channel instead of this
	 * instance's own (unregistered, since [partOfCollection]) one. Consulted by `ClientConfigSpec`.
	 */
	internal var collectionSync: (() -> Unit)? = null

	/** Path of the config file, relative to [configFolder], without extension. */
	open val filename: String
		get() = if (filenamePrefixed) (listOf(mod.modId) + pathSegments + id).joinToString("/") else id

	/** Whether [load] has run at least once, or (for a synced spec) a value has been received from the server. */
	var isLoaded: Boolean = false
		internal set

	/**
	 * Whether [isLoaded] is owed to a server's push rather than to this side's own [load].
	 *
	 * Distinguishes the two ways a synced spec becomes readable, which matters exactly once: on
	 * client disconnect, where only the pushed kind should be discarded. See the client-quit handler
	 * in [init].
	 */
	private var loadedFromSync: Boolean = false

	var configFolder: Path = Platform.getConfigFolder()
		internal set

	private var isEventsRegistered: Boolean = false

	abstract val predicate: () -> Boolean

	/**
	 * Registers all [categories] (and their subcategories) and, for a [synchronized] spec,
	 * registers this spec's [NetworkChannel] plus the player-join/quit listeners that push it to
	 * joining clients and reset [isLoaded] on disconnect.
	 *
	 * This does **not** load the config file or touch the client UI - each of [Common], [Client],
	 * [Server], and [Startup] overrides this to additionally register the lifecycle event
	 * (documented on that subclass) that calls [load] at the right time. The client-side settings
	 * screen is built and registered separately, once every nested spec in the container is
	 * initialized, by [ConfigContainer.initClient]. Call [ConfigContainer.init], not this method
	 * directly.
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
			cat.registerSpecCollectionNetworking(this)
			cat.wireAccessPredicate(this)
		}
		if (synchronized && !isEventsRegistered && !partOfCollection)
		{
			registerSync()
			channel.register()
			foldEnv(
				client = {
					ClientPlayerEvent.CLIENT_PLAYER_QUIT.register {
						if (this.loadedFromSync)
						{
							this.loadedFromSync = false
							this.isLoaded = false
						}
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


	/**
	 * Reads the config file via [fileSerializer], creating it with defaults if absent, marks
	 * [isLoaded], and attaches/scans every [DataSpec.configSpecList]/[DataSpec.configSpecMap]
	 * field reachable from [categories] - deferred to here, rather than [init], since [configFolder]
	 * isn't final until just before this runs (a [Server] spec only repoints it at the per-world
	 * folder right before calling [load]).
	 */
	fun load() = fileSerializer.load(this, configFolder).also {
		isLoaded = true
		// This side's own file, not a push - so a client disconnect must not discard it.
		loadedFromSync = false
		categoriesMap.values.forEach { cat -> cat.attachSpecCollections(this) }
	}

	/** Writes the current values of every field in [categories] to the config file via [fileSerializer]. */
	fun save() = fileSerializer.save(this, configFolder)

	/** Deletes this spec's file. Used by [ConfigSpecCollection.remove] on one of its entries. */
	internal fun deleteFile()
	{
		Files.deleteIfExists(fileSerializer.configPath(this, configFolder))
		isLoaded = false
	}

	/**
	 * Wires this [synchronized] spec's sync packets on [channel]: a server-bound edit that's
	 * only decoded (applying it to this live singleton, since [serializer]'s factory always
	 * returns `this`) after [Player.hasPermissions] passes - an unprivileged client's edit is
	 * never applied, just rejected with a message and corrected back to the current value - and a
	 * client-bound push that decodes and saves unconditionally, trusting the server.
	 */
	@Suppress("UNCHECKED_CAST")
	private fun registerSync()
	{
		val klass = this::class as KClass<ConfigSpec>
		channel.serverboundLazy(klass, serializer) { decode, ctx ->
			val player = ctx.player
			if (player.hasPermissions(3))
			{
				decode()
				save()
				channel.toAllPlayers(this)
			}
			else
			{
				player.sendSystemMessage {
					style {
						color = KColor.RED.toTextColor()
						underlined = true
					}
					translate("archie.networking.config.no_permissions")
				}
				channel.toPlayer(player as ServerPlayer, this)
			}
		}
		channel.clientbound(klass, serializer) { config, _ ->
			config.isLoaded = true
			config.loadedFromSync = minecraftServer == null
			config.save()
		}
	}

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
			internal val CONFIG_DIR = LevelResource("serverconfig")
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