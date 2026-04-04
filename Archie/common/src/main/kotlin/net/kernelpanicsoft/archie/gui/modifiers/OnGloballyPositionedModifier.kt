package net.kernelpanicsoft.archie.gui.modifiers

import net.kernelpanicsoft.archie.gui.layout.IntCoordinates

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

fun Modifier.onGloballyPositioned(onGloballyPositioned: (IntCoordinates) -> Unit) = this then OnGloballyPositionedModifier(onGloballyPositioned = onGloballyPositioned)
