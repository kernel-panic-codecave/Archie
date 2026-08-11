package net.kernelpanicsoft.archie.gui.modifiers

import net.kernelpanicsoft.archie.gui.layout.IntCoordinates

/**
 * A [Modifier.Element] that invokes [onGloballyPositioned] with the node's absolute on-screen
 * coordinates whenever it is placed by [net.kernelpanicsoft.archie.gui.layout.LayoutNode.placeAt].
 *
 * Multiple [OnGloballyPositionedModifier] elements on the same node are merged so that every
 * callback in the chain fires, in declaration order, for each placement.
 *
 * @property merged Internal flag marking whether this instance already wraps other merged
 *   callbacks; set automatically by [mergeWith], not intended to be passed by callers.
 * @property onGloballyPositioned Invoked with the node's absolute [IntCoordinates] on placement.
 */
class OnGloballyPositionedModifier(
	val merged: Boolean = false,
	val onGloballyPositioned: (IntCoordinates) -> Unit
) : Modifier.Element<OnGloballyPositionedModifier>
{
	override fun mergeWith(other: OnGloballyPositionedModifier): OnGloballyPositionedModifier = OnGloballyPositionedModifier(merged = true) { position ->
		if (!other.merged)
			onGloballyPositioned(position)
		other.onGloballyPositioned(position)
	}

}

/**
 * Registers [onGloballyPositioned] to be called with the node's absolute screen coordinates
 * every time it is placed (e.g. on layout changes).
 */
fun Modifier.onGloballyPositioned(onGloballyPositioned: (IntCoordinates) -> Unit) = this then OnGloballyPositionedModifier(onGloballyPositioned = onGloballyPositioned)
