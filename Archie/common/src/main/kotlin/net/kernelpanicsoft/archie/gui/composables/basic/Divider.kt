package net.kernelpanicsoft.archie.gui.composables.basic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.modifiers.appearance.BackgroundModifier
import net.kernelpanicsoft.archie.gui.modifiers.fillMaxHeight
import net.kernelpanicsoft.archie.gui.modifiers.fillMaxWidth
import net.kernelpanicsoft.archie.gui.modifiers.height
import net.kernelpanicsoft.archie.gui.modifiers.width
import net.kernelpanicsoft.archie.gui.util.KColor

/** Draws a thin horizontal or vertical separator line. */
@Composable
fun Divider(
    modifier: Modifier = Modifier,
    color: Int = KColor.GRAY.argb,
    thickness: Int = 1,
    vertical: Boolean = false,
) {
    val axisModifier = if (vertical) {
        Modifier.width(thickness).fillMaxHeight()
    } else {
        Modifier.height(thickness).fillMaxWidth()
    }

    Spacer(modifier = axisModifier.then(BackgroundModifier(color, color)).then(modifier))
}

/** Convenience horizontal divider. */
@Stable
@Composable
fun HorizontalDivider(
    modifier: Modifier = Modifier,
    color: Int = KColor.GRAY.argb,
    thickness: Int = 1,
) = Divider(modifier = modifier, color = color, thickness = thickness)

/** Convenience vertical divider. */
@Stable
@Composable
fun VerticalDivider(
    modifier: Modifier = Modifier,
    color: Int = KColor.GRAY.argb,
    thickness: Int = 1,
) = Divider(modifier = modifier, color = color, thickness = thickness, vertical = true)

