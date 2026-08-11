package net.kernelpanicsoft.archie.gui.item

import kotlinx.serialization.KSerializer
import net.kernelpanicsoft.archie.gui.blockentity.deserialize
import net.kernelpanicsoft.archie.networking.ArchieNetworkChannel

/**
 * Registers [ItemStatePacket]/[ItemUpdatePacket] handlers with [ArchieNetworkChannel] - the
 * [net.kernelpanicsoft.archie.gui.blockentity.BlockEntityStatePacketRegistry] equivalent for
 * item-backed menus.
 *
 * Unlike the block-entity path, routing needs no position-keyed lookup on either side - an
 * item-backed menu is inherently 1:1 with one player's session, so "the current menu" is always
 * just `context.player.containerMenu` (true on both sides: [net.minecraft.world.entity.player.Player]
 * has exactly one open [net.minecraft.world.inventory.AbstractContainerMenu] at a time). The
 * `containerId` on each packet is checked purely as a staleness guard against a stray packet
 * arriving after the player closed one item menu and opened another - a mismatch is silently
 * dropped, not an error.
 */
object ItemStatePacketRegistry {
	/** Registers the clientbound and serverbound packet handlers described above. */
	fun register() {
		ArchieNetworkChannel.clientbound<ItemStatePacket> { packet, context ->
			val menu = context.player.containerMenu as? ComposeItemContainerMenu<*> ?: return@clientbound
			if (menu.containerId != packet.containerId) return@clientbound
			packet.updates.forEach { (propertyName, value) ->
				menu.itemState.updateProperty(propertyName, value)
			}
		}

		ArchieNetworkChannel.serverbound<ItemUpdatePacket> { packet, context ->
			val menu = context.player.containerMenu as? ComposeItemContainerMenu<*> ?: return@serverbound
			if (menu.containerId != packet.containerId) return@serverbound
			packet.updates.forEach { (propertyName, serializedValue) ->
				val serializer = menu.itemState.propertySerializers[propertyName] ?: return@forEach
				val deserializedValue = serializedValue.deserialize(serializer)
				@Suppress("UNCHECKED_CAST")
				menu.applyRemoteUpdate(propertyName, serializer as KSerializer<Any?>, deserializedValue)
			}
		}
	}
}
