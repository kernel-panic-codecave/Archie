package net.kernelpanicsoft.archie.testing

import net.kernelpanicsoft.archie.gui.layout.LayoutNode
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.modifiers.clipToBounds
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Hit-testing stops at a clipping ancestor's bounds - see
 * [net.kernelpanicsoft.archie.gui.modifiers.ClipToBoundsModifier].
 *
 * The geometry throughout mirrors the case that exposed this: a 9-wide item grid in a
 * three-row-tall scroll viewport, with far more rows of content than fit. A scissor hid the
 * overflow, but the overflowing rows kept real coordinates further down the screen - on top of
 * the inventory slots below the viewport - and stayed clickable there, so clicking a slot
 * triggered whatever invisible grid cell happened to overlap it instead.
 */
class ClipToBoundsHitTestTests {

    private companion object {
        const val ROW_HEIGHT = 18
        const val GRID_WIDTH = 162
        const val VIEWPORT_TOP = 50
        const val VIEWPORT_HEIGHT = ROW_HEIGHT * 3
    }

    private fun node(x: Int, y: Int, width: Int, height: Int, modifier: Modifier = Modifier) =
        LayoutNode().apply {
            this.x = x; this.y = y
            this.width = width; this.height = height
            this.modifier = modifier
        }

    /**
     * A viewport of [VIEWPORT_HEIGHT] over ten rows of content, scrolled down by [scrolledRows]
     * the way [net.kernelpanicsoft.archie.gui.composables.containers.Scrollable] does it - by
     * placing the whole content column at a negative offset. Returns the row at [rowIndex].
     */
    private fun rowInViewport(rowIndex: Int, scrolledRows: Int = 0, clipped: Boolean = true): LayoutNode {
        val viewport = node(
            x = 0, y = VIEWPORT_TOP, width = GRID_WIDTH, height = VIEWPORT_HEIGHT,
            modifier = if (clipped) Modifier.clipToBounds() else Modifier,
        )
        val content = node(x = 0, y = -scrolledRows * ROW_HEIGHT, width = GRID_WIDTH, height = ROW_HEIGHT * 10)
            .also { it.parent = viewport }
        return node(x = 0, y = rowIndex * ROW_HEIGHT, width = GRID_WIDTH, height = ROW_HEIGHT)
            .also { it.parent = content }
    }

    @Test
    fun testRowInsideTheViewportIsHitTested() {
        val firstRow = rowInViewport(rowIndex = 0)
        assertTrue(firstRow.isBounded(10, VIEWPORT_TOP + 5)) {
            "A row within the viewport must still take clicks"
        }
    }

    @Test
    fun testRowOverflowingBelowTheViewportIsNotHitTested() {
        // Row 5 sits 90px into the content, i.e. well past the 54px viewport - invisible, and
        // physically over whatever the layout put underneath the scroller.
        val hiddenRow = rowInViewport(rowIndex = 5)
        assertFalse(hiddenRow.isBounded(10, VIEWPORT_TOP + 95)) {
            "A row scissored out of the viewport must not take clicks meant for what is under it"
        }
    }

    /** The same geometry with no clipping ancestor still hit-tests - the exclusion comes from the clip, not the coordinates. */
    @Test
    fun testAnUnclippedContainerStillHitTestsItsOverflow() {
        val overflowingRow = rowInViewport(rowIndex = 5, clipped = false)
        assertTrue(overflowingRow.isBounded(10, VIEWPORT_TOP + 95)) {
            "Without a clipping ancestor a node is hit-tested on its own bounds as before"
        }
    }

    /** Scrolling a row up into view makes it hit-testable, and pushes another out. */
    @Test
    fun testScrollingMovesWhichRowsAreHitTestable() {
        val scrolledIntoView = rowInViewport(rowIndex = 4, scrolledRows = 3)
        assertTrue(scrolledIntoView.isBounded(10, VIEWPORT_TOP + 20)) {
            "Row 4 scrolled to the second visible row must take clicks"
        }

        val scrolledOutAbove = rowInViewport(rowIndex = 0, scrolledRows = 3)
        assertFalse(scrolledOutAbove.isBounded(10, VIEWPORT_TOP - 5)) {
            "Row 0 scrolled above the viewport must not take clicks over whatever is up there"
        }
    }

    /** Every clipping ancestor counts, not just the nearest one. */
    @Test
    fun testAnOuterClipAlsoExcludes() {
        val outer = node(x = 0, y = 0, width = 100, height = 40, modifier = Modifier.clipToBounds())
        val inner = node(x = 0, y = 0, width = 100, height = 100, modifier = Modifier.clipToBounds())
            .also { it.parent = outer }
        val child = node(x = 0, y = 60, width = 100, height = 18).also { it.parent = inner }

        assertFalse(child.isBounded(10, 65)) {
            "A node inside its nearest clip but outside an outer one must not be hit-tested"
        }
    }
}
