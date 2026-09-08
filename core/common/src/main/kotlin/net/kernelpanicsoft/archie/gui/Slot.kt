package net.kernelpanicsoft.archie.gui

import androidx.compose.runtime.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.kernelpanicsoft.archie.gui.composables.theme.TextureStates
import net.kernelpanicsoft.archie.gui.layer.LocalLayerDepth
import net.kernelpanicsoft.archie.gui.layout.*
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.modifiers.onGloballyPositioned
import net.kernelpanicsoft.archie.gui.modifiers.onSizeChanged
import net.kernelpanicsoft.archie.gui.modifiers.position.padding
import net.kernelpanicsoft.archie.gui.modifiers.size
import net.kernelpanicsoft.archie.gui.modifiers.sizeIn
import net.kernelpanicsoft.archie.gui.nodes.UINode
import net.kernelpanicsoft.archie.gui.theme.LocalTheme
import net.kernelpanicsoft.archie.gui.theme.ThemeVariants
import net.kernelpanicsoft.archie.gui.util.extension.drawThemeState
import net.kernelpanicsoft.archie.gui.composables.containers.PLAYER_INVENTORY_GAP
import net.kernelpanicsoft.archie.gui.composables.containers.Scrollable
import net.minecraft.client.gui.GuiGraphics

/**
 * Per-slot-group layout data reported back from the Compose layout to [ComposeContainerMenuBase].
 *
 * Stores the group's absolute screen position, dimensions, slot positions, and clip bounds.
 */
@Serializable
data class SlotGroup(
	var pos: IntCoordinates = IntCoordinates(0, 0),
	var size: IntSize = IntSize(0, 0),
	var enabled: Boolean = true,
	var layerDepth: Int = 0,
	var slots: MutableSet<IntCoordinates> = mutableSetOf(),
	var clip: IntRect? = null,
)

/**
 * Aggregated slot layout data for an entire [ComposeContainerScreen].
 *
 * Contains named groups for block-entity slots and a separate [playerGroup] for the
 * player inventory rows.
 */
@Serializable
data class SlotData(
    val groups: MutableMap<String, SlotGroup> = mutableMapOf(),
    val playerGroup: SlotGroup = SlotGroup(size = IntSize(9, 3)),
) {
    /** All slot coordinates across all groups (does not include [playerGroup]). */
    val slots: Set<IntCoordinates> get() = groups.values.filter { it.enabled }.flatMap { it.slots }.toSet()

    /**
     * Layer depths that currently have a [Slots] (or [PlayerSlots]) on screen for each group id.
     *
     * A slot group exists once per menu - one set of vanilla slots, at one position, at one depth -
     * but more than one composable may want to *place* it. A layer renders above the slots of every
     * layer beneath it, and (since the [ComposeContainerScreen.isHovering] fix that goes with this)
     * only the top layer's slots answer the mouse, so a layer that covers a group its host placed
     * would otherwise leave those slots visible-ish and dead. The player inventory is the case that
     * forced this - a modal has to let the player reach their own items - but nothing about it is
     * specific to the player row, so any named group can be taken over the same way.
     *
     * Placement is therefore *claimed* rather than assumed, and the deepest claim wins; see
     * [slotGroupOwner]. Not serialized: composition bookkeeping, not layout the menu needs told
     * about.
     */
    @Transient
    private val slotGroupClaims: MutableMap<String, List<Int>> = mutableStateMapOf()

    /**
     * The layer depth currently placing the group named [id] - the deepest [Slots] on screen
     * declaring it, or `0` when nothing has claimed it.
     *
     * Backed by a snapshot map holding immutable lists, so a shallower declaration of the same
     * group recomposes when a layer takes it over; a plain map of mutable lists would mutate
     * unobserved and leave the loser still drawing a group that has moved.
     */
    fun slotGroupOwner(id: String): Int = slotGroupClaims[id]?.maxOrNull() ?: 0

    /** [slotGroupOwner] for the player inventory - see [PLAYER_GROUP_ID]. */
    val playerSlotsOwner: Int get() = slotGroupOwner(PLAYER_GROUP_ID)

    internal fun claimSlotGroup(id: String, depth: Int) {
        slotGroupClaims[id] = (slotGroupClaims[id] ?: emptyList()) + depth
    }

    internal fun releaseSlotGroup(id: String, depth: Int) {
        val remaining = (slotGroupClaims[id] ?: return) - depth
        if (remaining.isEmpty()) slotGroupClaims.remove(id) else slotGroupClaims[id] = remaining
    }

    companion object {
        /**
         * The group id [PlayerSlots] claims under.
         *
         * Namespaced so it cannot collide with a caller's own group name - the player row is not in
         * [groups] (it has its own [playerGroup] field), so this key exists only for claiming.
         */
        const val PLAYER_GROUP_ID: String = "archie:player_inventory"
    }
}

