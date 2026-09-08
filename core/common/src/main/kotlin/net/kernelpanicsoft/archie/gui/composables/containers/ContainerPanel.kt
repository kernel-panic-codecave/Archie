package net.kernelpanicsoft.archie.gui.composables.containers

import androidx.compose.runtime.Composable
import net.kernelpanicsoft.archie.gui.LocalContainerScreen
import net.kernelpanicsoft.archie.gui.PlayerSlots
import net.kernelpanicsoft.archie.gui.layout.Box
import net.kernelpanicsoft.archie.gui.layout.Column
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.modifiers.onGloballyPositioned
import net.kernelpanicsoft.archie.gui.modifiers.position.offset
import net.kernelpanicsoft.archie.gui.modifiers.position.padding
import net.kernelpanicsoft.archie.gui.modifiers.width
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen

private const val DEFAULT_CONTENT_WIDTH = 9 * 18

/**
 * The gap between a container's own contents and the player inventory below them, in pixels -
 * measured off the vanilla GUI textures, so a screen built out of these composables lines up with
 * one drawn the old way.
 *
 * Public because [ContainerPanel] is not the only thing that needs it: a layer that has claimed the
 * player row ([net.kernelpanicsoft.archie.gui.PlayerSlots]) assembles this same layout by hand, and
 * has nowhere else to read the spacing from.
 */
const val PLAYER_INVENTORY_GAP: Int = 14

/**
 * A complete container screen layout following the vanilla chest-screen pattern.
 *
 * Combines the screen contents (top section) with the player inventory (bottom section),
 * properly positioned and spaced. Automatically sets the game's label positions
 * ([AbstractContainerScreen.titleLabelX]/[AbstractContainerScreen.titleLabelY] and [AbstractContainerScreen.inventoryLabelX]/[AbstractContainerScreen.inventoryLabelY]) based on the layout.
 *
 * The game then renders the labels using the title and inventory label components.
 *
 * ### Layout Structure
 * ```
 * ┌─────────────────────────────────┐
 * │ [Screen Contents]               │  <- titleLabelX/Y set here
 * ├─────────────────────────────────┤  <- contentSpacing
 * │ [Player Inventory 3×9]          │  <- inventoryLabelX/Y set here
 * │ [spacing]                       │
 * │ [Hotbar 1×9]                    │
 * └─────────────────────────────────┘
 * ```
 *
 * @param contentWidth   Width of the panel's content area, in pixels. Defaults to 9 slots wide.
 * @param modifier       Additional modifiers applied to the outer container.
 * @param content        The screen contents composable (container inventory, custom widgets, etc.).
 */
@Composable
fun ContainerPanel(
    contentWidth: Int = DEFAULT_CONTENT_WIDTH,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val screen = LocalContainerScreen.current

    Panel(contentWidth = contentWidth, modifier = modifier) {
        Column {
            // Screen contents (container inventory)
            Box(
                modifier = Modifier
                    .padding(top = 10)
                    .onGloballyPositioned { coords ->
                        screen.titleLabelPos = coords
                    }
            ) {
                content()
            }
            // Player inventory section. Its label position is reported by [PlayerSlots] itself,
            // not from here: the row may be claimed by a layer drawn above this screen, and the
            // label has to follow it there.
            Box(modifier = Modifier.padding(top = PLAYER_INVENTORY_GAP)) {
                // Player inventory slots (3×9 main inventory + 1×9 hotbar)
                PlayerSlots()
            }
        }
    }
}

/**
 * A [ContainerPanel] whose contents are switched between tabs, following the same
 * container/player-inventory layout as [ContainerPanel].
 *
 * @param contentWidth Width of the panel's content area, in pixels. Defaults to 9 slots wide.
 * @param modifier     Additional modifiers applied to the outer [TabPanel].
 * @param builder      Declares the tabs; see [TabContainerScope].
 */
@Composable
fun TabContainerPanel(
    contentWidth: Int = DEFAULT_CONTENT_WIDTH,
    modifier: Modifier = Modifier,
    builder: TabContainerScope.() -> Unit
) {
    TabPanel(
        modifier = modifier.width(contentWidth + 16),
        contentWrapper = { content ->
            ContainerPanel(
                contentWidth = contentWidth,
                modifier = Modifier.offset(y = -12),
                content = content,
            )
        },
        builder = builder
    )
}
