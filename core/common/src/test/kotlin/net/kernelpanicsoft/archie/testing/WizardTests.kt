package net.kernelpanicsoft.archie.testing

import net.kernelpanicsoft.archie.gui.composables.modal.WizardState
import net.kernelpanicsoft.archie.gui.composables.modal.WizardTransition
import net.kernelpanicsoft.archie.gui.composables.modal.wizardSlideOffsets
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Navigation and slide geometry for `Wizard`, both of which are easy to get off by one or backwards. */
class WizardTests {

    private fun state(initialIndex: Int = 0) = WizardState(initialIndex)

    @Test
    fun testNextAndBackWalkThePages() {
        val wizard = state()
        assertEquals(0, wizard.currentIndex)
        assertFalse(wizard.canGoBack) { "The first page has nothing behind it" }

        wizard.next(3)
        assertEquals(1, wizard.currentIndex)
        assertTrue(wizard.canGoBack)

        wizard.back()
        assertEquals(0, wizard.currentIndex)
    }

    @Test
    fun testNavigationStopsAtBothEnds() {
        val wizard = state()
        wizard.back()
        assertEquals(0, wizard.currentIndex) { "Back on the first page must not run off the front" }

        repeat(5) { wizard.next(3) }
        assertEquals(2, wizard.currentIndex) { "Next past the last page must stop there" }
        assertTrue(wizard.isLastPage(3))
    }

    @Test
    fun testDirectionFollowsTheMove() {
        val wizard = state()
        wizard.next(3)
        assertEquals(1, wizard.direction) { "Moving forward must slide forward" }

        wizard.back()
        assertEquals(-1, wizard.direction) { "Moving back must slide back" }

        wizard.moveTo(2)
        assertEquals(1, wizard.direction) { "A jump forward is still a forward slide" }
    }

    /** A move to the page already showing is not a transition and must not restart one. */
    @Test
    fun testMovingToTheCurrentPageIsInert() {
        val wizard = state()
        wizard.next(3)
        wizard.back()
        assertEquals(-1, wizard.direction)

        wizard.moveTo(0)
        assertEquals(0, wizard.currentIndex)
        assertEquals(-1, wizard.direction) { "A no-op move must leave the slide direction alone" }
    }

    /** A page list that shrinks under the wizard must not leave the index off the end. */
    @Test
    fun testIndexIsClampedWhenPagesDisappear() {
        val wizard = state()
        wizard.moveTo(4)
        assertEquals(4, wizard.currentIndex)

        wizard.ensureRange(2)
        assertEquals(1, wizard.currentIndex) { "An index past the last page must clamp to it" }

        wizard.ensureRange(0)
        assertEquals(1, wizard.currentIndex) { "With no pages at all there is nothing to clamp to" }
    }

    @Test
    fun testResetReturnsToTheFirstPage() {
        val wizard = state()
        wizard.moveTo(3)
        wizard.reset()
        assertEquals(0, wizard.currentIndex)
        assertEquals(-1, wizard.direction) { "Reset reads as going back, not forward" }
    }

    /** Forward: the new page enters from the right, the old one leaves to the left. */
    @Test
    fun testForwardSlideBringsTheNewPageInFromTheRight() {
        val width = 162

        val (outStart, inStart) = wizardSlideOffsets(width, progress = 0f, direction = 1)
        assertEquals(0, outStart) { "At the start the outgoing page still fills the viewport" }
        assertEquals(width, inStart) { "At the start the incoming page waits one width to the right" }

        val (outEnd, inEnd) = wizardSlideOffsets(width, progress = 1f, direction = 1)
        assertEquals(-width, outEnd) { "The outgoing page ends one width to the left" }
        assertEquals(0, inEnd) { "The incoming page ends filling the viewport" }
    }

