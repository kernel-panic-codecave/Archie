package net.kernelpanicsoft.archie.testing

import net.kernelpanicsoft.archie.gui.modifiers.Constraints
import net.kernelpanicsoft.archie.gui.modifiers.position.MarginModifier
import net.kernelpanicsoft.archie.gui.modifiers.position.MarginValues
import net.kernelpanicsoft.archie.gui.modifiers.position.PaddingModifier
import net.kernelpanicsoft.archie.gui.modifiers.position.PaddingValues
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PaddingMarginTests {
    @Test
    fun testPaddingModifierReducesConstraints() {
        val padding = PaddingValues(left = 10, right = 10, top = 5, bottom = 5)
        val modifier = PaddingModifier(padding)

        val constraints = Constraints(
            minWidth = 100,
            maxWidth = 200,
            minHeight = 50,
            maxHeight = 100,
        )

        val modified = modifier.modifyInnerConstraints(constraints)

        assertEquals(180, modified.maxWidth)
        assertEquals(90, modified.maxHeight)
    }

    @Test
    fun testMarginModifierHorizontal() {
        val margin = MarginValues(left = 5, right = 5, top = 3, bottom = 3)
        val modifier = MarginModifier(margin)

        assertEquals(10, modifier.horizontal)
        assertEquals(6, modifier.vertical)
    }

    @Test
    fun testPaddingValuesGetOffset() {
        val padding = PaddingValues(left = 8, right = 12, top = 4, bottom = 6)
        val offset = padding.getOffset()

        assertEquals(8, offset.x)
        assertEquals(4, offset.y)
    }

    @Test
    fun testPaddingModifierNeverNegative() {
        val largePadding = PaddingValues(left = 100, right = 100, top = 100, bottom = 100)
        val modifier = PaddingModifier(largePadding)

        val constraints = Constraints(
            minWidth = 0,
            maxWidth = 50,
            minHeight = 0,
            maxHeight = 50,
        )

        val modified = modifier.modifyInnerConstraints(constraints)

        assertTrue(modified.maxWidth >= 0) { "Max width should never be negative" }
        assertTrue(modified.maxHeight >= 0) { "Max height should never be negative" }
    }

    @Test
    fun testAsymmetricPadding() {
        val padding = PaddingValues(
            left = 5,
            right = 15,
            top = 10,
            bottom = 20,
        )
        val modifier = PaddingModifier(padding)

        assertEquals(20, modifier.horizontal)
        assertEquals(30, modifier.vertical)
    }

    @Test
    fun testPaddingMerge() {
        val padding1 = PaddingValues(left = 5, top = 5, right = 0, bottom = 0)
        val padding2 = PaddingValues(left = 0, top = 0, right = 5, bottom = 5)

        val merged = padding1 + padding2

        assertEquals(5, merged.left)
        assertEquals(5, merged.right)
        assertEquals(5, merged.top)
        assertEquals(5, merged.bottom)
    }

    @Test
    fun testMarginMerge() {
        val margin1 = MarginValues(left = 2, top = 2, right = 0, bottom = 0)
        val margin2 = MarginValues(left = 0, top = 0, right = 3, bottom = 3)

        val merged = margin1 + margin2

        assertEquals(2, merged.left)
        assertEquals(3, merged.right)
        assertEquals(2, merged.top)
        assertEquals(3, merged.bottom)
    }

    @Test
    fun testPaddingReducesMinConstraints() {
        val padding = PaddingValues(left = 10, right = 10, top = 10, bottom = 10)
        val modifier = PaddingModifier(padding)

        val constraints = Constraints(
            minWidth = 50,
            maxWidth = 200,
            minHeight = 50,
            maxHeight = 200,
        )

        val modified = modifier.modifyInnerConstraints(constraints)

        assertEquals(30, modified.minWidth)
        assertEquals(30, modified.minHeight)
    }
}

