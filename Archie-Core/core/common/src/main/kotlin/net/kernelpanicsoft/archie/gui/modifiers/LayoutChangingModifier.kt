package net.kernelpanicsoft.archie.gui.modifiers

import net.kernelpanicsoft.archie.gui.layout.IntOffset
import net.kernelpanicsoft.archie.gui.layout.IntSize

/**
 * A [Modifier.Element] that can alter a node's position and the [Constraints] seen by the
 * node and its children.
 *
 * Implement this interface alongside [Modifier.Element] when a modifier needs to change
 * where the node is placed (e.g. [net.kernelpanicsoft.archie.gui.modifiers.position.OffsetModifier],
 * [net.kernelpanicsoft.archie.gui.modifiers.position.MarginModifier]) or the space available
 * to the node's subtree (e.g. [net.kernelpanicsoft.archie.gui.modifiers.SizeModifier],
 * [net.kernelpanicsoft.archie.gui.modifiers.position.PaddingModifier]).
 */
interface LayoutChangingModifier {
    /**
     * Shifts the node's placement by transforming the parent-supplied [offset].
     *
     * @param offset The position computed by the parent layout.
     * @return The adjusted position for this node.
     */
    fun modifyPosition(offset: IntOffset): IntOffset = offset

    /**
     * Adjusts the [Constraints] as seen by the **parent** when it lays out this node.
     *
     * Use this to report a different effective size to the parent (e.g. after accounting for
     * margin space that the parent should reserve).
     *
     * @param measuredSize The actual measured size of this node.
     * @param constraints  The constraints originally passed by the parent.
     * @return The constraints the parent should use when accounting for this node's footprint.
     */
    fun modifyLayoutConstraints(measuredSize: IntSize, constraints: Constraints): Constraints =
        modifyInnerConstraints(constraints)

    /**
     * Adjusts the [Constraints] passed **into** this node for measuring its children.
     *
     * Use this to reduce the available space before measuring children (e.g. padding).
     *
     * @param constraints The constraints supplied by this node's parent.
     * @return The constraints to use when measuring this node's children.
     */
    fun modifyInnerConstraints(constraints: Constraints): Constraints = constraints
}