    /** Back mirrors it exactly. */
    @Test
    fun testBackSlideMirrorsForward() {
        val width = 162

        val (outStart, inStart) = wizardSlideOffsets(width, progress = 0f, direction = -1)
        assertEquals(0, outStart)
        assertEquals(-width, inStart) { "Going back, the incoming page waits to the left" }

        val (outEnd, inEnd) = wizardSlideOffsets(width, progress = 1f, direction = -1)
        assertEquals(width, outEnd) { "Going back, the outgoing page leaves to the right" }
        assertEquals(0, inEnd)
    }

    /** Mid-slide the two pages stay exactly one width apart, so no gap or overlap shows. */
    @Test
    fun testPagesStayAdjacentThroughoutTheSlide() {
        val width = 162
        for (direction in listOf(1, -1)) {
            for (step in 0..10) {
                val progress = step / 10f
                val (outgoing, incoming) = wizardSlideOffsets(width, progress, direction)
                assertEquals(width, kotlin.math.abs(incoming - outgoing)) {
                    "At progress $progress direction $direction the pages were ${kotlin.math.abs(incoming - outgoing)}px apart, not $width"
                }
            }
        }
    }

    /**
     * The composition that starts a transition must keep the outgoing page.
     *
     * This is the regression that made the slide do nothing at all: the animation's progress still
     * reads as finished on that pass, and clearing the outgoing page from it left a single composed
     * page with nothing to slide against.
     */
    @Test
    fun testTheOutgoingPageSurvivesTheFirstPass() {
        val transition = WizardTransition(initialIndex = 0)
        val pages = 0..2

        // Settled on page 0: no transition, nothing outgoing.
        assertEquals(1f, transition.advance(currentIndex = 0, pageIndices = pages, rawProgress = 1f))
        assertNull(transition.outgoingIndex)

        // The page changed, but the animation has not restarted yet and still reports 1f.
        val startProgress = transition.advance(currentIndex = 1, pageIndices = pages, rawProgress = 1f)
        assertEquals(0, transition.outgoingIndex) { "The page being left must stay composed to slide against" }
        assertEquals(0f, startProgress) { "A stale 1f must be drawn as the start of the slide, not its end" }
    }

    @Test
    fun testProgressFollowsTheAnimationOnceItRestarts() {
        val transition = WizardTransition(initialIndex = 0)
        val pages = 0..2

        transition.advance(0, pages, 1f)
        transition.advance(1, pages, 1f)

        assertEquals(0.25f, transition.advance(1, pages, 0.25f))
        assertEquals(0, transition.outgoingIndex) { "Mid-slide both pages stay composed" }

        assertEquals(0.8f, transition.advance(1, pages, 0.8f))
        assertEquals(0, transition.outgoingIndex)
    }

    @Test
    fun testTheOutgoingPageIsDroppedOnceTheSlideFinishes() {
        val transition = WizardTransition(initialIndex = 0)
        val pages = 0..2

        transition.advance(0, pages, 1f)
        transition.advance(1, pages, 1f)
        transition.advance(1, pages, 0.5f)
        transition.advance(1, pages, 1f)

        assertNull(transition.outgoingIndex) { "A settled wizard must compose one page only" }
    }

    /** A page change part-way through another one restarts against whatever was showing. */
    @Test
    fun testAChangeMidSlideRetargetsTheTransition() {
        val transition = WizardTransition(initialIndex = 0)
        val pages = 0..2

        transition.advance(0, pages, 1f)
        transition.advance(1, pages, 1f)
        transition.advance(1, pages, 0.4f)

        val progress = transition.advance(2, pages, 0.4f)
        assertEquals(1, transition.outgoingIndex) { "The page half-way in becomes the one being left" }
        assertEquals(0f, progress) { "The new slide starts from the beginning" }
    }

    /** A page that no longer exists cannot be composed to slide against. */
    @Test
    fun testAnOutgoingPageOutsideTheListIsDropped() {
        val transition = WizardTransition(initialIndex = 5)
        transition.advance(currentIndex = 0, pageIndices = 0..1, rawProgress = 1f)
        assertNull(transition.outgoingIndex) { "Page 5 is gone from a two-page wizard and must not be composed" }
    }
}
