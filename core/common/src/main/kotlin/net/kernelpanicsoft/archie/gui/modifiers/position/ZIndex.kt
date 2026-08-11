package net.kernelpanicsoft.archie.gui.modifiers.position

import androidx.compose.runtime.Stable
import net.kernelpanicsoft.archie.gui.modifiers.Modifier

/**
 * A [Modifier.Element] that controls the rendering and input-dispatch order of a composable
 * relative to its siblings.
 *
 * A higher [zIndex] causes the node to be drawn on top of siblings with lower z-indices and
 * to receive input events first. The effective depth is accumulated hierarchically: each
 * node's z-index is added to its parent's computed depth.
 *
 * When multiple [ZIndexModifier] elements are chained on the same node, the **last** one wins.
 *
 * @property zIndex The z-index value. Positive values move the node towards the viewer.
 */
data class ZIndexModifier(val zIndex: Float) : Modifier.Element<ZIndexModifier> {
    /** When multiple z-index modifiers exist on the same node, the last one always wins. */
    override fun mergeWith(other: ZIndexModifier): ZIndexModifier = other

    override fun toString(): String = "ZIndexModifier(zIndex=$zIndex)"
}

/**
 * Sets the rendering depth of this composable relative to its siblings.
 *
 * Higher values appear on top and receive input events before lower-valued siblings.
 *
 * @param zIndex The z-index to apply.
 */
@Stable
fun Modifier.zIndex(zIndex: Float): Modifier = this then ZIndexModifier(zIndex)
