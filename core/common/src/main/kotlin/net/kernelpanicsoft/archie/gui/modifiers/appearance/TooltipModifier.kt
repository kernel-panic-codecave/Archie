package net.kernelpanicsoft.archie.gui.modifiers.appearance

import androidx.compose.runtime.Stable
import net.kernelpanicsoft.archie.gui.layout.LayoutNode
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.minecraft.network.chat.Component

/**
 * A [Modifier.Element] carrying the tooltip lines a composable shows on hover.
 *
 * Multiple [TooltipModifier] elements on the same node merge by concatenating their lines, so a
 * wrapper can add a line to whatever it wraps.
 *
 * @property lines The tooltip's lines, top to bottom.
 */
data class TooltipModifier(val lines: List<Component>) : Modifier.Element<TooltipModifier> {
    override fun mergeWith(other: TooltipModifier): TooltipModifier =
        TooltipModifier(lines + other.lines)
}

/**
 * Shows [lines] as a tooltip while the pointer is over this composable.
 *
 * Declared on the composable rather than tracked by the screen, which is the point: a screen can
 * only draw a tooltip for something it knows about, so anything drawn inside a
 * [net.kernelpanicsoft.archie.gui.layer.Layer] - a modal, a popup editor - had no way to offer one
 * without every possible host screen plumbing the hover back out for it. The renderer finds the
 * hovered node itself; see [tooltipAt].
 *
 * @param lines The tooltip's lines, top to bottom. An empty list shows nothing.
 */
@Stable
fun Modifier.tooltip(vararg lines: Component): Modifier = tooltip(lines.toList())

/** [tooltip], for lines a caller already holds as a list. */
@Stable
fun Modifier.tooltip(lines: List<Component>): Modifier =
    if (lines.isEmpty()) this else this then TooltipModifier(lines)

/**
 * The tooltip of the deepest node under ([mouseX], [mouseY]) that declares one, or `null`.
 *
 * Deepest wins, and children are searched last-first: a node drawn over its siblings is the one the
 * cursor is actually on, and the draw order is what says which that is. A parent's own tooltip is
 * therefore a fallback for the area of it no child covers.
 */
fun tooltipAt(node: LayoutNode, mouseX: Int, mouseY: Int): List<Component>? {
    if (!node.isBounded(mouseX, mouseY)) return null
    // Snapshot: recomposition can restructure children from the recomposer's own thread.
    for (child in node.children.toList().asReversed()) {
        tooltipAt(child, mouseX, mouseY)?.let { return it }
    }
    var found: TooltipModifier? = null
    node.modifier.foldIn(Unit) { _, element -> if (element is TooltipModifier) found = element }
    return found?.lines
}
