package net.kernelpanicsoft.archie.testing

import net.kernelpanicsoft.archie.gui.composables.input.normalizeSliderValue
import net.kernelpanicsoft.archie.gui.composables.input.resolveSliderThumbX
import net.kernelpanicsoft.archie.gui.composables.input.resolveSwitchThumbOffset
import net.kernelpanicsoft.archie.gui.composables.input.snapSliderValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class InputPrimitiveTests {
    @Test
    fun testSliderNormalizeClamps() {
        assertEquals(0f, normalizeSliderValue(-1f))
        assertEquals(0.25f, normalizeSliderValue(0.25f))
        assertEquals(1f, normalizeSliderValue(2f))
    }

    @Test
    fun testSliderSnapRespectsSteps() {
        assertEquals(0.5f, snapSliderValue(0.49f, steps = 4))
        assertEquals(0.75f, snapSliderValue(0.74f, steps = 4))
        assertEquals(1f, snapSliderValue(1.4f, steps = 4))
    }

    @Test
    fun testSwitchThumbOffsetHandlesNarrowTrack() {
        assertEquals(2, resolveSwitchThumbOffset(thumbOffset = 10, trackWidth = 0))
        assertEquals(2, resolveSwitchThumbOffset(thumbOffset = 10, trackWidth = 10))
        assertEquals(2, resolveSwitchThumbOffset(thumbOffset = -5, trackWidth = 18))
        assertEquals(4, resolveSwitchThumbOffset(thumbOffset = 4, trackWidth = 20))
    }

    @Test
    fun testSliderThumbXHandlesNarrowWidth() {
        assertEquals(15, resolveSliderThumbX(rawThumbX = 20, sliderX = 15, sliderWidth = 0))
        assertEquals(15, resolveSliderThumbX(rawThumbX = -10, sliderX = 15, sliderWidth = 2))
        assertEquals(19, resolveSliderThumbX(rawThumbX = 19, sliderX = 15, sliderWidth = 12))
    }
}