/** Provides the [SlotData] to all composables within a [ComposeContainerScreen]. */
val LocalSlotData = compositionLocalOf { SlotData() }

/** Provides the current [SlotGroup] to [Slot] composables inside a [Slots] container. */
val LocalSlotGroup = compositionLocalOf { SlotGroup() }

/**
 * A layout-synchronous (non-Compose-state) holder for a [Scrollable]'s
 * current clip bounds.
 *
 * The [Scrollable] updates [bounds] directly from its `onGloballyPositioned`/`onSizeChanged`
 * callbacks, which fire every layout pass regardless of composition state. A descendant [Slot]
 * reads [bounds] live, from its own `onGloballyPositioned` callback, at the same layout-pass
 * granularity as [SlotGroup.pos]. Using [androidx.compose.runtime.mutableStateOf] here instead
 * would only propagate the new value on the *next* recomposition - a composition-cycle lag
 * behind position tracking that let a slot's clip bounds go stale exactly when the surrounding
 * layout had just finished settling into a new position.
 */
class SlotClipSource {
	private var origin: IntCoordinates = IntCoordinates(0, 0)
	private var size: Size = Size(0, 0)

	var bounds: IntRect? = null
		private set

	fun updateOrigin(newOrigin: IntCoordinates) {
		origin = newOrigin
		recompute()
	}

	fun updateSize(newSize: Size) {
		size = newSize
		recompute()
	}

	private fun recompute() {
		bounds = if (size.width <= 0 || size.height <= 0) null else IntRect.fromPositionAndSize(origin, size)
	}
}

/** Provides the active [SlotClipSource] (if any) from the nearest ancestor scroll/clip container. */
val LocalSlotClipBounds = compositionLocalOf<SlotClipSource?> { null }

/**
 * Defines a named region of inventory slots within a [ComposeContainerScreen].
 *
 * This composable tracks its absolute on-screen position and populates the enclosing
 * [SlotData] with the group's location and dimensions so that [ComposeContainerMenuBase] can
 * register the corresponding vanilla [net.minecraft.world.inventory.Slot]s.
 *
 * **May be declared from more than one place at once** - typically a screen and a layer opened over
 * it, which needs the group's slots reachable above its own content. There is only one group per
 * [id] to place, so the deepest declaration wins it and the others reserve its footprint and draw
 * nothing; see [SlotData.slotGroupOwner].
 *
 * @param id      The name that matches the `handler(id, storage)` call in your menu.
 * @param width   The number of slot columns in this group. Defaults to 1.
 * @param height  The number of slot rows in this group. Defaults to 1.
 * @param content The composable [Slot] grid inside this region.
 * @return The [SlotGroup] that will be populated once the layout runs - the *owner's*, which is not
 *   this call site's own when something deeper has taken the group over.
 */
@Composable
fun Slots(
    id: String,
    width: Int = 1,
    height: Int = 1,
    content: @Composable () -> Unit = {
        Column {
            repeat(height) {
                Row {
                    repeat(width) {
                        Slot()
                    }
                }
            }
        }
    },
): SlotGroup {
    val layerDepth = LocalLayerDepth.current
    val clipSource = LocalSlotClipBounds.current
    val group = remember(id) { SlotGroup(size = IntSize(width = width, height = height)) }
    val data = LocalSlotData.current

    var measured by remember(id) { mutableStateOf<Size?>(null) }
    if (!claimSlotGroupPlacement(id, layerDepth)) {
        SlotGroupPlaceholder(measured, Size(width * SLOT_SIZE, height * SLOT_SIZE))
        // Deliberately *not* this call site's own group, and nothing written into [SlotData.groups]:
        // the owner's entry is the live one, and overwriting it here is exactly the clobbering this
        // claim exists to prevent.
        return data.groups[id] ?: group
    }

    group.size = IntSize(width = width, height = height)
    group.layerDepth = layerDepth
    data.groups[id] = group
    group.clip = clipSource?.bounds

    DisposableEffect(data, id, group) {
        data.groups[id] = group
        group.enabled = true
        onDispose {
            group.enabled = false
        }
    }

    // Clear slots so re-layout starts fresh each composition pass
    group.slots.clear()

    Box(
        modifier = Modifier
            // Recorded so that if a layer later takes this group over, this call site can reserve
            // the space it actually occupied rather than guessing from the slot count.
            .onSizeChanged { measured = it }
            .onGloballyPositioned { coords ->
                group.pos = coords
                group.layerDepth = layerDepth
                // Live read, not a composition-time snapshot - see SlotClipSource.
                group.clip = clipSource?.bounds
                data.groups[id] = group
            }
    ) {
        CompositionLocalProvider(LocalSlotGroup provides group) {
            content()
        }
    }
    return group
}

