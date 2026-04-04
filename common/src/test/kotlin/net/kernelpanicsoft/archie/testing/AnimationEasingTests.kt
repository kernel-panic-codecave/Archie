package net.kernelpanicsoft.archie.testing

import net.kernelpanicsoft.archie.gui.animation.Easings
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs

class AnimationEasingTests {
    @Test
    fun testOutCubicAnchors() {
	    assertEquals(0f, Easings.OutCubic.transform(0f))
	    assertEquals(1f, Easings.OutCubic.transform(1f))
    }

    @Test
    fun testOutBackOvershoots() {
        val sample = Easings.OutBack.transform(0.9f)
	    assertTrue(sample > 1f) { "OutBack should overshoot near the end, got $sample" }
    }

    @Test
    fun testLinearMidpoint() {
        val sample = Easings.Linear.transform(0.5f)
	    assertTrue(abs(sample - 0.5f) < 0.0001f) { "Expected 0.5, got $sample" }
    }
}