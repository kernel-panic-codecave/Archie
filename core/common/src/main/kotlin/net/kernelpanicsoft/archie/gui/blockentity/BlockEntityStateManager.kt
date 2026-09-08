package net.kernelpanicsoft.archie.gui.blockentity

import dev.architectury.event.events.common.TickEvent
import net.kernelpanicsoft.archie.networking.ArchieNetworkChannel
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.block.entity.BlockEntity
import java.util.concurrent.ConcurrentHashMap

/**
 * Server-side manager for tracking and syncing block entity state to clients.
 *
 * This manager:
 * - Maintains a registry of block entities by position
 * - Tracks which properties have changed on each block entity
 * - Sends state packets to players who are observing each block entity
 * - Cleans up state when block entities are unloaded
 */
object BlockEntityStateManager {
    /** Registry of tracked block entities keyed by (level, pos) */
    private val trackedEntities = ConcurrentHashMap<String, BlockEntityStateContainer>()

    /** Registry of players tracking each block entity, keyed by (level, pos) */
    private val trackedPlayers = ConcurrentHashMap<String, MutableSet<ServerPlayer>>()

    /**
     * Registers the server tick listener that drives [syncDirtyEntities] every tick.
     *
     * Must be called once during mod init.
     */
    fun init() {
        TickEvent.SERVER_POST.register {
            syncDirtyEntities(it.tickCount.toLong())
        }

    }

    /**
     * Registers a block entity for state tracking.
     *
     * Should be called when a container menu is opened for a block entity.
     *
     * @param blockEntity The block entity to track.
     * @return The state container for this block entity.
     */
    fun registerBlockEntity(blockEntity: BlockEntity): BlockEntityStateContainer {
        val key = getKey(blockEntity)
        val container = trackedEntities.computeIfAbsent(key) {
            BlockEntityStateContainer(blockEntity)
        }
        return container
    }

    /**
     * Unregisters a block entity from state tracking.
     *
     * Should be called when a container menu is closed.
     *
     * @param blockEntity The block entity to untrack.
     */
    fun unregisterBlockEntity(blockEntity: BlockEntity) {
        val key = getKey(blockEntity)
        trackedEntities.remove(key)?.reset()
        trackedPlayers.remove(key)
    }

    /**
     * Adds a player to the tracking list for a block entity.
     *
     * The player will receive state packets when the block entity changes.
     *
     * @param blockEntity The block entity.
     * @param player The player to add.
     */
    fun addTrackedPlayer(blockEntity: BlockEntity, player: ServerPlayer) {
        val key = getKey(blockEntity)
        trackedPlayers.computeIfAbsent(key) { mutableSetOf() }.add(player)

        // A player who has just started tracking has missed every change so far, and the sync is a
        // delta stream - without this they would see nothing at all until the block entity next
        // changed, which for anything sitting idle is never. Seeding from the live block entity
        // covers state that predates the container entirely.
        val container = registerBlockEntity(blockEntity)
        container.captureCurrentValues()
        container.markAllDirty()
    }

    /** Whether anyone is still tracking [blockEntity] - see [unregisterBlockEntity]'s own caveat. */
    fun hasTrackedPlayers(blockEntity: BlockEntity): Boolean =
        trackedPlayers[getKey(blockEntity)]?.isNotEmpty() == true

    /**
     * Removes a player from the tracking list for a block entity.
     *
     * @param blockEntity The block entity.
     * @param player The player to remove.
     */
    fun removeTrackedPlayer(blockEntity: BlockEntity, player: ServerPlayer) {
        val key = getKey(blockEntity)
        trackedPlayers[key]?.remove(player)
        if (trackedPlayers[key]?.isEmpty() == true) {
            trackedPlayers.remove(key)
        }
    }

    /**
     * Gets the state container for a block entity, if it exists.
     *
     * @param blockEntity The block entity.
     * @return The state container, or null if not registered.
     */
    fun getContainer(blockEntity: BlockEntity): BlockEntityStateContainer? {
        return trackedEntities[getKey(blockEntity)]
    }

    /**
     * Syncs all dirty block entities to their tracked players.
     *
     * Should be called once per server tick via a tick event.
     *
     * @param currentTick The current server tick.
     * @param networkChannel The network channel to send packets through.
     */
    fun syncDirtyEntities(
        currentTick: Long,
        networkChannel: (BlockEntityStatePacket, List<ServerPlayer>) -> Unit = DEFAULT_NETWORK_SENDER,
    ) {
        trackedEntities.forEach { (key, container) ->
            val packet = container.generatePacket(currentTick) ?: return@forEach
            val players = trackedPlayers[key] ?: emptySet()
            if (players.isNotEmpty()) {
                networkChannel(packet, players.toList())
                container.clearDirty(currentTick)
            }
        }
    }

    /**
     * Clears all tracking data.
     *
     * Useful for cleanup on server shutdown.
     */
    fun clear() {
        trackedEntities.values.forEach { it.reset() }
        trackedEntities.clear()
        trackedPlayers.clear()
    }

    /**
     * Gets a unique key for a block entity based on level and position.
     *
     * @param blockEntity The block entity.
     * @return A unique key string.
     */
    private fun getKey(blockEntity: BlockEntity): String {
        val levelName = blockEntity.level?.hashCode() ?: 0
        return "${levelName}_${blockEntity.blockPos.x}_${blockEntity.blockPos.y}_${blockEntity.blockPos.z}"
    }

    /**
     * Default network sender used by [syncDirtyEntities]; sends [BlockEntityStatePacket]s to the
     * given players via [ArchieNetworkChannel].
     */
    private val DEFAULT_NETWORK_SENDER: (BlockEntityStatePacket, List<ServerPlayer>) -> Unit = { packet, players ->
        ArchieNetworkChannel.toPlayers(players, packet)
    }
}

/**
 * Convenience extension to get or create a state container for a block entity.
 */
fun BlockEntity.getStateContainer(): BlockEntityStateContainer =
    BlockEntityStateManager.getContainer(this) ?: BlockEntityStateManager.registerBlockEntity(this)