/**
 * Claims placement of the slot group named [id] for the layer at [depth], releasing the claim when
 * this leaves composition, and answers whether this call site is the one that should place it.
 *
 * See [SlotData.slotGroupOwner] for why placement is claimed at all.
 */
@Composable
private fun claimSlotGroupPlacement(id: String, depth: Int): Boolean {
    val data = LocalSlotData.current
    DisposableEffect(data, id, depth) {
        data.claimSlotGroup(id, depth)
        onDispose { data.releaseSlotGroup(id, depth) }
    }
    return data.slotGroupOwner(id) == depth
}

/**
 * The gap a call site that lost [claimSlotGroupPlacement] leaves behind: the footprint it last
 * [measured] while it did own the group, or [fallback] before it ever has.
 *
 * Reserved rather than collapsed so the surrounding layout holds still when a layer borrows a group
 * from it - a host panel that resized itself the moment a modal opened would jump, and jump back on
 * close, around content the player is trying to read.
 */
@Composable
private fun SlotGroupPlaceholder(measured: Size?, fallback: Size) {
    val size = measured ?: fallback
    Box(modifier = Modifier.size(size.width, size.height)) {}
}

/**
 * Renders a single inventory slot graphic and records its absolute screen position.
 *
 * Triggers [ComposeContainerMenuBase.updateSlotData] once **all** named groups and the player
 * group have reported their positions for this layout pass.
 *
 * @param modifier Additional modifiers applied to the slot layout node.
 */
@Composable
fun Slot(texture: String = "slot", modifier: Modifier = Modifier) {
    val data  = LocalSlotData.current
    val group = LocalSlotGroup.current
    val menu  = LocalContainerMenu.current
    val theme = LocalTheme.current
    val composableTheme = theme.getComposableTheme(texture)
    val state = composableTheme.getState(TextureStates.DEFAULT, ThemeVariants.DEFAULT)
    var lastPos by remember { mutableStateOf(IntCoordinates(0, 0)) }
    Layout(
        name = "Slot",
        measurePolicy = { _, _, constraints ->
            MeasureResult(constraints.minWidth, constraints.minHeight) {}
        },
        renderer = object : Renderer {
            override fun render(
                node: UINode, x: Int, y: Int,
                guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float,
            ) {
                guiGraphics.drawThemeState(state, x, y, node.width, node.height)

                super.render(node, x, y, guiGraphics, mouseX, mouseY, partialTick)
            }
        },
        modifier = Modifier
            .sizeIn(minWidth = 18, minHeight = 18)
            .onGloballyPositioned { pos ->
                if (pos == lastPos) return@onGloballyPositioned // Skip if position hasn't changed since last report
                if (group.slots.contains(lastPos))
                    group.slots.remove(lastPos)
                group.slots.add(pos)
                lastPos = pos
                tryUpdateMenu(data, menu)
            }
            .then(modifier),
    )
}

/**
 * Renders the standard 4-row player inventory (3 main rows + hotbar) as [Slot] composables.
 *
 * The 58-pixel gap between the main inventory and the hotbar matches the pixel offset used
 * by [ComposeContainerMenuBase.addPlayerSlots] so positions reported to the menu are consistent.
 *
 * **May be called from more than one place at once**, and the deepest layer wins - the same
 * claiming any named [Slots] group gets, under [SlotData.PLAYER_GROUP_ID]; see
 * [SlotData.slotGroupOwner]. A caller that does not own the row reserves the same footprint and
 * draws nothing, so the layout it sits in neither jumps nor shows a second, itemless inventory;
 * the real row appears at the owner's position, above.
 */
