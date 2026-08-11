package net.kernelpanicsoft.archie.gui.modifiers.input

import net.kernelpanicsoft.archie.gui.nodes.UINode
import net.kernelpanicsoft.archie.gui.modifiers.Modifier

/**
 * A [Modifier.Element] that registers a character-typed handler on a composable node.
 *
 * Multiple [OnCharTypedModifier] elements on the same node are **chained**: earlier handlers
 * fire first, and later handlers fire only if the event was not yet consumed.
 *
 * @property onEvent The callback invoked with (node, [CharEvent]) when a character is typed.
 */
data class OnCharTypedModifier(
	val onEvent: (UINode, CharEvent) -> Unit,
) : Modifier.Element<OnCharTypedModifier> {

    override fun mergeWith(other: OnCharTypedModifier): OnCharTypedModifier =
        OnCharTypedModifier { node, event ->
            onEvent(node, event)
            if (!event.isConsumed) other.onEvent(node, event)
        }
}

/**
 * Registers a character-typed handler on this composable.
 *
 * Called when the user types a printable character while the node (or a descendant)
 * participates in key event dispatch.
 *
 * @param onEvent Callback invoked with (node, [CharEvent]).
 */
fun Modifier.onCharTyped(onEvent: (UINode, CharEvent) -> Unit): Modifier =
    this then OnCharTypedModifier(onEvent)
