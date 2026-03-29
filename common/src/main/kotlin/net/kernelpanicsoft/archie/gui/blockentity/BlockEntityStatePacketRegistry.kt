package net.kernelpanicsoft.archie.gui.blockentity

import net.kernelpanicsoft.archie.networking.ArchieNetworkChannel
import java.util.concurrent.ConcurrentHashMap

/**
 * Client-side registry of block entity states.
 *
 * Stores Compose state objects for active block entities so they can be updated
 * when network packets arrive.
 */
private val clientBlockEntityStates = ConcurrentHashMap<String, ComposeBlockEntityState>()

/**
 * Gets or creates a Compose state for a block entity by position.
 *
 * @param pos The block position.
 * @return The [ComposeBlockEntityState] for that position.
 */
fun getOrCreateBlockEntityState(pos: net.minecraft.core.BlockPos): ComposeBlockEntityState {
    val key = "${pos.x}_${pos.y}_${pos.z}"
    return clientBlockEntityStates.computeIfAbsent(key) {
        ComposeBlockEntityState(pos)
    }
}

/**
 * Removes a block entity state from the client registry.
 *
 * @param pos The block position.
 */
fun removeBlockEntityState(pos: net.minecraft.core.BlockPos) {
    val key = "${pos.x}_${pos.y}_${pos.z}"
    clientBlockEntityStates.remove(key)
}

/**
 * Registers the [BlockEntityStatePacket] with the Archie network channel.
 *
 * This allows the packet to be sent from server to client for block entity state updates.
 */
object BlockEntityStatePacketRegistry {
    fun register() {
        ArchieNetworkChannel.clientbound(BlockEntityStatePacket::class) { packet, context ->
            // Update the client-side state with new values from the packet
            val state = getOrCreateBlockEntityState(packet.pos)
            packet.updates.forEach { (propertyName, value) ->
                state.updateProperty(propertyName, value)
            }
        }
    }
}

