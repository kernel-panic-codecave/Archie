package net.kernelpanicsoft.archie.gui

import earth.terrarium.common_storage_lib.item.impl.vanilla.AbstractVanillaContainer
import earth.terrarium.common_storage_lib.resources.item.ItemResource
import earth.terrarium.common_storage_lib.storage.base.CommonStorage
import net.kernelpanicsoft.archie.gui.blockentity.BlockEntityStateManager
import net.kernelpanicsoft.archie.gui.blockentity.ComposeBlockEntityState
import net.kernelpanicsoft.archie.gui.blockentity.getOrCreateBlockEntityState
import net.kernelpanicsoft.archie.gui.layout.IntRect
import net.kernelpanicsoft.archie.networking.ArchieNetworkChannel
import net.kernelpanicsoft.archie.transfer.ArchieItemMenuSlot
import net.kernelpanicsoft.archie.transfer.ArchieItemStorage
import net.kernelpanicsoft.archie.transfer.VanillaMenuSlot
import net.minecraft.server.level.ServerPlayer
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
import java.util.function.Predicate

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
     * On the client it is received from the server via a [net.kernelpanicsoft.archie.networking.NetworkChannel].
     */
    var slotData: SlotData = SlotData()
        private set

    /** `true` once [repositionSlots] has run at least once and slot pixel positions are valid. */
    var ready: Boolean = false
        private set

    /** Parallel to [slots], stores which compose layer produced each vanilla slot. */
    private var slotLayerDepthByIndex: IntArray = IntArray(0)
    private var slotClipBoundsByIndex: Array<IntRect?> = emptyArray()

    /**
     * The enabled group ids [rebuildSlots] last ran with, in [SlotData.groups] iteration order.
     * Used by [applySlotData] to tell a genuine shape change (e.g. switching tabs to a group
     * backed by different storage) from a mere reposition (scrolling, resizing) that must not
     * discard existing [Slot] identity.
     */
    private var registeredGroupIds: List<String> = emptyList()

    val blockEntityState: ComposeBlockEntityState = getOrCreateBlockEntityState(tile.blockPos)

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
    private val slotFilters: MutableMap<String, Predicate<ItemStack>> = mutableMapOf()

    protected val player: Player  = playerInventory.player
    protected val level: Level    = player.level()

    init
    {
        BlockEntityStateManager.registerBlockEntity(tile)
        if (!level.isClientSide)
        {
            BlockEntityStateManager.addTrackedPlayer(tile, player as ServerPlayer)
        }
    }

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
     * Registers a generic [CommonStorage] handler for the named slot group.
     */
    protected fun handler(group: String, storage: CommonStorage<ItemResource>, filter: Predicate<ItemStack> = Predicate { true }) {
        slotHandlers[group] = storage
        slotFilters[group] = filter
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
        applySlotData()
        broadcastFullState()
        // Notify server of the new layout so it can validate slot indices
        ArchieNetworkChannel.toServer(data)
    }

    fun slotLayerDepth(slotIndex: Int): Int = slotLayerDepthByIndex.getOrElse(slotIndex) { 0 }
    fun slotClipBounds(slotIndex: Int): IntRect? = slotClipBoundsByIndex.getOrNull(slotIndex)

    /**
     * Whether the slot at [slotIndex] currently overlaps its group's clip bounds (if it has
     * any, i.e. it sits inside a [net.kernelpanicsoft.archie.gui.composables.containers.Scrollable]).
     *
     * Used as this slot's [Slot.isActive] - the same vanilla hook that hides the Donkey/Mule
     * armor slot - so a slot scrolled out of view stops rendering its item icon and stops being
     * hoverable/clickable, without needing to remove it from [slots] and break its identity.
     * A slot with no clip bounds (not inside a scrollable) is always visible.
     */
    fun isSlotVisible(slotIndex: Int): Boolean {
        val clip = slotClipBoundsByIndex.getOrNull(slotIndex) ?: return true
        val slot = slots.getOrNull(slotIndex) ?: return true
        val absX = screenLeftPos + slot.x
        val absY = screenTopPos + slot.y
        return absX + 16 > clip.minX && absX < clip.maxX && absY + 16 > clip.minY && absY < clip.maxY
    }

    /**
     * Re-derives every [Slot]'s pixel position from the current [slotData] without touching
     * slot identity or [slotHandlers]. Call after [screenLeftPos]/[screenTopPos] change -
     * slot coordinates have the screen offset baked into their subtraction (see
     * [repositionSlots]), so they go stale whenever the screen recenters, independent of
     * whether Compose's own slot layout changed.
     */
    fun refreshSlotPositions() {
        if (slots.isEmpty()) return
        repositionSlots()
        rebuildSlotMetadataMaps()
    }

    /**
     * Applies the current [slotData]: rebuilds the vanilla [Slot] list from scratch only when
     * the set of enabled groups actually changed (or on first layout); otherwise repositions
     * the existing slots in place.
     *
     * Every layout pass - including a single frame of scrolling - re-reports the full
     * [SlotData], so rebuilding unconditionally here would discard and recreate every [Slot]
     * object on every scroll tick / resize, breaking anything holding a reference to one
     * (drag-in-progress, vanilla's own hovered-slot tracking, quick-move).
     */
    private fun applySlotData() {
        val enabledGroupIds = slotData.groups.entries.filter { it.value.enabled }.map { it.key }
        if (slots.isEmpty() || enabledGroupIds != registeredGroupIds) {
            rebuildSlots()
            registeredGroupIds = enabledGroupIds
        } else {
            repositionSlots()
        }
        rebuildSlotMetadataMaps()
    }

    /**
     * Full slot-list (re)construction: registers all menu slots and player slots.
     * Only called by [applySlotData] when the enabled group set actually changed.
     */
    private fun rebuildSlots() {
        registerSlotHandlers()
        this.slots.clear()
        this.lastSlots.clear()
        this.remoteSlots.clear()
        addMenuSlots()
        addPlayerSlots()
        repositionSlots()
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
            if (group.enabled)
            {
                slotHandlers[id]?.let { _ ->
                    for (row in 0 until group.size.height)
                    {
                        for (col in 0 until group.size.width)
                        {
                            if (slotIndex < slots.size)
                            {
                                val mcSlot = slots[slotIndex]
                                mcSlot.x = group.pos.x + 1 + col * 18 - screenLeftPos
                                mcSlot.y = group.pos.y + 1 + row * 18 - screenTopPos
                                slotIndex++
                            }
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
        ready = true
    }

    private fun rebuildSlotMetadataMaps() {
        val depths = ArrayList<Int>(slots.size)
        val clips = ArrayList<IntRect?>(slots.size)

        slotData.groups.forEach { (id, group) ->
            if (!group.enabled) return@forEach
            if (slotHandlers[id] == null) return@forEach
            val clip = group.clip
            repeat(group.size.width * group.size.height) {
                depths += group.layerDepth
                clips += clip
            }
        }

        val playerClip = slotData.playerGroup.clip
        repeat(36) {
            depths += slotData.playerGroup.layerDepth
            clips += playerClip
        }

        while (depths.size < slots.size) {
            depths += 0
            clips += null
        }
        slotLayerDepthByIndex = depths.toIntArray()
        slotClipBoundsByIndex = clips.toTypedArray()
    }

    // ── Internal slot helpers ──────────────────────────────────────────────

    private fun addMenuSlots() {
        slotData.groups.forEach { (id, group) ->
            if (!group.enabled) return@forEach
            slotHandlers[id]?.let { handler ->
                // Use slot-relative coords (subtract screen offset so vanilla adds it back correctly)
//	            group.slots.forEachIndexed { slot, coords ->
//		            slot(handler, slot, coords.x - screenLeftPos, coords.y - screenTopPos)
//	            }
                slotGrid(group.pos.x - screenLeftPos, group.pos.y - screenTopPos, group.size.width, group.size.height, handler, slotFilters[id] ?: Predicate { true })
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
                    slot(playerInventory, {true}, col + row * 9 + 9, relX + col * 18, relY + row * 18)
                }
            }
            // Hotbar (playerInventory indices 0–8), 58px below main inventory
            for (col in 0 until 9) {
                slot(playerInventory, {true}, col, relX + col * 18, relY + 58)
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

    protected fun slotGrid(x: Int, y: Int, width: Int, height: Int, container: Container, filter: Predicate<ItemStack> = Predicate { true }) {
        slotGrid(x, y, width, height) { slot(container, filter, slot, this.x, this.y) }
    }

    protected fun slotGrid(x: Int, y: Int, width: Int, height: Int, storage: CommonStorage<ItemResource>, filter: Predicate<ItemStack> = Predicate { true }) {
        slotGrid(x, y, width, height) { slot(storage, filter, slot, this.x, this.y) }
    }

    protected fun slot(mcSlot: Slot) { addSlot(mcSlot) }

    protected fun slot(storage: CommonStorage<ItemResource>, filter: Predicate<ItemStack> = Predicate { true }, slot: Int, x: Int, y: Int) {
        if (slot !in 0 until storage.size()) return
	    when (storage)
	    {
		    is ArchieItemStorage -> slot(storage, filter, slot, x, y)
		    is AbstractVanillaContainer -> slot(storage, filter, slot, x, y)
	    }
    }

    protected fun slot(container: Container, filter: Predicate<ItemStack> = Predicate { true }, slot: Int, x: Int, y: Int) {
        addSlot(object : Slot(container, slot, x, y)
        {
            override fun mayPlace(itemStack: ItemStack): Boolean = filter.test(itemStack)
            override fun isActive(): Boolean = isSlotVisible(index)
        })
    }

    protected fun slot(storage: ArchieItemStorage, filter: Predicate<ItemStack> = Predicate { true }, slot: Int, x: Int, y: Int) {
        addSlot(ArchieItemMenuSlot(storage, filter, slot, x, y, this))
    }

    protected fun slot(storage: AbstractVanillaContainer, filter: Predicate<ItemStack> = Predicate { true }, slot: Int, x: Int, y: Int) {
        addSlot(VanillaMenuSlot(storage, filter, slot, x, y, this))
    }

    // ── AbstractContainerMenu overrides ────────────────────────────────────

    /**
     * Shift-click handling: menu slots move into the player inventory/hotbar, and player
     * slots move into the menu, falling back between main inventory and hotbar when the menu
     * has no room. Slot ranges are derived from [slots].size rather than hardcoded, since the
     * number of menu slots varies with which groups are enabled.
     */
    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        val slot = slots.getOrNull(index) ?: return ItemStack.EMPTY
        if (!slot.hasItem()) return ItemStack.EMPTY

        val stackInSlot = slot.item
        val copied = stackInSlot.copy()

        val totalSlots = slots.size
        val playerSlotCount = 36
        val playerStart = (totalSlots - playerSlotCount).coerceAtLeast(0)
        val playerEndExclusive = totalSlots
        val menuStart = 0
        val menuEndExclusive = playerStart
        val hotbarSize = 9
        val hotbarStart = (playerEndExclusive - hotbarSize).coerceAtLeast(playerStart)
        val inventoryStart = playerStart
        val inventoryEndExclusive = hotbarStart

        val moved = when {
            // From menu -> player inventory/hotbar
            index in menuStart until menuEndExclusive ->
                moveItemStackTo(stackInSlot, playerStart, playerEndExclusive, true)

            // From player main inventory -> menu first, then hotbar fallback
            index in inventoryStart until inventoryEndExclusive -> {
                val movedToMenu = menuEndExclusive > menuStart && moveItemStackTo(stackInSlot, menuStart, menuEndExclusive, false)
                movedToMenu || moveItemStackTo(stackInSlot, hotbarStart, playerEndExclusive, false)
            }

            // From hotbar -> menu first, then main inventory fallback
            index in hotbarStart until playerEndExclusive -> {
                val movedToMenu = menuEndExclusive > menuStart && moveItemStackTo(stackInSlot, menuStart, menuEndExclusive, false)
                movedToMenu || moveItemStackTo(stackInSlot, inventoryStart, inventoryEndExclusive, false)
            }

            else -> false
        }

        if (!moved) return ItemStack.EMPTY

        if (stackInSlot.isEmpty) slot.set(ItemStack.EMPTY) else slot.setChanged()
        slot.onTake(player, stackInSlot)
        return copied
    }

    override fun clicked(slotId: Int, button: Int, clickType: ClickType, player: Player) {
        super.clicked(slotId, button, clickType, player)
        broadcastChanges()
    }

    override fun stillValid(player: Player): Boolean = true
    override fun removed(player: Player)
    {
        super.removed(player)
        BlockEntityStateManager.unregisterBlockEntity(tile)
        if (!level.isClientSide)
        {
            BlockEntityStateManager.removeTrackedPlayer(tile, player as ServerPlayer)
        }
    }

    // ── Networking ─────────────────────────────────────────────────────────

    companion object {

        /**
         * Registers the serverbound [SlotData] packet handler that keeps the server's slot
         * positions/clip bounds in sync with whichever [ComposeContainerMenu] the sending
         * player has open. Call once during network channel setup.
         */
        fun register() {
            ArchieNetworkChannel.serverbound(SlotData::class) { data, context ->
                 val menu = context.player.containerMenu
                 if (menu is ComposeContainerMenu<*, *>) {
                     menu.slotData = data
                     // Match slot positions on the server to the client layout
                     menu.applySlotData()
                     menu.broadcastFullState()
                 }
             }
         }
     }
 }
