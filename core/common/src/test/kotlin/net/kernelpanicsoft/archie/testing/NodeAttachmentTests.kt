package net.kernelpanicsoft.archie.testing

import net.kernelpanicsoft.archie.gui.layout.LayoutNode
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * [LayoutNode.isAttachedTo] is what lets a cached node be trusted across recompositions.
 *
 * `Layer.rootContainerNode` used to be a `by lazy`, so once recomposition replaced the
 * RootContainer node the layer kept folding `screenPos`/`screenSize` over the detached original -
 * which is never measured again, so a container screen's origin, and the clip rect its vanilla
 * slots render through, froze at pre-recomposition values until the GUI was reopened.
 *
 * Attach/detach here mirrors what `LayoutNodeApplier` does: insert sets `parent`, remove clears it.
 */
class NodeAttachmentTests {

    private fun attach(parent: LayoutNode, child: LayoutNode) {
        parent.children.add(child)
        child.parent = parent
    }

    private fun detach(parent: LayoutNode, child: LayoutNode) {
        parent.children.remove(child)
        child.parent = null
    }

    @Test
    fun testAttachedNodeReportsItsRoot() {
        val root = LayoutNode("Root")
        val container = LayoutNode("RootContainer")
        val leaf = LayoutNode("Slot")
        attach(root, container)
        attach(container, leaf)

        assertTrue(container.isAttachedTo(root)) { "A direct child must report as attached" }
        assertTrue(leaf.isAttachedTo(root)) { "A deeper descendant must report as attached" }
    }

    @Test
    fun testDetachedNodeIsNoLongerAttached() {
        val root = LayoutNode("Root")
        val container = LayoutNode("RootContainer")
        attach(root, container)
        detach(root, container)

        assertFalse(container.isAttachedTo(root)) {
            "A removed node must not report as attached - its geometry is frozen from here on"
        }
    }

    /** Detaching a subtree's top strands everything under it, however deep. */
    @Test
    fun testDetachingASubtreeStrandsItsDescendants() {
        val root = LayoutNode("Root")
        val container = LayoutNode("RootContainer")
        val leaf = LayoutNode("Slot")
        attach(root, container)
        attach(container, leaf)
        detach(root, container)

        assertFalse(leaf.isAttachedTo(root)) { "A descendant of a removed node must not report as attached" }
    }

    /** Replacing the container, the case that actually bit: the old one must not still look live. */
    @Test
    fun testAReplacedContainerDoesNotMasqueradeAsCurrent() {
        val root = LayoutNode("Root")
        val original = LayoutNode("RootContainer")
        attach(root, original)
        detach(root, original)
        val replacement = LayoutNode("RootContainer")
        attach(root, replacement)

        assertFalse(original.isAttachedTo(root)) { "The replaced node must be rejected" }
        assertTrue(replacement.isAttachedTo(root)) { "The live node must be accepted" }
        assertTrue(root.findNode("RootContainer") === replacement) {
            "A fresh lookup must find the replacement, not the orphan"
        }
    }
}
