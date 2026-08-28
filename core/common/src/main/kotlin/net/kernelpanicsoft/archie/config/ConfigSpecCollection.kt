@file:OptIn(ExperimentalSerializationApi::class)

package net.kernelpanicsoft.archie.config

import dev.architectury.event.events.client.ClientPlayerEvent
import dev.architectury.event.events.common.LifecycleEvent
import dev.architectury.event.events.common.PlayerEvent
import dev.architectury.platform.Mod
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import net.kernelpanicsoft.archie.config.ConfigSpec.Server.Companion.CONFIG_DIR
import net.kernelpanicsoft.archie.gui.util.KColor
import net.kernelpanicsoft.archie.networking.NetworkChannel
import net.kernelpanicsoft.archie.serialization.SerializationManager
import net.kernelpanicsoft.archie.util.foldEnv
import net.kernelpanicsoft.archie.util.rem
import net.kernelpanicsoft.archie.util.sendSystemMessage
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import java.nio.file.Files
import java.nio.file.Path

/** One entry's current bytes, pushed server->client and (for a permitted edit) client->server, over a synchronized collection's shared channel. */
@Serializable
internal data class EntrySync(val entryId: String, val data: ByteArray)
{
	override fun equals(other: Any?): Boolean
	{
		if (this === other) return true
		if (other !is EntrySync) return false
		return entryId == other.entryId && data.contentEquals(other.data)
	}

	override fun hashCode(): Int = 31 * entryId.hashCode() + data.contentHashCode()
}

/** Tells clients an entry was deleted server-side, over a synchronized collection's shared channel. */
@Serializable
internal data class EntryRemoved(val entryId: String)

/**
 * A directory-backed, runtime-managed collection of independent [ConfigSpec] files - one file per
 * entry, each an ordinary [ConfigSpec] instance built by [factory] rather than a singleton
 * `object`, since the number of entries isn't known at compile time. Existing files are
 * discovered and loaded from [directory] on [init]. Its Cloth Config UI (a native add/remove
 * [net.kernelpanicsoft.archie.config.builder.ListFieldBuilder]/[net.kernelpanicsoft.archie.config.builder.MapFieldBuilder]
 * row, see [ConfigSpecList]/[ConfigSpecMap]) edits a scratch copy and only calls [reconcile] on
 * save; [add]/[remove] act immediately instead, for programmatic use. Declare [ConfigSpecList] or
 * [ConfigSpecMap], not this directly.
 *
 * A collection can be placed in two ways, and doesn't need to know which:
 * - **Top-level**, as a nested `object` inside a [ConfigContainer]/[ConfigGroup] - [directory]
 *   resolves under that container's own path.
 * - **As a field**, via [DataSpec.configSpecList]/[DataSpec.configSpecMap] - [directory] resolves
 *   as a subfolder next to the *owning* [ConfigSpec]'s own file (e.g. `MyConfig/presets/`).
 *
 * Either way, whatever attaches this collection assigns [baseFolder] before calling [init].
 *
 * @param directory Subfolder (under whatever [baseFolder] resolves to) this collection's files
 * live in. Defaults to [id].
 * @param synchronized Whether [factory] produces [ConfigSpec.Server] entries that should sync to
 * clients, server-authoritative: the server broadcasts which entries exist (a client's own
 * add/remove is disabled in the UI - see `buildCollectionEntry`), and an existing entry's *values*
 * sync like a normal [ConfigSpec.Server]'s (permission-gated edits, reverted with a message if
 * denied). All entries share one channel, registered once at attach time - a fresh payload type
 * per entry can't be negotiated once the server has already accepted connections, so entries can't
 * each get their own the way a standalone [ConfigSpec.Server] does.
 * @param factory Builds a new/loading entry for the given entry id (typically its filename).
 */
