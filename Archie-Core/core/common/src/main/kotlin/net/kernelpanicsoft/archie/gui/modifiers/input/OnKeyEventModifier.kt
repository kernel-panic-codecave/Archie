package net.kernelpanicsoft.archie.gui.modifiers.input

import net.kernelpanicsoft.archie.gui.nodes.UINode
import net.kernelpanicsoft.archie.gui.modifiers.Modifier

/**
 * A [Modifier.Element] that registers a keyboard key-press handler on a composable node.
 *
 * Multiple [OnKeyEventModifier] elements on the same node are **chained**: the earlier
 * handler fires first, and the later handler fires only if the event was not yet consumed.
 *
 * @property onEvent The callback invoked with (node, [KeyEvent]) on a key press.
 */
data class OnKeyEventModifier(
	val onEvent: (UINode, KeyEvent) -> Unit,
) : Modifier.Element<OnKeyEventModifier> {

    override fun mergeWith(other: OnKeyEventModifier): OnKeyEventModifier =
        OnKeyEventModifier { node, event ->
            onEvent(node, event)
            if (!event.isConsumed) other.onEvent(node, event)
        }

    override fun toString(): String = "OnKeyEventModifier()"
}

/**
 * Registers a key-press handler on this composable.
 *
 * The handler is called when a keyboard key is pressed while the node (or a descendant)
 * holds focus in the key event dispatch chain.
 *
 * @param onEvent Callback invoked with (node, [KeyEvent]).
 */
fun Modifier.onKeyEvent(onEvent: (UINode, KeyEvent) -> Unit): Modifier =
    this then OnKeyEventModifier(onEvent)
