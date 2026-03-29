package net.kernelpanicsoft.archie.gui.blockentity

import kotlinx.serialization.Serializable
import net.kernelpanicsoft.archie.serialization.serializers.SBlockPos
import net.minecraft.core.BlockPos

/**
 * A network packet that carries block entity state changes from server to client.
 *
 * This packet is used to synchronize block entity property changes with connected clients,
 * enabling reactive UI updates in Compose-based screens.
 *
 * @property pos The block position of the block entity being updated.
 * @property updates A map of property names to their serialized values.
 * @property timestamp Server tick when this packet was created (for ordering/deduplication).
 */
@Serializable
data class BlockEntityStatePacket(
    val pos: SBlockPos,
    val updates: Map<String, SerializedValue> = emptyMap(),
    val timestamp: Long = 0,
) {
    /**
     * A serialized property value that can be transmitted over the network.
     *
     * Supports common types (Int, String, Boolean, Float, Double, etc.) as well as
     * complex types that need NBT serialization.
     */
    @Serializable
    sealed class SerializedValue {
        @Serializable
        data class IntValue(val value: Int) : SerializedValue()

        @Serializable
        data class StringValue(val value: String) : SerializedValue()

        @Serializable
        data class BooleanValue(val value: Boolean) : SerializedValue()

        @Serializable
        data class FloatValue(val value: Float) : SerializedValue()

        @Serializable
        data class DoubleValue(val value: Double) : SerializedValue()

        @Serializable
        data class LongValue(val value: Long) : SerializedValue()

        @Serializable
        data class ByteValue(val value: Byte) : SerializedValue()

        @Serializable
        data class ListValue(val values: List<SerializedValue>) : SerializedValue()

        @Serializable
        data class MapValue(val values: Map<String, SerializedValue>) : SerializedValue()

        @Serializable
        object NullValue : SerializedValue()
    }

    companion object {
        /**
         * Creates a new packet with a single property update.
         *
         * @param pos The block position.
         * @param propertyName The name of the property being updated.
         * @param value The new value.
         * @param timestamp The server tick.
         */
        fun singleUpdate(
            pos: BlockPos,
            propertyName: String,
            value: SerializedValue,
            timestamp: Long = 0,
        ): BlockEntityStatePacket = BlockEntityStatePacket(
            pos = pos,
            updates = mapOf(propertyName to value),
            timestamp = timestamp,
        )
    }
}

/**
 * Extension function to convert common Kotlin types to [BlockEntityStatePacket.SerializedValue].
 */
fun Any?.toSerializedValue(): BlockEntityStatePacket.SerializedValue = when (this) {
    null -> BlockEntityStatePacket.SerializedValue.NullValue
    is Int -> BlockEntityStatePacket.SerializedValue.IntValue(this)
    is String -> BlockEntityStatePacket.SerializedValue.StringValue(this)
    is Boolean -> BlockEntityStatePacket.SerializedValue.BooleanValue(this)
    is Float -> BlockEntityStatePacket.SerializedValue.FloatValue(this)
    is Double -> BlockEntityStatePacket.SerializedValue.DoubleValue(this)
    is Long -> BlockEntityStatePacket.SerializedValue.LongValue(this)
    is Byte -> BlockEntityStatePacket.SerializedValue.ByteValue(this)
    is List<*> -> BlockEntityStatePacket.SerializedValue.ListValue(this.map { it.toSerializedValue() })
    is Map<*, *> -> BlockEntityStatePacket.SerializedValue.MapValue(
        this.mapKeys { it.key.toString() }.mapValues { it.value.toSerializedValue() }
    )
    else -> BlockEntityStatePacket.SerializedValue.NullValue
}
