package net.kernelpanicsoft.archie.testing

import net.kernelpanicsoft.archie.gui.layout.Alignment
import net.kernelpanicsoft.archie.gui.layout.IntCoordinates
import net.kernelpanicsoft.archie.gui.layout.IntSize
import net.kernelpanicsoft.archie.gui.layout.LayoutDirection
import net.kernelpanicsoft.archie.gui.layout.offset as layoutOffset
import net.kernelpanicsoft.archie.gui.layout.pos
import net.kernelpanicsoft.archie.gui.layout.size
import net.kernelpanicsoft.archie.gui.modifiers.Constraints
import net.kernelpanicsoft.archie.gui.modifiers.offset
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LayoutCoreTests {
    @Test
    fun testIntCoordinatesMath() {
        val a = IntCoordinates(20, 6)
        val b = IntCoordinates(5, 4)
        assertEquals(IntCoordinates(25, 10), a + b)
        assertEquals(IntCoordinates(15, 2), a - b)
    }

    @Test
    fun testAliasesConstructExpectedCoordinates() {
        assertEquals(IntCoordinates(2, 3), pos(2, 3))
        assertEquals(IntCoordinates(7, 9), layoutOffset(7, 9))
        assertEquals(IntSize(10, 11), size(10, 11))
    }

    @Test
    fun testConstraintsCopyNormalizesBounds() {
        val c = Constraints(minWidth = 90, maxWidth = 10, minHeight = 50, maxHeight = 12)
        val normalized = c.copy()
        assertEquals(10, normalized.minWidth)
        assertEquals(90, normalized.maxWidth)
        assertEquals(12, normalized.minHeight)
        assertEquals(50, normalized.maxHeight)
    }

    @Test
    fun testConstraintsOffsetKeepsMaxUnbounded() {
        val c = Constraints(maxWidth = Int.MAX_VALUE, maxHeight = Int.MAX_VALUE)
        val shifted = c.offset(horizontal = -20, vertical = -30)
        assertEquals(Int.MAX_VALUE, shifted.maxWidth)
        assertEquals(Int.MAX_VALUE, shifted.maxHeight)
    }

    @Test
    fun testConstraintsOffsetCoercesAtZero() {
        val c = Constraints(minWidth = 4, maxWidth = 8, minHeight = 3, maxHeight = 9)
        val shifted = c.offset(horizontal = -20, vertical = -20)
        assertEquals(0, shifted.minWidth)
        assertEquals(0, shifted.maxWidth)
        assertEquals(0, shifted.minHeight)
        assertEquals(0, shifted.maxHeight)
    }

    @Test
    fun testAlignmentStartEndByDirection() {
        val child = IntSize(20, 20)
        val space = IntSize(100, 100)
        assertEquals(IntCoordinates(0, 0), Alignment.TopStart.align(child, space, LayoutDirection.Ltr))
        assertEquals(IntCoordinates(80, 0), Alignment.TopStart.align(child, space, LayoutDirection.Rtl))
        assertEquals(IntCoordinates(40, 40), Alignment.Center.align(child, space, LayoutDirection.Ltr))
    }
}

