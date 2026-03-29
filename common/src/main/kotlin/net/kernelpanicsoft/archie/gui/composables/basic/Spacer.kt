package net.kernelpanicsoft.archie.gui.composables.basic

import androidx.compose.runtime.Composable
import net.kernelpanicsoft.archie.gui.layout.Layout
import net.kernelpanicsoft.archie.gui.layout.MeasureResult
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.modifiers.fillMaxSize

/**
 * An invisible layout composable that expands to fill available space.
 *
 * `Spacer` is the idiomatic way to push siblings apart inside [net.kernelpanicsoft.archie.gui.layout.Row]
 * or [net.kernelpanicsoft.archie.gui.layout.Column] arrangements. By default it stretches
 * to consume all remaining space in its parent.
 *
 * ### Example
 * ```kotlin
 * Row {
 *     Text(Component.literal("Left"))
 *     Spacer()          // pushes "Right" to the far end
 *     Text(Component.literal("Right"))
 * }
 * ```
 *
 * @param modifier Additional modifiers; most commonly used to constrain the spacer to a
 *   fixed size with `Modifier.size(width, height)`.
 */
@Composable
fun Spacer(modifier: Modifier = Modifier) {
    Layout(
        measurePolicy = { _, _, constraints ->
            MeasureResult(constraints.minWidth, constraints.minHeight) {}
        },
        modifier = modifier.fillMaxSize(),
    )
}
