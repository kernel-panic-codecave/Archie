package net.kernelpanicsoft.archie.gui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import kotlinx.serialization.Serializable
import net.kernelpanicsoft.archie.Archie
import net.kernelpanicsoft.archie.gui.composables.basic.Spacer
import net.kernelpanicsoft.archie.gui.composables.theme.TextureStates
import net.kernelpanicsoft.archie.gui.layout.*
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.modifiers.OnGloballyPositionedModifier
import net.kernelpanicsoft.archie.gui.modifiers.OnSizeChangedModifier
import net.kernelpanicsoft.archie.gui.modifiers.height
import net.kernelpanicsoft.archie.gui.modifiers.onGloballyPositioned
import net.kernelpanicsoft.archie.gui.modifiers.size
import net.kernelpanicsoft.archie.gui.modifiers.sizeIn
import net.kernelpanicsoft.archie.gui.nodes.AUINode
import net.kernelpanicsoft.archie.gui.theme.LocalTheme
import net.kernelpanicsoft.archie.gui.theme.NinePatchThemeState
import net.kernelpanicsoft.archie.gui.theme.SimpleThemeState
import net.kernelpanicsoft.archie.gui.util.extension.ninePatchTexture
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.resources.ResourceLocation

/**
 * Per-slot-group layout data reported back from the Compose layout to [ComposeContainerMenu].
 *
 * @property x      Absolute screen x of the group's top-left corner (set by [Slots]).
 * @property y      Absolute screen y of the group's top-left corner (set by [Slots]).
 * @property width  Number of slot columns in this group.
 * @property height Number of slot rows in this group.
 * @property slots  Absolute screen coordinates of each rendered [Slot] within this group.
 */
@Serializable
data class SlotGroup(
    var pos: IntCoordinates = IntCoordinates(0, 0),
    var size: IntSize = IntSize(0, 0),
    var slots: MutableSet<IntCoordinates> = mutableSetOf(),
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
    val slots: Set<IntCoordinates> get() = groups.values.flatMap { it.slots }.toSet()
}

/** Provides the [SlotData] to all composables within a [ComposeContainerScreen]. */
val LocalSlotData = compositionLocalOf { SlotData() }

/** Provides the current [SlotGroup] to [Slot] composables inside a [Slots] container. */
val LocalSlotGroup = compositionLocalOf { SlotGroup() }

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
            for (i in 0 until height)
            {
                Row {
                    for (j in 0 until width)
                    {
                        Slot()
                    }
                }
            }
        }
    },
): SlotGroup {
    val group = SlotGroup(size = IntSize(width = width, height = height))
    val data = LocalSlotData.current
    data.groups[id] = group

    // Clear slots so re-layout starts fresh each composition pass
    group.slots.clear()

    Box(
        modifier = Modifier.onGloballyPositioned { coords ->
            group.pos = coords
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
    val state = composableTheme.getState(TextureStates.DEFAULT, theme.mode)

    Layout(
        name = "Slot",
        measurePolicy = { _, _, constraints ->
            MeasureResult(constraints.minWidth, constraints.minHeight) {}
        },
        renderer = object : Renderer {
//            private val SLOT = ResourceLocation.fromNamespaceAndPath(Archie.MOD_ID, "textures/gui/slot.png")
            override fun render(
                node: AUINode, x: Int, y: Int,
                guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float,
            ) {
//                guiGraphics.blit(SLOT, x, y, 18, 18, 0f, 0f, 18, 18, 18, 18)
                if (composableTheme.isNinepatch) return guiGraphics.ninePatchTexture(
                    x,
                    y,
                    node.width,
                    node.height,
                    state as NinePatchThemeState
                )

                guiGraphics.blit(
                    (state as SimpleThemeState).texture,
                    x,
                    y,
                    state.width,
                    state.height,
                    state.u.toFloat(),
                    state.v.toFloat(),
                    state.textureSize.width,
                    state.textureSize.height,
                    state.uWidth,
                    state.vHeight
                )

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
    val menu = LocalContainerMenu.current

    // Clear so re-layout starts fresh
    data.playerGroup.slots.clear()

    Box(
        modifier = Modifier.onGloballyPositioned { coords ->
            data.playerGroup.pos = coords
        }
    ) {
        Column {
            // 3 rows of 9 (main inventory)
            for (i in 0 until 3) {
                Row {
                    for (j in 0 until 9) {
                        PlayerSlot()
                    }
                }
            }
            // 58px gap to match vanilla inventory layout
            Spacer(modifier = Modifier.size(4))
            // Hotbar (1 row of 9)
            Row {
                for (i in 0 until 9) {
                    PlayerSlot()
                }
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
    val state = composableTheme.getState(TextureStates.DEFAULT, theme.mode)
    Layout(
        name = "PlayerSlot",
        measurePolicy = { _, _, constraints ->
            MeasureResult(constraints.minWidth, constraints.minHeight) {}
        },
        renderer = object : Renderer {
//            private val SLOT = ResourceLocation.fromNamespaceAndPath(Archie.MOD_ID, "textures/gui/slot.png")
            override fun render(
                node: AUINode, x: Int, y: Int,
                guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float,
            ) {
//                guiGraphics.blit(SLOT, x, y, 18, 18, 0f, 0f, 18, 18, 18, 18)
                if (composableTheme.isNinepatch) return guiGraphics.ninePatchTexture(
                    x,
                    y,
                    node.width,
                    node.height,
                    state as NinePatchThemeState
                )

                guiGraphics.blit(
                    (state as SimpleThemeState).texture,
                    x,
                    y,
                    state.width,
                    state.height,
                    state.u.toFloat(),
                    state.v.toFloat(),
                    state.textureSize.width,
                    state.textureSize.height,
                    state.uWidth,
                    state.vHeight
                )

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
