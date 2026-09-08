package net.kernelpanicsoft.archie.testing

import net.kernelpanicsoft.archie.gui.composables.containers.tableExtent
import net.kernelpanicsoft.archie.gui.composables.containers.tableTrackSizes
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * `Table`'s column sizing - the part that makes it a table rather than a stack of rows.
 *
 * A track is measured against every cell crossing it before anything is placed, which is what lets
 * a column of numbers share an edge however many digits each row has.
 */
class TableLayoutTests {

    /** Cell widths as [row][column]. */
    private val widths = arrayOf(
        intArrayOf(20, 60, 30),
        intArrayOf(8, 90, 30),
        intArrayOf(14, 40, 30),
    )

    private fun widthOf(row: Int, column: Int) = widths[row][column]

    @Test
    fun testEachColumnTakesItsWidestCell() {
        val columns = tableTrackSizes(trackCount = 3, crossCount = 3, fixedSize = { null }, sizeOf = ::widthOf)
        assertArrayEquals(intArrayOf(20, 90, 30), columns) {
            "Every column must be as wide as its widest cell, not its first or last"
        }
    }

    @Test
    fun testAFixedColumnIgnoresItsContent() {
        val columns = tableTrackSizes(trackCount = 3, crossCount = 3, fixedSize = { if (it == 1) 50 else null }, sizeOf = ::widthOf)
        assertArrayEquals(intArrayOf(20, 50, 30), columns) {
            "A pinned column keeps its width even when a cell is wider"
        }
    }

    /** Rows use the same function with the axes swapped, so a tall cell sets its whole row's height. */
    @Test
    fun testRowHeightsUseTheSameSizing() {
        val heights = arrayOf(
            intArrayOf(8, 8, 8),
            intArrayOf(8, 18, 8),
            intArrayOf(8, 8, 8),
        )
        val rows = tableTrackSizes(trackCount = 3, crossCount = 3, fixedSize = { null }) { column, row -> heights[row][column] }
        assertArrayEquals(intArrayOf(8, 18, 8), rows) {
            "A single tall cell must set its row's height without affecting the others"
        }
    }

    @Test
    fun testExtentCountsGapsBetweenTracksOnly() {
        assertEquals(20 + 90 + 30 + 6 * 2, tableExtent(intArrayOf(20, 90, 30), spacing = 6)) {
            "Three tracks have two gaps between them, and no trailing gap"
        }
        assertEquals(40, tableExtent(intArrayOf(40), spacing = 6)) { "A lone track has no gap at all" }
        assertEquals(0, tableExtent(intArrayOf(), spacing = 6)) { "An empty table takes no room" }
    }

    /** A table with no rows still has to produce a width per column rather than failing. */
    @Test
    fun testAnEmptyTableSizesToNothing() {
        val columns = tableTrackSizes(trackCount = 2, crossCount = 0, fixedSize = { null }, sizeOf = ::widthOf)
        assertArrayEquals(intArrayOf(0, 0), columns)
    }
}
