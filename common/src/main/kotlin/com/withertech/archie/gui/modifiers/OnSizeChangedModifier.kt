package com.withertech.archie.gui.modifiers

import com.withertech.archie.gui.layout.Size

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
fun Modifier.onSizeChanged(onSizeChanged: (Size) -> Unit) = then(
	OnSizeChangedModifier(onSizeChanged = onSizeChanged)
)