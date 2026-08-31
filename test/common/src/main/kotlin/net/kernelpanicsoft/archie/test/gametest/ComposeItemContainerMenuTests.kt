package net.kernelpanicsoft.archie.test.gametest

import earth.terrarium.common_storage_lib.storage.base.UpdateManager
import net.kernelpanicsoft.archie.gui.item.PlayerInventoryItemAccess
import net.kernelpanicsoft.archie.gametest.internal.EMPTY
import net.kernelpanicsoft.archie.test.TestItemMenu
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.GameType

/**
 * GameTest coverage for [net.kernelpanicsoft.archie.gui.item.ComposeItemContainerMenu], via the
 * fixture [TestItemMenu] (declared directly against a real
 * [net.minecraft.gametest.framework.GameTestHelper.makeMockPlayer] player - no
 * network round trip, matching [net.kernelpanicsoft.archie.test.gametest.CapabilityLookupTests]'s
 * "construct against real infrastructure directly" style).
 *
 * Every test that constructs a [TestItemMenu] here must explicitly call `menu.removed(player)`
 * once done (unless the test is specifically about the auto-close path) - `onMenuOpened()`
 * unconditionally registers the menu with the real, process-wide
 * [net.kernelpanicsoft.archie.gui.item.ItemStateManager], whose real tick listener is genuinely
 * running for the whole GameTest server process, not just this test method. A menu left
 * registered keeps getting ticked (and its `itemAccess.stillValid(player)` re-checked) by every
 * *later* test in this run, not just this one - confirmed the hard way: an earlier draft of this
 * suite left menus registered, and a later test's `itemAccess.stillValid` flip cascaded into an
 * unrelated packet-registration crash days after this test method had already returned.
 *
 * Not covered here - manual-only, matching the Phase 3 plan's own carve-out for full on-screen
 * GUI open/close/shift-click flow (real Compose rendering + real player interaction isn't
 * GameTest-covered anywhere else in this codebase either):
 * - The excluded player-inventory slot's `mayPlace`/`mayPickup`/`quickMoveStack` behavior - see
 *   [ComposeItemContainerMenuClientTests] instead, which covers exactly this via a real menu-open
 *   round trip (this class can't: it needs
 *   [net.kernelpanicsoft.archie.gui.ComposeContainerMenuBase.updateSlotData] to have run, which
 *   sends an outbound `SlotData` packet unconditionally, and packet registration itself is
 *   deliberately skipped for a server-only GameTest run - `Archie.kt`'s own
 *   `ArchieNetworkChannel.init()` gating, unrelated to this feature).
 */
@Suppress("unused")
class ComposeItemContainerMenuTests
{
	@GameTest(template = EMPTY)
	fun GameTestHelper.testSlotContentPersistsThroughHolder()
	{
		val player = makeMockPlayer(GameType.CREATIVE)
		player.inventory.setItem(0, ItemStack(Items.PAPER))
		val menu = TestItemMenu(1, player.inventory, PlayerInventoryItemAccess(player, 0, Items.PAPER))

		menu.items[0].set(ItemStack(Items.DIAMOND, 5))
		// ArchieItemSlot.set() only mutates in-memory state - persistence is triggered externally,
		// the same way ArchieItemMenuSlot.setChanged() (a real vanilla Slot's hook) does it.
		UpdateManager.batch(menu.items)

		// A second, independent menu constructed against the same real backing stack re-loads
		// fresh from its persisted NBT (ItemStackNBTHolderImpl.init reloads every construction) -
		// this is a genuine round trip through the public API, not just reading `menu`'s own
		// still-live in-memory ArchieItemStorage back.
		val reloaded = TestItemMenu(11, player.inventory, PlayerInventoryItemAccess(player, 0, Items.PAPER))
		val item = reloaded.items[0].getItem()
		menu.removed(player)
		reloaded.removed(player)
		if (item.item != Items.DIAMOND || item.count != 5)
			fail("Expected the item written through menu.items to persist onto the real backing stack, got $item")
		succeed()
	}

	@GameTest(template = EMPTY)
	fun GameTestHelper.testStillValidReflectsItemAccess()
	{
		val player = makeMockPlayer(GameType.CREATIVE)
		player.inventory.setItem(0, ItemStack(Items.PAPER))
		val menu = TestItemMenu(4, player.inventory, PlayerInventoryItemAccess(player, 0, Items.PAPER))

		val initiallyValid = menu.stillValid(player)
		player.inventory.setItem(0, ItemStack(Items.STONE))
		val validAfterCorruption = menu.stillValid(player)
		menu.removed(player)

		if (!initiallyValid) fail("Expected stillValid to be true while the backing stack still matches")
		if (validAfterCorruption) fail("Expected stillValid to be false once the backing stack no longer matches")
		succeed()
	}

	@GameTest(template = EMPTY)
	fun GameTestHelper.testSyncedFieldRegistersSerializer()
	{
		val player = makeMockPlayer(GameType.CREATIVE)
		player.inventory.setItem(0, ItemStack(Items.PAPER))
		val menu = TestItemMenu(5, player.inventory, PlayerInventoryItemAccess(player, 0, Items.PAPER))

		val registered = menu.itemState.propertySerializers.containsKey("counter")
		menu.removed(player)

		if (!registered)
			fail("Expected the @Sync-annotated `counter` field to have registered a serializer in itemState at declaration time")
		succeed()
	}

	@GameTest(template = EMPTY)
	fun GameTestHelper.testItemStateManagerForceClosesInvalidMenu()
	{
		val player = makeMockPlayer(GameType.CREATIVE)
		player.inventory.setItem(0, ItemStack(Items.PAPER))
		val menu = TestItemMenu(6, player.inventory, PlayerInventoryItemAccess(player, 0, Items.PAPER))
		player.containerMenu = menu

		// Corrupt the backing stack so itemAccess.stillValid(player) flips false. No explicit
		// removed() call here - the whole point of this test is that ItemStateManager's real tick
		// loop closes (and so unregisters) it on its own.
		player.inventory.setItem(0, ItemStack(Items.STONE))

		succeedWhen {
			if (player.containerMenu === menu) fail("Expected ItemStateManager's tick loop to force-close the menu via player.closeContainer() once itemAccess reports it invalid")
		}
	}
}
