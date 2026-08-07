package net.kernelpanicsoft.archie.test.gametest

import dev.architectury.registry.menu.MenuRegistry
import net.kernelpanicsoft.archie.gametest.*
import net.kernelpanicsoft.archie.gui.item.PlayerInventoryItemAccess
import net.kernelpanicsoft.archie.test.TestItemContainerScreen
import net.kernelpanicsoft.archie.test.TestItemMenu
import net.minecraft.network.chat.Component
import net.minecraft.world.SimpleMenuProvider
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

/**
 * End-to-end coverage for [TestItemMenu]'s excluded-slot behavior, opened through the *real*
 * server-to-client menu flow ([MenuRegistry.openExtendedMenu] + a real [TestItemContainerScreen]
 * layout pass) - unlike [ComposeItemContainerMenuTests], which constructs [TestItemMenu] directly
 * and so can't exercise anything depending on
 * [net.kernelpanicsoft.archie.gui.ComposeContainerMenuBase.updateSlotData] actually having run
 * (that method unconditionally sends an outbound `SlotData` packet, which throws from a
 * server-only GameTest since packet registration itself is skipped there - see that class's KDoc).
 * A client GameTest has both sides' networking registered for real, so this is where that gap is
 * actually covered.
 */
@Suppress("unused")
class ComposeItemContainerMenuClientTests
{
	@ClientGameTest
	fun ClientGameTestContext.testExcludedSlotBehaviorViaRealMenuOpen()
	{
		withWorld {
			withSingleplayer {
				waitForPlayer()

				server.runOnServer { minecraftServer ->
					val player = minecraftServer.playerList.players.first()
					player.inventory.setItem(0, ItemStack(Items.PAPER))
					MenuRegistry.openExtendedMenu(
						player,
						SimpleMenuProvider(
							{ id, inv, p -> TestItemMenu(id, inv, PlayerInventoryItemAccess(p, 0, Items.PAPER)) },
							Component.literal("Test Item Menu"),
						),
					) { buf -> buf.writeVarInt(0) }
				}

				clientContext.waitForScreen<TestItemContainerScreen>()
				clientContext.waitForComposeIdle()
				// One extra tick so the client's just-reported SlotData round-trips back through
				// the server (ComposeContainerMenuBase.updateSlotData -> serverbound SlotData ->
				// menu.applySlotData()) and the server's own slot list is actually built.
				clientContext.waitTicks(2)

				server.runOnServer { minecraftServer ->
					val player = minecraftServer.playerList.players.first()
					val menu = player.containerMenu as? TestItemMenu
						?: error("Expected the player's current menu to be a TestItemMenu, was ${player.containerMenu}")

					val excludedSlot = menu.slots.firstOrNull { it.container === player.inventory && it.containerSlot == 0 }
						?: error("Expected a player-inventory slot bound to container index 0 among ${menu.slots.size} slots")
					if (excludedSlot.mayPlace(ItemStack(Items.STONE))) fail("Expected the excluded slot to reject placement")
					if (excludedSlot.mayPickup(player)) fail("Expected the excluded slot to reject pickup")

					val excludedIndex = menu.slots.indexOf(excludedSlot)
					val result = menu.quickMoveStack(player, excludedIndex)
					if (!result.isEmpty) fail("Expected quickMoveStack on the excluded slot to return an empty stack, got $result")
				}
			}
		}
	}
}
