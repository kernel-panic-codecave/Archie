package net.kernelpanicsoft.archie.testing

import net.kernelpanicsoft.archie.gui.composables.containers.resolveScrollableContentAxis
import net.kernelpanicsoft.archie.gui.composables.containers.resolveScrollableViewportAxis
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ScrollableLayoutTests {
    @Test
    fun testHorizontalScrollableWrapsHeight() {
        val resolved = resolveScrollableContentAxis(childSize = 24, min = 0, max = 198)
        assertEquals(24, resolved)
    }

    @Test
    fun testScrollableViewportAxisFillsBounds() {
        val finite = resolveScrollableViewportAxis(childSize = 32, min = 0, max = 150)
        assertEquals(150, finite)

        val unbounded = resolveScrollableViewportAxis(childSize = 32, min = 0, max = Int.MAX_VALUE)
        assertEquals(32, unbounded)
    }
}

