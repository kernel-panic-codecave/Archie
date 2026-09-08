package net.kernelpanicsoft.archie.gui.access

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.world.item.ItemStack

/**
 * Lets a screen hold back the two overlays vanilla's `AbstractContainerScreen.render` draws inline -
 * the hovered slot's highlight, and the carried/dragging item that follows the cursor - so it can
 * draw them itself, later.
 *
 * Both are drawn *inside* the slot loop's pose, which is to say before anything a
 * [net.kernelpanicsoft.archie.gui.ComposeContainerScreen] renders on top of that loop. Their Z is
 * not the problem and cannot be the fix: by the time layers are drawn, depth testing is off (see
 * `AbstractContainerScreenDepthMixin`) and submission order decides, so an open modal paints over
 * both no matter what Z they were given. The highlight of a slot the modal itself owns - and the
 * stack the player is carrying, which has to stay visible over everything - therefore have to be
 * *re-ordered*, not re-numbered.
 *
 * So the mixins ask [defersScreenOverlays] before drawing either. A screen that answers `true`
 * takes on the obligation to draw them: see [DeferredSlotHighlights] and [DeferredFloatingItems],
 * the replay halves, each implemented by the mixin that captured the call.
 */
interface ScreenOverlayDeferral {
    /** Whether this screen will draw the held-back overlays itself. Asked once per draw of each. */
    fun defersScreenOverlays(): Boolean
}

/**
 * The replay half of [ScreenOverlayDeferral] for the hovered-slot highlight, implemented by each
 * loader's own highlight mixin - vanilla and NeoForge call different overloads at that call site,
 * so only the mixin that captured it knows how to draw it again.
 */
interface DeferredSlotHighlights {
    /** Draws (and discards) whatever highlight was held back this frame. */
    fun archieDrawDeferredSlotHighlights(guiGraphics: GuiGraphics)
}

/** The replay half of [ScreenOverlayDeferral] for the carried, dragged and snapping-back items. */
interface DeferredFloatingItems {
    /** Draws (and discards) whatever floating items were held back this frame. */
    fun archieDrawDeferredFloatingItems(guiGraphics: GuiGraphics)
}

/**
 * One held-back `renderFloatingItem` call - the carried stack, a split-drag stack, or an item
 * snapping back to where it came from.
 */
data class FloatingItemDraw(val stack: ItemStack, val x: Int, val y: Int, val text: String?)
