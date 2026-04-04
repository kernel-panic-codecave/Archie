package net.kernelpanicsoft.archie.gui.modifiers.position

import androidx.compose.runtime.Stable
import net.kernelpanicsoft.archie.gui.layout.IntOffset
import net.kernelpanicsoft.archie.gui.modifiers.LayoutChangingModifier
import net.kernelpanicsoft.archie.gui.modifiers.Modifier

/**
 * A [Modifier.Element] that shifts a composable's position by a fixed pixel offset after
 * layout has been computed.
 *
 * The offset is applied on top of any position assigned by the parent layout; it does not
 * affect the parent's size calculation.
 *
 * Only the **last** [OffsetModifier] in a chain takes effect.
 *
 * @property offset The pixel offset to apply as an [IntOffset] value.
 */
data class OffsetModifier(val offset: IntOffset) : Modifier.Element<OffsetModifier>, LayoutChangingModifier {
    override fun mergeWith(other: OffsetModifier): OffsetModifier = other

    override fun modifyPosition(offset: IntOffset): IntOffset = offset + this.offset
}

/**
 * Shifts the composable by ([x], [y]) pixels after layout.
 *
 * The shift does not affect the space reserved for the composable in its parent layout.
 *
 * @param x Horizontal pixel offset (positive moves right).
 * @param y Vertical pixel offset (positive moves down).
 */
@Stable
fun Modifier.offset(x: Int = 0, y: Int = 0): Modifier = this then OffsetModifier(IntOffset(x, y))
