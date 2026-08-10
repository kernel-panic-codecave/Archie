package net.kernelpanicsoft.archie.gui.access

import net.kernelpanicsoft.archie.gui.layout.IntRect

/**
 * Allows container screens to clip vanilla's static `renderSlotHighlight(GuiGraphics, x, y,
 * blitOffset)` call to a sub-rect of the slot's own 16x16 bounds. Returning `null` skips the
 * highlight draw entirely (fully clipped away); returning the full unclipped rect renders it
 * normally.
 */
interface SlotHighlightClipProvider {
    /**
     * Returns the sub-rect (in slot-local pixel coordinates) to clip the slot highlight to at
     * screen position [x], [y], or `null` to skip the highlight entirely.
     */
    fun slotHighlightClipRect(x: Int, y: Int): IntRect?
}
