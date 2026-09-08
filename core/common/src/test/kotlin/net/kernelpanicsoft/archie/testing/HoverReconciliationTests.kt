package net.kernelpanicsoft.archie.testing

import net.kernelpanicsoft.archie.gui.layout.LayoutNode
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.modifiers.input.PointerEventType
import net.kernelpanicsoft.archie.gui.modifiers.input.onPointerEvent
import net.kernelpanicsoft.archie.gui.nodes.UINode
import net.kernelpanicsoft.archie.gui.util.extension.reconcilePointerHover
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Hover is reconciled from each node's own recorded state, not from cursor deltas.
 *
 * Two failures motivated this. Hover transitions used to be derived by comparing the current
 * cursor position against the previous one, so anything that moved *under* a stationary cursor -
 * a scroll, a filter, results arriving - fired nothing and left the old cell highlighted. And
 * ENTER/EXIT were dispatched as one shared consumable event, so the first node to handle it
 * consumed it and any other node that also needed to transition was skipped, stranding it hovered.
 */
class HoverReconciliationTests {

    /** Records the enter/exit events a node receives, in order, as "+name" / "-name". */
    private class Recorder {
        val events = mutableListOf<String>()
    }

    private fun node(name: String, x: Int, y: Int, width: Int, height: Int, recorder: Recorder): LayoutNode =
        LayoutNode(name).apply {
            this.x = x; this.y = y
            this.width = width; this.height = height
            modifier = Modifier
                .onPointerEvent<UINode>(PointerEventType.ENTER) { _, _ -> recorder.events += "+$name" }
                .onPointerEvent<UINode>(PointerEventType.EXIT) { _, _ -> recorder.events += "-$name" }
        }

    private fun attach(parent: LayoutNode, child: LayoutNode) {
        parent.children.add(child)
        child.parent = parent
    }

    @Test
    fun testEnteringAndLeavingFiresOncePerTransition() {
        val recorder = Recorder()
        val root = LayoutNode("Root")
        val cell = node("cell", 0, 0, 18, 18, recorder)
        attach(root, cell)

        reconcilePointerHover(root, 5.0, 5.0)
        assertEquals(listOf("+cell"), recorder.events)

        // Still inside - no repeat.
        reconcilePointerHover(root, 6.0, 6.0)
        assertEquals(listOf("+cell"), recorder.events)

        reconcilePointerHover(root, 50.0, 50.0)
        assertEquals(listOf("+cell", "-cell"), recorder.events)
    }

    /** The scroll case: the cursor never moves, the content does. */
    @Test
    fun testContentMovingUnderAStationaryCursorReconciles() {
        val recorder = Recorder()
        val root = LayoutNode("Root")
        val cell = node("cell", 0, 0, 18, 18, recorder)
        attach(root, cell)

        reconcilePointerHover(root, 5.0, 5.0)
        assertEquals(listOf("+cell"), recorder.events)

        // Scrolled out from under the cursor, which has not moved.
        cell.y = 100
        reconcilePointerHover(root, 5.0, 5.0)
        assertEquals(listOf("+cell", "-cell"), recorder.events) {
            "A cell scrolled out from under a still cursor must receive its EXIT"
        }
    }

    /** Two nodes transitioning in one pass: neither may swallow the other's event. */
    @Test
    fun testEveryTransitioningNodeIsNotified() {
        val recorder = Recorder()
        val root = LayoutNode("Root")
        val left = node("left", 0, 0, 18, 18, recorder)
        val right = node("right", 18, 0, 18, 18, recorder)
        attach(root, left)
        attach(root, right)

        reconcilePointerHover(root, 5.0, 5.0)
        assertEquals(listOf("+left"), recorder.events)

        // One move that leaves `left` and enters `right`.
        reconcilePointerHover(root, 25.0, 5.0)
        assertEquals(setOf("-left", "+right"), recorder.events.drop(1).toSet()) {
            "Both the node being left and the node being entered must be notified in one pass"
        }
    }

    /** A node and the ancestor containing it both track the pointer. */
    @Test
    fun testNestedNodesBothTransition()
    {
        val recorder = Recorder()
        val root = LayoutNode("Root")
        val container = node("container", 0, 0, 100, 100, recorder)
        val cell = node("cell", 10, 10, 18, 18, recorder)
        attach(root, container)
        attach(container, cell)

        reconcilePointerHover(root, 15.0, 15.0)
        assertEquals(setOf("+container", "+cell"), recorder.events.toSet()) {
            "An ancestor containing the cursor must hover alongside its child"
        }

        reconcilePointerHover(root, 200.0, 200.0)
        assertEquals(setOf("-container", "-cell"), recorder.events.drop(2).toSet()) {
            "Both must be released when the cursor leaves"
        }
    }
}
