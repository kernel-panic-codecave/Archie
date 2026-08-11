package net.kernelpanicsoft.archie.gui.modifiers

import net.kernelpanicsoft.archie.gui.layout.Size

/**
 * A [Modifier.Element] that invokes [onSizeChanged] whenever the node's measured [Size] changes
 * between layout passes.
 *
 * Multiple [OnSizeChangedModifier] elements on the same node are merged so that every callback
 * in the chain fires, in declaration order, for each size change.
 *
 * @property merged Internal flag marking whether this instance already wraps other merged
 *   callbacks; set automatically by [mergeWith], not intended to be passed by callers.
 * @property onSizeChanged Invoked with the node's new measured [Size].
 */
class OnSizeChangedModifier(
	val merged: Boolean = false,
	val onSizeChanged: (Size) -> Unit
) : Modifier.Element<OnSizeChangedModifier> {
	override fun mergeWith(other: OnSizeChangedModifier) = OnSizeChangedModifier(merged = true) { size ->
		if (!other.merged)
			onSizeChanged(size)
		other.onSizeChanged(size)
	}
}

/** Notifies callback of any size changes to element. */
fun Modifier.onSizeChanged(onSizeChanged: (Size) -> Unit) = this then OnSizeChangedModifier(onSizeChanged = onSizeChanged)
