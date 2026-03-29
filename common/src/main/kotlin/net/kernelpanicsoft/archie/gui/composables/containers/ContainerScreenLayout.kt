package net.kernelpanicsoft.archie.gui.composables.containers

import androidx.compose.runtime.Composable
import net.kernelpanicsoft.archie.gui.ComposeContainerScreen
import net.kernelpanicsoft.archie.gui.LocalContainerScreen
import net.kernelpanicsoft.archie.gui.PlayerSlots
import net.kernelpanicsoft.archie.gui.composables.basic.Spacer
import net.kernelpanicsoft.archie.gui.layout.Box
import net.kernelpanicsoft.archie.gui.layout.Column
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.modifiers.fillMaxSize
import net.kernelpanicsoft.archie.gui.modifiers.onGloballyPositioned
import net.kernelpanicsoft.archie.gui.modifiers.position.margin
import net.kernelpanicsoft.archie.gui.modifiers.position.padding
import net.kernelpanicsoft.archie.gui.modifiers.size

/**
 * A complete container screen layout following the vanilla chest-screen pattern.
 *
 * Combines the screen contents (top section) with the player inventory (bottom section),
 * properly positioned and spaced. Automatically sets the game's label positions
 * ([titleLabelX]/[titleLabelY] and [inventoryLabelX]/[inventoryLabelY]) based on the layout.
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
 * @param contentSpacing Vertical spacing between screen contents and player inventory in pixels.
 * @param modifier       Additional modifiers applied to the outer container.
 * @param content        The screen contents composable (container inventory, custom widgets, etc.).
 */
@Composable
fun ContainerScreenLayout(
    contentSpacing: Int = 14,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val screen = LocalContainerScreen.current

    Surface(modifier = modifier.padding(8)) {
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
            // Player inventory section
            Box(
                modifier = Modifier
                    .padding(top = contentSpacing)
                    .onGloballyPositioned { coords ->
                    screen.inventoryLabelPos = coords
                }
            ) {
                // Player inventory slots (3×9 main inventory + 1×9 hotbar)
                PlayerSlots()
            }
        }
    }
}
