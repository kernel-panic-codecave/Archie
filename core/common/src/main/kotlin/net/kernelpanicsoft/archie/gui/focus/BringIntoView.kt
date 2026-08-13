package net.kernelpanicsoft.archie.gui.focus

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import net.kernelpanicsoft.archie.gui.layout.LayoutNode

/**
 * Adjusts scroll position (or otherwise) so a focused descendant's bounds become visible - the
 * `net.kernelpanicsoft.archie` equivalent of Compose Foundation's `BringIntoViewRequester`/
 * `BringIntoViewParent` mechanism. `Scrollable` provides one automatically; `Modifier.focusable`'s
 * vanilla-focus bridge calls it whenever a descendant gains focus, so Tab-navigating to a node
 * scrolled out of view brings it back into the viewport, the same as a real browser or Android
 * view does.
 */
fun interface BringIntoViewParent {
    /** Called with a descendant [LayoutNode] that just gained focus. */
    fun bringIntoView(node: LayoutNode)
}

/** Provides the nearest ancestor `Scrollable`'s [BringIntoViewParent], if any. */
val LocalBringIntoViewParent: ProvidableCompositionLocal<BringIntoViewParent?> = compositionLocalOf { null }
