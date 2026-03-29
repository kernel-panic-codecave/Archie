package net.kernelpanicsoft.archie.gui

import earth.terrarium.common_storage_lib.item.impl.vanilla.AbstractVanillaContainer
import earth.terrarium.common_storage_lib.resources.item.ItemResource
import earth.terrarium.common_storage_lib.storage.base.CommonStorage
import net.kernelpanicsoft.archie.Archie
import net.kernelpanicsoft.archie.networking.ArchieNetworkChannel
import net.kernelpanicsoft.archie.networking.NetworkChannel
import net.kernelpanicsoft.archie.transfer.ArchieItemMenuSlot
import net.kernelpanicsoft.archie.transfer.ArchieItemStorage
import net.kernelpanicsoft.archie.transfer.VanillaMenuSlot
import net.kernelpanicsoft.archie.util.rem
import net.minecraft.world.Container
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ClickType
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity

/**
 * Base class for Compose-backed container menus.
 *
 * Slots are pre-registered at construction time with placeholder pixel positions so that
 * [AbstractContainerMenu.initializeContents] (triggered by the server's slot-sync packet)
 * always finds the correct number of slots. When the Compose layout runs and reports actual
 * on-screen positions via [updateSlotData], existing slot objects have their pixel coordinates
 * updated in-place rather than the slot list being rebuilt from scratch.
 *
 * ### Subclassing
 * ```kotlin
 * class MyMenu(id: Int, inventory: Inventory, tile: MyTile) :
 *     ComposeContainerMenu<MyTile, MyMenu>(MY_MENU_TYPE, id, inventory, tile) {
 *
 *     override fun registerSlotHandlers() {
 *         handler("inventory", tile.items)   // ties the "inventory" slot group to the storage
 *     }
 * }
 * ```
 *
 * @param T    The [BlockEntity] type that owns the storage.
 * @param SELF The concrete menu subclass (self-referential for the [MenuType]).
 * @param type The registered [MenuType] for this menu.
 * @param id   The container id assigned by the server.
 * @param playerInventory The opening player's inventory.
 * @param tile The block entity instance.
 */
