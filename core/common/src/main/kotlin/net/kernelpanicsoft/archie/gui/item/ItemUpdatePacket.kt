package net.kernelpanicsoft.archie.gui.item

import kotlinx.serialization.Serializable
import net.kernelpanicsoft.archie.gui.blockentity.BlockEntityStatePacket

/**
 * A network packet that carries [ComposeItemContainerMenu] state updates from client to server -
 * the item-backed-menu equivalent of [net.kernelpanicsoft.archie.gui.blockentity.BlockEntityUpdatePacket].
 * See [ItemStatePacket] for what [containerId] is used for.
 *
 * @property containerId The owning menu's vanilla [net.minecraft.world.inventory.AbstractContainerMenu.containerId].
 * @property updates A map of property names to their serialized values.
 */
@Serializable
data class ItemUpdatePacket(
	val containerId: Int,
	val updates: Map<String, BlockEntityStatePacket.SerializedValue>,
) {
	companion object {
		/** Creates a new packet with a single property update. */
		fun singleUpdate(
			containerId: Int,
			propertyName: String,
			value: BlockEntityStatePacket.SerializedValue,
		): ItemUpdatePacket = ItemUpdatePacket(
			containerId = containerId,
			updates = mapOf(propertyName to value),
		)
	}
}
