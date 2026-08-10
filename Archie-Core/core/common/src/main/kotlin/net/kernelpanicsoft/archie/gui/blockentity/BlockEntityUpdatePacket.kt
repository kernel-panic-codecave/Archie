package net.kernelpanicsoft.archie.gui.blockentity

import kotlinx.serialization.Serializable
import net.kernelpanicsoft.archie.serialization.serializers.SBlockPos
import net.minecraft.core.BlockPos

/**
 * A network packet that carries block entity state updates from client to server.
 *
 * This packet is used to send client-side modifications of block entity properties back to the server.
 *
 * @property pos The block position of the block entity being updated.
 * @property updates A map of property names to their serialized values.
 */
@Serializable
data class BlockEntityUpdatePacket(
    val pos: SBlockPos,
    val updates: Map<String, BlockEntityStatePacket.SerializedValue>,
) {
    companion object {
        /**
         * Creates a new packet with a single property update.
         *
         * @param pos The block position.
         * @param propertyName The name of the property being updated.
         * @param value The new value.
         */
        fun singleUpdate(
            pos: BlockPos,
            propertyName: String,
            value: BlockEntityStatePacket.SerializedValue,
        ): BlockEntityUpdatePacket = BlockEntityUpdatePacket(
            pos = pos,
            updates = mapOf(propertyName to value),
        )
    }
}