abstract class ComposeContainerMenu<T : BlockEntity, SELF : ComposeContainerMenu<T, SELF>>(
    type: MenuType<SELF>,
    id: Int,
    protected val playerInventory: Inventory,
    protected val tile: T,
) : AbstractContainerMenu(type, id) {

    /**
     * The most recently reported [SlotData] from the Compose layout.
     * On the server this is the authoritative source of slot group sizes.
     * On the client it is received from the server via [CHANNEL].
     */
    var slotData: SlotData = SlotData()
        private set

    /**
     * The screen's `leftPos` offset — set by [ComposeContainerScreen] so that absolute
     * Compose coordinates can be converted to slot-relative coordinates that vanilla's
     * item rendering expects (vanilla renders items at `leftPos + slot.x`).
     */
    var screenLeftPos: Int = 0

    /**
     * The screen's `topPos` offset — set by [ComposeContainerScreen] so that absolute
     * Compose coordinates can be converted to slot-relative coordinates that vanilla's
     * item rendering expects (vanilla renders items at `topPos + slot.y`).
     */
    var screenTopPos: Int = 0

    /** Maps slot-group id → the storage that backs it. */
    private val slotHandlers: MutableMap<String, CommonStorage<ItemResource>> = mutableMapOf()

    protected val player: Player  = playerInventory.player
    protected val level: Level    = player.level()

    /** The index range occupied by block-entity slots in [slots]. */
    protected open val menuSlots: IntRange   get() = 0 until slotData.slots.size

    /** The index range occupied by player-inventory slots in [slots]. */
    protected open val playerSlots: IntRange get() = slotData.slots.size until slots.size

    // ── Slot registration ──────────────────────────────────────────────────

    /**
     * Implement this to call [handler] for each slot group your menu exposes.
     *
     * This is called:
     * 1. Once at construction time so the slots list is pre-populated with the correct count.
     * 2. Again every time [updateSlotData] fires with updated positions.
     */
    protected abstract fun registerSlotHandlers()

    /**
     * Registers a storage handler for the named slot group.
     *
     * @param group   The id matching a [SlotGroup] reported by the Compose layout.
     * @param storage The [ArchieItemStorage] that backs this group.
     */
    protected fun handler(group: String, storage: ArchieItemStorage) {
        slotHandlers[group] = storage
    }

    /**
     * Registers a generic [CommonStorage] handler for the named slot group.
     */
    protected fun handler(group: String, storage: CommonStorage<ItemResource>) {
        slotHandlers[group] = storage
    }

    // ── Called by the Compose layout ───────────────────────────────────────

    /**
     * Called by the [Slot] composable once its absolute screen position is known.
     *
     * On the **first** call (before any slots exist) the full slot list is built.
     * On **subsequent** calls (re-layout / window resize) existing slot objects have their
     * pixel coordinates updated in-place so [initializeContents] is never broken.
     *
     * @param data The updated [SlotData] from the Compose layout.
     */
    fun updateSlotData(data: SlotData) {
        slotData = data
        val isFirstTime = slots.isEmpty()
        if (isFirstTime) {
            buildSlots()
        } else {
            repositionSlots()
        }
        broadcastChanges()
        // Notify server of the new layout so it can validate slot indices
        ArchieNetworkChannel.toServer(data)
    }

    /**
     * First-time slot construction: registers all menu slots and player slots.
     * Must only be called when [slots] is empty.
     */
    private fun buildSlots() {
        registerSlotHandlers()
        addMenuSlots()
        addPlayerSlots()
    }

    /**
     * Subsequent calls: update pixel coordinates of already-registered slots in-place.
     * Converts absolute Compose screen coordinates to slot-relative coordinates by
     * subtracting [screenLeftPos]/[screenTopPos], because vanilla renders items at
     * `leftPos + slot.x` and `topPos + slot.y`.
     * Slot *count* must not change between layouts.
     */
    private fun repositionSlots() {
        var slotIndex = 0

        // Reposition menu slots
        slotData.groups.forEach { (id, group) ->

            slotHandlers[id]?.let { _ ->
                for (row in 0 until group.size.height) {
                    for (col in 0 until group.size.width) {
                        if (slotIndex < slots.size) {
                            val mcSlot = slots[slotIndex]
                            mcSlot.x = group.pos.x + 1 + col * 18 - screenLeftPos
                            mcSlot.y = group.pos.y + 1 + row * 18 - screenTopPos
                            slotIndex++
                        }
                    }
                }
            }
        }

        // Reposition player slots (main inventory 3×9, then hotbar 1×9)
        slotData.playerGroup.let { pg ->
            for (row in 0 until 3) {
                for (col in 0 until 9) {
                    if (slotIndex < slots.size) {
                        slots[slotIndex].x = pg.pos.x + 1 + col * 18 - screenLeftPos
                        slots[slotIndex].y = pg.pos.y + 1 + row * 18 - screenTopPos
                        slotIndex++
                    }
                }
            }
            for (col in 0 until 9) {
                if (slotIndex < slots.size) {
                    slots[slotIndex].x = pg.pos.x + 1 + col * 18 - screenLeftPos
                    slots[slotIndex].y = pg.pos.y + 1 + 58 - screenTopPos
                    slotIndex++
                }
            }
        }
    }

    // ── Internal slot helpers ──────────────────────────────────────────────

    private fun addMenuSlots() {
        slotData.groups.forEach { (id, group) ->
            slotHandlers[id]?.let { handler ->
                // Use slot-relative coords (subtract screen offset so vanilla adds it back correctly)
//	            group.slots.forEachIndexed { slot, coords ->
//		            slot(handler, slot, coords.x - screenLeftPos, coords.y - screenTopPos)
//	            }
                slotGrid(group.pos.x - screenLeftPos, group.pos.y - screenTopPos, group.size.width, group.size.height, handler)
            }
        }
    }

    private fun addPlayerSlots() {
        slotData.playerGroup.let { pg ->
            val relX = pg.pos.x - screenLeftPos
            val relY = pg.pos.y - screenTopPos
            // 3 rows of 9 (main inventory: playerInventory indices 9–35)
            for (row in 0 until 3) {
                for (col in 0 until 9) {
                    slot(playerInventory, col + row * 9 + 9, relX + col * 18, relY + row * 18)
                }
            }
            // Hotbar (playerInventory indices 0–8), 58px below main inventory
            for (col in 0 until 9) {
                slot(playerInventory, col, relX + col * 18, relY + 58)
            }
        }
    }

    // ── Slot grid helper ───────────────────────────────────────────────────

    data class SlotGridLocation(val slot: Int, val x: Int, val y: Int)

    protected fun slotGrid(x: Int, y: Int, width: Int, height: Int, block: SlotGridLocation.() -> Unit) {
        for (row in 0 until height) {
            for (col in 0 until width) {
                SlotGridLocation(col + row * width, x + col * 18, y + row * 18).block()
            }
        }
    }

    protected fun slotGrid(x: Int, y: Int, width: Int, height: Int, container: Container) {
        slotGrid(x, y, width, height) { slot(container, slot, this.x, this.y) }
    }

    protected fun slotGrid(x: Int, y: Int, width: Int, height: Int, storage: CommonStorage<ItemResource>) {
        slotGrid(x, y, width, height) { slot(storage, slot, this.x, this.y) }
    }

    protected fun slot(mcSlot: Slot) { addSlot(mcSlot) }

    protected fun slot(storage: CommonStorage<ItemResource>, slot: Int, x: Int, y: Int) {
        when {
            storage is ArchieItemStorage       -> slot(storage, slot, x, y)
            storage is AbstractVanillaContainer -> slot(storage as Container, slot, x, y)
        }
    }

    protected fun slot(container: Container, slot: Int, x: Int, y: Int) {
        addSlot(Slot(container, slot, x, y))
    }

    protected fun slot(storage: ArchieItemStorage, slot: Int, x: Int, y: Int) {
        addSlot(ArchieItemMenuSlot(storage, slot, x, y))
    }

    protected fun slot(storage: AbstractVanillaContainer, slot: Int, x: Int, y: Int) {
        addSlot(VanillaMenuSlot(storage, slot, x, y))
    }

    // ── AbstractContainerMenu overrides ────────────────────────────────────

    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        var result = ItemStack.EMPTY
        val slot = slots.getOrNull(index) ?: return result
        if (slot.hasItem()) {
            val slotStack = slot.item
            result = slotStack.copy()
            if (index in menuSlots) {
                if (!moveItemStackTo(slotStack, playerSlots.first, playerSlots.last, true))
                    return ItemStack.EMPTY
            } else if (index in playerSlots) {
                if (!moveItemStackTo(slotStack, menuSlots.first, menuSlots.last, false))
                    return ItemStack.EMPTY
            }
            if (slotStack.isEmpty) slot.set(ItemStack.EMPTY) else slot.setChanged()
        }
        return result
    }

    override fun clicked(slotId: Int, button: Int, clickType: ClickType, player: Player) {
        super.clicked(slotId, button, clickType, player)
        broadcastChanges()
    }

    override fun stillValid(player: Player): Boolean = true

    // ── Networking ─────────────────────────────────────────────────────────

    companion object {

        fun register() {
            ArchieNetworkChannel.serverbound(SlotData::class) { data, context ->
                val menu = context.player.containerMenu
                if (menu is ComposeContainerMenu<*, *>) {
                    menu.slotData = data
                    // Rebuild slot positions on the server to match the client layout
                    if (menu.slots.isEmpty()) {
                        menu.buildSlots()
                    } else {
                        menu.repositionSlots()
                    }
                    menu.broadcastChanges()
                }
            }
        }
    }
}