@Suppress("unused")
sealed class ConfigSpecCollection<T : ConfigSpec>(
	override val title: Component,
	override val id: String = title.string.toSnakeCase(),
	val directory: String = id,
	val synchronized: Boolean = false,
	private val factory: (entryId: String) -> T,
) : ConfigNode
{
	protected val backing: LinkedHashMap<String, T> = linkedMapOf()

	/**
	 * Resolves the folder this collection's files live in. Assigned once, before [init] is
	 * called, by whatever discovered this collection (see the class doc) - for a [synchronized]
	 * top-level collection, reassigned again (to the per-world folder) at [deferScanToServerStart].
	 */
	internal lateinit var baseFolder: () -> Path

	private lateinit var channel: NetworkChannel
	private var networkRegistered = false
	private var serverStartDeferred = false

	private fun resolvedDirectory(): Path = baseFolder().resolve(directory)

	private fun encode(entry: T): ByteArray = SerializationManager.cbor.encodeToByteArray(entry.serializer, entry)

	/** Builds and wires (but doesn't persist) a new entry for [entryId]. */
	private fun createEntry(entryId: String): T = factory(entryId).also { entry ->
		entry.configFolder = resolvedDirectory()
		entry.filenamePrefixed = false
		if (synchronized)
		{
			entry.partOfCollection = true
			entry.collectionSync = { channel.toServer(EntrySync(entry.id, encode(entry))) }
		}
		entry.init()
	}

	private var scratchCounter = 0

	/**
	 * Builds a wired-but-unsaved entry under an auto-generated id, for the Cloth Config "+"
	 * button - it's only actually persisted once [reconcile] sees it survive to save time.
	 */
	internal fun createScratchEntry(): T
	{
		var candidate: String
		do candidate = "new_${scratchCounter++}" while (candidate in backing)
		return createEntry(candidate)
	}

	/**
	 * Scans [resolvedDirectory] for existing files - each becomes one entry, keyed by its
	 * filename without extension - and loads them. Entries already present (e.g. added at
	 * runtime this session via [add]) are left untouched.
	 */
	internal fun init()
	{
		val dir = resolvedDirectory()
		if (!Files.isDirectory(dir)) return
		Files.list(dir).use { stream ->
			stream
				.filter { Files.isRegularFile(it) && !it.fileName.toString().endsWith(".corrupted") }
				.forEach { path ->
					val entryId = path.fileName.toString().substringBeforeLast('.')
					if (entryId !in backing)
						backing[entryId] = createEntry(entryId).also { it.load() }
				}
		}
	}

	/**
	 * Registers this collection's sync channel, wiring both directions of [EntrySync]/[EntryRemoved]
	 * plus the join/quit listeners - a no-op if [synchronized] is `false` or this was already
	 * called. Must run at common-init time, on *both* physical sides (a non-hosting client never
	 * calls [ConfigSpec.load], but still needs its receiver registered to be pushed entries).
	 */
	internal fun registerNetwork(mod: Mod, channelId: String)
	{
		if (!synchronized || networkRegistered) return
		networkRegistered = true
		channel = NetworkChannel(mod % channelId)
		channel.serverbound<EntrySync> { packet, ctx ->
			val player = ctx.player as ServerPlayer
			val entry = backing[packet.entryId] ?: return@serverbound
			if (player.hasPermissions(3))
			{
				SerializationManager.cbor.decodeFromByteArray(entry.serializer, packet.data)
				entry.save()
				channel.toAllPlayers(packet)
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
				channel.toPlayer(player, EntrySync(packet.entryId, encode(entry)))
			}
		}
		channel.clientbound<EntrySync> { packet, _ ->
			val entry = backing.getOrPut(packet.entryId) { createEntry(packet.entryId) }
			SerializationManager.cbor.decodeFromByteArray(entry.serializer, packet.data)
			entry.isLoaded = true
			entry.save()
		}
		channel.clientbound<EntryRemoved> { packet, _ ->
			backing.remove(packet.entryId)?.deleteFile()
		}
		channel.register()
		foldEnv(
			client = {
				ClientPlayerEvent.CLIENT_PLAYER_QUIT.register { backing.clear() }
			},
			server = {
				PlayerEvent.PLAYER_JOIN.register { player ->
					if (!player.server.isSingleplayer)
						backing.values.forEach { channel.toPlayer(player, EntrySync(it.id, encode(it))) }
				}
			}
		)
	}

	/**
	 * For a top-level [synchronized] collection: defers [init] to [LifecycleEvent.SERVER_BEFORE_START],
	 * repointing [baseFolder] at the per-world `serverconfig/` folder first - the same timing
	 * [ConfigSpec.Server] itself uses, and for the same reason (the per-world folder isn't known
	 * any earlier). A non-hosting client never fires this event, so its [backing] starts empty and
	 * is populated purely by the join sync pushed from [registerNetwork].
	 */
	internal fun deferScanToServerStart(mod: Mod, segments: List<String>)
	{
		if (!synchronized || serverStartDeferred) return
		serverStartDeferred = true
		LifecycleEvent.SERVER_BEFORE_START.register { server ->
			backing.clear()
			baseFolder = { (listOf(mod.modId) + segments).fold(server.getWorldPath(CONFIG_DIR)) { acc, seg -> acc.resolve(seg) } }
			init()
		}
	}

	/**
	 * Reconciles this collection against [current] (the Cloth Config field's final value on
	 * save): any tracked entry no longer present (by identity) is deleted, and any present entry
	 * not yet tracked (a row inserted via [createScratchEntry] this session) is saved and
	 * registered. Entries already tracked are left alone - their own fields save independently,
	 * via their own drill-down screen. For a [synchronized] collection, only ever call this
	 * server-side - see the class doc.
	 */
	internal fun reconcile(current: Collection<T>)
	{
		val stillPresent = current.map { System.identityHashCode(it) }.toSet()
		backing.entries.filter { System.identityHashCode(it.value) !in stillPresent }
			.map { it.key }
			.forEach { remove(it) }
		current.forEach { entry ->
			if (backing.values.none { it === entry })
			{
				entry.save()
				backing[entry.id] = entry
				if (synchronized) channel.toAllPlayers(EntrySync(entry.id, encode(entry)))
			}
		}
	}

	/**
	 * Creates, saves, and registers a new entry for [entryId]. For a [synchronized] collection,
	 * only ever call this server-side - see the class doc.
	 */
	fun add(entryId: String): T
	{
		require(entryId !in backing) { "'$id' already has an entry named '$entryId'" }
		val entry = createEntry(entryId)
		entry.save()
		backing[entryId] = entry
		if (synchronized) channel.toAllPlayers(EntrySync(entryId, encode(entry)))
		return entry
	}

	/**
	 * Deletes [entryId]'s file and drops it from this collection. No-op if absent. For a
	 * [synchronized] collection, only ever call this server-side - see the class doc.
	 */
	fun remove(entryId: String)
	{
		backing.remove(entryId)?.deleteFile() ?: return
		if (synchronized) channel.toAllPlayers(EntryRemoved(entryId))
	}
}

/** A [ConfigSpecCollection] exposed as an ordered [entries] list. */
class ConfigSpecList<T : ConfigSpec>(
	title: Component,
	id: String = title.string.toSnakeCase(),
	directory: String = id,
	synchronized: Boolean = false,
	factory: (entryId: String) -> T,
) : ConfigSpecCollection<T>(title, id, directory, synchronized, factory)
{
	/** The current entries, in discovery/insertion order. */
	val entries: List<T> get() = backing.values.toList()
}

/** A [ConfigSpecCollection] exposed as a [entries] map keyed by each entry's id (its filename). */
class ConfigSpecMap<T : ConfigSpec>(
	title: Component,
	id: String = title.string.toSnakeCase(),
	directory: String = id,
	synchronized: Boolean = false,
	factory: (entryId: String) -> T,
) : ConfigSpecCollection<T>(title, id, directory, synchronized, factory)
{
	/** The current entries, keyed by id. */
	val entries: Map<String, T> get() = backing.toMap()
}
