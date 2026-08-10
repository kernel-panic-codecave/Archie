package net.kernelpanicsoft.archie.gui.item

import kotlinx.serialization.Serializable
import net.kernelpanicsoft.archie.gui.blockentity.BlockEntityStatePacket

/**
 * A network packet that carries [ComposeItemContainerMenu] state changes from server to client -
 * the item-backed-menu equivalent of [BlockEntityStatePacket]. [containerId] addresses the
 * player's currently open menu directly rather than acting as a lookup key: an item-backed menu
 * is inherently 1:1 with one player's session, so there's no position-keyed registry to look
 * anything up in, unlike a block entity that can be watched by multiple players at once. It's
 * checked purely as a staleness guard against a stray packet arriving after this player closed
 * one item menu and opened another.
 *
 * @property containerId The owning menu's vanilla [net.minecraft.world.inventory.AbstractContainerMenu.containerId].
 * @property updates A map of property names to their serialized values.
 * @property timestamp Server tick when this packet was created (for ordering/deduplication).
 */
@Serializable
data class ItemStatePacket(
	val containerId: Int,
	val updates: Map<String, BlockEntityStatePacket.SerializedValue> = emptyMap(),
	val timestamp: Long = 0,
) {
	companion object {
		/** Creates a new packet with a single property update. */
		fun singleUpdate(
			containerId: Int,
			propertyName: String,
			value: BlockEntityStatePacket.SerializedValue,
			timestamp: Long = 0,
		): ItemStatePacket = ItemStatePacket(
			containerId = containerId,
			updates = mapOf(propertyName to value),
			timestamp = timestamp,
		)
	}
}
