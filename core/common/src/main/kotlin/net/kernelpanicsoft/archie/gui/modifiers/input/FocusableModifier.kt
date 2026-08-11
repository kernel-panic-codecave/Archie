package net.kernelpanicsoft.archie.gui.modifiers.input

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import net.kernelpanicsoft.archie.gui.modifiers.Modifier

/**
 * Marks a composable as a stop in vanilla Minecraft's built-in focus-navigation graph.
 *
 * A node carrying this modifier is exposed as a synthetic `GuiEventListener` leaf from
 * `ComposeScreen`/`ComposeContainerScreen`'s `children()` override (see
 * `net.kernelpanicsoft.archie.gui.focus.LayoutNodeFocusAdapter`), so vanilla's own Tab/
 * Shift-Tab and arrow-key navigation - and anything else that walks `GuiEventListener`, e.g.
 * Controlify's controller-driven `ScreenProcessor` - can reach it exactly like an ordinary
 * `AbstractWidget`.
 *
 * @property focused Backing focus state, shared with the adapter: vanilla writes to it via
 *   `setFocused`, and the owning composable reads it to render a focus indicator or gate
 *   activation (see [onKeyEvent]).
 */
data class FocusableModifier(
    val focused: MutableState<Boolean>,
) : Modifier.Element<FocusableModifier> {
    override fun mergeWith(other: FocusableModifier): FocusableModifier = other
    override fun toString(): String = "FocusableModifier(focused=${focused.value})"
}

/**
 * Registers this composable as a vanilla focus-navigation stop, backed by [focused].
 *
 * Combine with [onKeyEvent], gated on `focused.value`, to react to Enter/Space while
 * vanilla-focused - mirroring how a plain `AbstractWidget` handles activation.
 */
@Stable
fun Modifier.focusable(focused: MutableState<Boolean>): Modifier = this then FocusableModifier(focused)
