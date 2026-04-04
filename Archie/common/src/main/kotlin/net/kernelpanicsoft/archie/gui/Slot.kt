package net.kernelpanicsoft.archie.gui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import kotlinx.serialization.Serializable
import net.kernelpanicsoft.archie.gui.composables.theme.TextureStates
import net.kernelpanicsoft.archie.gui.layer.LocalLayerDepth
import net.kernelpanicsoft.archie.gui.layout.*
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.modifiers.onGloballyPositioned
import net.kernelpanicsoft.archie.gui.modifiers.position.padding
import net.kernelpanicsoft.archie.gui.modifiers.sizeIn
import net.kernelpanicsoft.archie.gui.nodes.AUINode
import net.kernelpanicsoft.archie.gui.theme.LocalTheme
import net.kernelpanicsoft.archie.gui.theme.ThemeVariants
import net.kernelpanicsoft.archie.gui.util.extension.drawThemeState
import net.minecraft.client.gui.GuiGraphics

/**
 * Per-slot-group layout data reported back from the Compose layout to [ComposeContainerMenu].
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
}

/** Provides the [SlotData] to all composables within a [ComposeContainerScreen]. */
val LocalSlotData = compositionLocalOf { SlotData() }

/** Provides the current [SlotGroup] to [Slot] composables inside a [Slots] container. */
val LocalSlotGroup = compositionLocalOf { SlotGroup() }

/** Provides the active clip bounds (if any) applied by ancestor scroll/clip containers. */
val LocalSlotClipBounds = compositionLocalOf<IntRect?> { null }

/**
 * Defines a named region of inventory slots within a [ComposeContainerScreen].
 *
 * This composable tracks its absolute on-screen position and populates the enclosing
 * [SlotData] with the group's location and dimensions so that [ComposeContainerMenu] can
 * register the corresponding vanilla [net.minecraft.world.inventory.Slot]s.
 *
 * @param id      The name that matches the `handler(id, storage)` call in your menu.
 * @param width   The number of slot columns in this group.
 * @param height  The number of slot rows in this group.
 * @param content The composable [Slot] grid inside this region.
 * @return The [SlotGroup] that will be populated once the layout runs.
 */
@Composable
fun Slots(
    id: String,
    width: Int,
    height: Int,
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
    val clipBounds = LocalSlotClipBounds.current
    val group = remember(id) { SlotGroup(size = IntSize(width = width, height = height)) }
    group.size = IntSize(width = width, height = height)
    group.layerDepth = layerDepth
    val data = LocalSlotData.current
    data.groups[id] = group
    group.clip = clipBounds

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
        modifier = Modifier.onGloballyPositioned { coords ->
            group.pos = coords
            group.layerDepth = layerDepth
            group.clip = clipBounds
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
 * Renders a single inventory slot graphic and records its absolute screen position.
 *
 * Triggers [ComposeContainerMenu.updateSlotData] once **all** named groups and the player
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

    Layout(
        name = "Slot",
        measurePolicy = { _, _, constraints ->
            MeasureResult(constraints.minWidth, constraints.minHeight) {}
        },
        renderer = object : Renderer {
            override fun render(
                node: AUINode, x: Int, y: Int,
                guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float,
            ) {
                guiGraphics.drawThemeState(state, x, y, node.width, node.height)

                super.render(node, x, y, guiGraphics, mouseX, mouseY, partialTick)
            }
        },
        modifier = Modifier
            .sizeIn(minWidth = 18, minHeight = 18)
            .onGloballyPositioned { pos ->
                group.slots.add(pos)
                tryUpdateMenu(data, menu)
            }
            .then(modifier),
    )
}

/**
 * Renders the standard 4-row player inventory (3 main rows + hotbar) as [Slot] composables.
 *
 * The 58-pixel gap between the main inventory and the hotbar matches the pixel offset used
 * by [ComposeContainerMenu.addPlayerSlots] so positions reported to the menu are consistent.
 */
@Composable
fun PlayerSlots() {
    val data = LocalSlotData.current
    val layerDepth = LocalLayerDepth.current
    val clipBounds = LocalSlotClipBounds.current
    data.playerGroup.layerDepth = layerDepth
    data.playerGroup.clip = clipBounds

    // Clear so re-layout starts fresh
    data.playerGroup.slots.clear()

    Box(
        modifier = Modifier.onGloballyPositioned { coords ->
            data.playerGroup.pos = coords
            data.playerGroup.layerDepth = layerDepth
            data.playerGroup.clip = clipBounds
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
    Layout(
        name = "PlayerSlot",
        measurePolicy = { _, _, constraints ->
            MeasureResult(constraints.minWidth, constraints.minHeight) {}
        },
        renderer = object : Renderer {
            override fun render(
                node: AUINode, x: Int, y: Int,
                guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float,
            ) {
                guiGraphics.drawThemeState(state, x, y, node.width, node.height)

                super.render(node, x, y, guiGraphics, mouseX, mouseY, partialTick)
            }
        },
        modifier = modifier
            .sizeIn(minWidth = 18, minHeight = 18)
            .onGloballyPositioned { pos ->
                data.playerGroup.slots.add(pos)
                tryUpdateMenu(data, menu)
            },
    )
}

/**
 * Fires [ComposeContainerMenu.updateSlotData] only when every named slot group AND the
 * player group have all reported their slot positions for this layout pass.
 *
 * This prevents partial updates where only some groups are positioned.
 */
private fun tryUpdateMenu(data: SlotData, menu: ComposeContainerMenu<*, *>) {
    val namedGroupsFull = data.groups.values.all { g -> g.slots.size >= g.size.width * g.size.height }
    val playerGroupFull = data.playerGroup.slots.size >= 36
    if (namedGroupsFull && playerGroupFull) {
        menu.updateSlotData(data)
    }
}
