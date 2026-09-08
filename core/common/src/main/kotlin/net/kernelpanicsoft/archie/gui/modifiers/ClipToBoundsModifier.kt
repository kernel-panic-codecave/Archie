package net.kernelpanicsoft.archie.gui.modifiers

/**
 * A [Modifier.Element] marking a node as clipping its subtree for *input* as well as for
 * rendering: a descendant is hit-testable only where it overlaps this node's own bounds.
 *
 * Every clipping container (see [net.kernelpanicsoft.archie.gui.composables.containers.Scrollable],
 * `Collapsible`, `PannableCanvas`) draws its children inside a scissor, but a scissor hides
 * pixels and nothing more - the children keep real absolute coordinates outside the viewport.
 * A scrolled-off row therefore still sits wherever the scroll offset put it, usually on top of
 * whatever the layout placed *below* the container, and hit-testing a node against its own
 * bounds alone (see [net.kernelpanicsoft.archie.gui.layout.LayoutNode.isBounded]) hands it clicks
 * and hover the user has no way to see coming. Because a press consumes the event, it does not
 * merely fire in addition to the real target - it takes the click away from it.
 *
 * Applied by the clipping containers themselves; there is no reason for a leaf to carry it.
 * Multiple elements on one node merge to a single, idempotent clip - the node's own bounds are
 * the clip either way.
 */
class ClipToBoundsModifier : Modifier.Element<ClipToBoundsModifier> {
	override fun mergeWith(other: ClipToBoundsModifier) = this
}

/** Restricts hit-testing of this node's descendants to its own bounds - see [ClipToBoundsModifier]. */
fun Modifier.clipToBounds() = this then ClipToBoundsModifier()
