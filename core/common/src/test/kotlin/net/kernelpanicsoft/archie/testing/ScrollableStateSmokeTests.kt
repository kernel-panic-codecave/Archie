package net.kernelpanicsoft.archie.testing

import net.kernelpanicsoft.archie.gui.composables.containers.ScrollableState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ScrollableStateSmokeTests {
    @Test
    fun testScrollByClampsToRange() {
        val state = ScrollableState().apply { maxScroll = 100 }

        state.scrollBy(250.0)
        assertEquals(100.0, state.scrollOffset)

        state.scrollBy(-500.0)
        assertEquals(0.0, state.scrollOffset)
    }

    @Test
    fun testScrollByUpdatesInteractionTimestamp() {
        val state = ScrollableState().apply { maxScroll = 100 }
        val before = state.lastInteractTime

        state.scrollBy(1.0)

        assertTrue(state.lastInteractTime >= before) {
            "Expected interaction timestamp to increase after scrollBy"
        }
    }
}

