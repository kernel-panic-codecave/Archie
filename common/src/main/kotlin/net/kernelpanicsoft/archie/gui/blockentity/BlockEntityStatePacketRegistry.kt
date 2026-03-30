package net.kernelpanicsoft.archie.gui.blockentity

import kotlinx.serialization.KSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import net.kernelpanicsoft.archie.block.entity.NBTBlockEntity
import net.kernelpanicsoft.archie.networking.ArchieNetworkChannel
import net.kernelpanicsoft.archie.serialization.SerializationManager
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
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
fun getOrCreateBlockEntityState(pos: BlockPos): ComposeBlockEntityState {
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
fun removeBlockEntityState(pos: BlockPos) {
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

        ArchieNetworkChannel.serverbound(BlockEntityUpdatePacket::class) { packet, context ->
            val player = context.player
            val level = player.level() as? ServerLevel ?: return@serverbound
            val blockEntity = level.getBlockEntity(packet.pos) as? NBTBlockEntity ?: return@serverbound
            val container = BlockEntityStateManager.getContainer(blockEntity) ?: return@serverbound

            packet.updates.forEach { (propertyName, serializedValue) ->
                val serializer = container.propertySerializers[propertyName]
                if (serializer != null) {
                    val deserializedValue = serializedValue.deserialize(serializer)
                    container.updateProperty(propertyName, deserializedValue)
                    blockEntity.updateProperty(propertyName, serializer as KSerializer<Any>, deserializedValue as Any)
                }
            }
        }
    }
}

/**
 * Extension function to convert SerializedValue back to its original Kotlin type.
 */
@OptIn(ExperimentalSerializationApi::class)
internal fun BlockEntityStatePacket.SerializedValue.deserialize(serializer: KSerializer<out Any>? = null): Any? = when (this) {
    is BlockEntityStatePacket.SerializedValue.IntValue -> this.value
    is BlockEntityStatePacket.SerializedValue.StringValue -> this.value
    is BlockEntityStatePacket.SerializedValue.BooleanValue -> this.value
    is BlockEntityStatePacket.SerializedValue.FloatValue -> this.value
    is BlockEntityStatePacket.SerializedValue.DoubleValue -> this.value
    is BlockEntityStatePacket.SerializedValue.LongValue -> this.value
    is BlockEntityStatePacket.SerializedValue.ByteValue -> this.value
    is BlockEntityStatePacket.SerializedValue.NullValue -> null
    is BlockEntityStatePacket.SerializedValue.CBORValue -> {
        if (serializer != null) {
            SerializationManager.cbor.decodeFromByteArray(serializer, this.value)
        } else {
            // If no serializer is provided, we can't deserialize CBOR, so return the raw bytes or null
            this.value
        }
    }
}

/**
 * Extension function to convert common Kotlin types to [BlockEntityStatePacket.SerializedValue].
 */
@Suppress("UNCHECKED_CAST")
@OptIn(ExperimentalSerializationApi::class)
fun <T> T?.toSerializedValue(serializer: KSerializer<T>): BlockEntityStatePacket.SerializedValue = when (this) {
    null -> BlockEntityStatePacket.SerializedValue.NullValue
    is Int -> BlockEntityStatePacket.SerializedValue.IntValue(this)
    is String -> BlockEntityStatePacket.SerializedValue.StringValue(this)
    is Boolean -> BlockEntityStatePacket.SerializedValue.BooleanValue(this)
    is Float -> BlockEntityStatePacket.SerializedValue.FloatValue(this)
    is Double -> BlockEntityStatePacket.SerializedValue.DoubleValue(this)
    is Long -> BlockEntityStatePacket.SerializedValue.LongValue(this)
    is Byte -> BlockEntityStatePacket.SerializedValue.ByteValue(this)
    is ByteArray -> BlockEntityStatePacket.SerializedValue.CBORValue(this)
    else -> BlockEntityStatePacket.SerializedValue.CBORValue(SerializationManager.cbor.encodeToByteArray(serializer, this))
}