@Composable
fun PlayerSlots() {
    val data = LocalSlotData.current
    val layerDepth = LocalLayerDepth.current
    val clipSource = LocalSlotClipBounds.current

    if (!claimSlotGroupPlacement(SlotData.PLAYER_GROUP_ID, layerDepth)) {
        // The row's own footprint is fixed by what this composable draws, so unlike a named group
        // there is nothing to measure: three rows, the hotbar, and the 4-pixel gap between them.
        SlotGroupPlaceholder(measured = null, fallback = Size(PLAYER_ROW_WIDTH, PLAYER_ROW_HEIGHT))
        return
    }

    val screen = LocalContainerScreen.current
    data.playerGroup.layerDepth = layerDepth
    data.playerGroup.clip = clipSource?.bounds

    // Clear so re-layout starts fresh
    data.playerGroup.slots.clear()

    Box(
        modifier = Modifier.onGloballyPositioned { coords ->
            data.playerGroup.pos = coords
            data.playerGroup.layerDepth = layerDepth
            // Live read, not a composition-time snapshot - see SlotClipSource.
            data.playerGroup.clip = clipSource?.bounds
            // Reported from here rather than from whatever placed this, so the "Inventory" label
            // travels with the row when a layer claims it, instead of staying behind captioning
            // the gap the previous owner left. Offset off the row's own top-left, which is why the
            // y is negative: the label sits above the slots, not on them.
            screen.inventoryLabelPos = coords + offset(x = PLAYER_LABEL_OFFSET_X, y = PLAYER_LABEL_OFFSET_Y)
        }
    ) {
        Column {
            // 3 rows of 9 (main inventory)
            repeat(3) {
                Row {
                    repeat(9) {
                        PlayerSlot()
                    }
                }
            }
            // Hotbar (1 row of 9)
            Row(modifier = Modifier.padding(top = 4)) {
                repeat(9) { PlayerSlot() }
            }
        }
    }
}

/** A single player-inventory slot cell that tracks its position in [SlotData.playerGroup]. */
@Composable
private fun PlayerSlot(texture: String = "slot", modifier: Modifier = Modifier) {
    val data = LocalSlotData.current
    val menu = LocalContainerMenu.current
    val theme = LocalTheme.current
    val composableTheme = theme.getComposableTheme(texture)
    val state = composableTheme.getState(TextureStates.DEFAULT, ThemeVariants.DEFAULT)
    var lastPos by remember { mutableStateOf(IntCoordinates(0, 0)) }
    Layout(
        name = "PlayerSlot",
        measurePolicy = { _, _, constraints ->
            MeasureResult(constraints.minWidth, constraints.minHeight) {}
        },
        renderer = object : Renderer {
            override fun render(
                node: UINode, x: Int, y: Int,
                guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float,
            ) {
                guiGraphics.drawThemeState(state, x, y, node.width, node.height)

                super.render(node, x, y, guiGraphics, mouseX, mouseY, partialTick)
            }
        },
        modifier = modifier
            .sizeIn(minWidth = 18, minHeight = 18)
            .onGloballyPositioned { pos ->
                if (pos == lastPos) return@onGloballyPositioned // Skip if position hasn't changed since last report
                if (data.playerGroup.slots.contains(lastPos))
                    data.playerGroup.slots.remove(lastPos)
                data.playerGroup.slots.add(pos)
                lastPos = pos
                tryUpdateMenu(data, menu)
            },
    )
}

/**
 * Fires [ComposeContainerMenuBase.updateSlotData] only when every named slot group AND the
 * player group have all reported their slot positions for this layout pass.
 *
 * This prevents partial updates where only some groups are positioned.
 */
private fun tryUpdateMenu(data: SlotData, menu: ComposeContainerMenuBase<*>) {
    val namedGroupsFull = data.groups.values.all { g -> g.slots.size >= g.size.width * g.size.height }
    val playerGroupFull = data.playerGroup.slots.size >= 36
    if (namedGroupsFull && playerGroupFull) {
        menu.updateSlotData(data)
    }
}

/** The edge length of one slot, gap included - what [Slot] measures itself to. */
private const val SLOT_SIZE = 18

/** The player row's own footprint - nine columns, and four rows with the hotbar's 4-pixel gap. */
private const val PLAYER_ROW_WIDTH = 9 * SLOT_SIZE
private const val PLAYER_ROW_HEIGHT = 4 * SLOT_SIZE + 4

/**
 * Where the "Inventory" label sits relative to the top-left of the row it names - just above it.
 *
 * Off the vanilla GUI textures, same as
 * [net.kernelpanicsoft.archie.gui.composables.containers.PLAYER_INVENTORY_GAP]: the label is 3
 * pixels below the top of the gap that separates the row from the contents above it, which puts it
 * 11 above the row itself.
 */
private const val PLAYER_LABEL_OFFSET_X = 1
private const val PLAYER_LABEL_OFFSET_Y = 3 - PLAYER_INVENTORY_GAP
