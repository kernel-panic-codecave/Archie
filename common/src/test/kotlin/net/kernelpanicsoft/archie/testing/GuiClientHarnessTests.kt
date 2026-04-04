package net.kernelpanicsoft.archie.testing

import net.kernelpanicsoft.archie.gui.composables.containers.resolveScrollableContentAxis
import net.kernelpanicsoft.archie.gui.composables.containers.resolveScrollableViewportAxis
import net.kernelpanicsoft.archie.gui.composables.input.normalizeSliderValue
import net.kernelpanicsoft.archie.gui.composables.input.resolveSliderThumbX
import net.kernelpanicsoft.archie.gui.composables.input.resolveSwitchThumbOffset
import net.kernelpanicsoft.archie.gui.composables.input.snapSliderValue
import net.kernelpanicsoft.archie.gui.layout.IntCoordinates
import net.kernelpanicsoft.archie.gui.layout.IntRect
import net.kernelpanicsoft.archie.gui.layout.Size
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class GuiClientHarnessTests {
    @Test
    fun testSliderNormalization() {
        assertEquals(0f, normalizeSliderValue(-0.1f))
        assertEquals(0.5f, normalizeSliderValue(0.5f))
        assertEquals(1f, normalizeSliderValue(1.5f))
    }

    @Test
    fun testSliderStepSnapping() {
        assertEquals(0f, snapSliderValue(0.1f, steps = 4))
        assertEquals(0.5f, snapSliderValue(0.49f, steps = 4))
        assertEquals(1f, snapSliderValue(0.99f, steps = 4))
    }

    @Test
    fun testScrollableAxisResolution() {
        assertEquals(140, resolveScrollableViewportAxis(childSize = 24, min = 0, max = 140))
        assertEquals(24, resolveScrollableViewportAxis(childSize = 24, min = 0, max = Int.MAX_VALUE))
        assertEquals(24, resolveScrollableContentAxis(childSize = 24, min = 0, max = 140))
    }

    @Test
    fun testClipRectIntersection() {
        val a = IntRect.fromPositionAndSize(IntCoordinates(10, 10), Size(30, 20))
        val b = IntRect.fromPositionAndSize(IntCoordinates(25, 20), Size(20, 20))
        val intersection = a.intersect(b)

        assertNotNull(intersection)
        assertEquals(IntRect(25, 20, 40, 30), intersection)
    }

    @Test
    fun testSwitchThumbOffsetNarrowTrack() {
        assertEquals(2, resolveSwitchThumbOffset(thumbOffset = 30, trackWidth = 0))
        assertEquals(2, resolveSwitchThumbOffset(thumbOffset = 30, trackWidth = 12))
        assertEquals(18, resolveSwitchThumbOffset(thumbOffset = 18, trackWidth = 34))
    }

    @Test
    fun testSliderThumbXNarrowWidth() {
        assertEquals(15, resolveSliderThumbX(rawThumbX = 40, sliderX = 15, sliderWidth = 0))
        assertEquals(15, resolveSliderThumbX(rawThumbX = -5, sliderX = 15, sliderWidth = 6))
        assertEquals(22, resolveSliderThumbX(rawThumbX = 22, sliderX = 15, sliderWidth = 15))
    }
}

