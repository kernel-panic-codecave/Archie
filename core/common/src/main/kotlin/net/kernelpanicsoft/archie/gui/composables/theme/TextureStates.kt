package net.kernelpanicsoft.archie.gui.composables.theme
import net.kernelpanicsoft.archie.gui.theme.ComposableTheme
import net.kernelpanicsoft.archie.gui.theme.ThemeState

/**
 * Constant keys used to look up [ThemeState] entries within a [ComposableTheme]'s state map.
 *
 * Composables use these keys to select the correct texture variant based on their current
 * interactive state (e.g. hovered, pressed, disabled).
 */
object TextureStates {
    /** The default idle state used when no other state applies. */
    const val DEFAULT           = "default"

    /** Used when the composable is disabled and cannot be interacted with. */
    const val DISABLED          = "disabled"

    /**
     * Used when the composable is highlighted - the mouse cursor is hovering over it, or (see
     * [WidgetState.focused]) it holds vanilla keyboard/controller focus. Both count as the same
     * texture state: there's one "this is the thing about to be interacted with" visual,
     * regardless of which input method put it there.
     */
    const val HOVERED           = "hovered"

    /** Used when the composable has been activated/checked/clicked (toggle state). */
    const val CLICKED           = "clicked"

    /** Used when the composable is both activated and hovered simultaneously. */
    const val CLICKED_AND_HOVERED = "clicked_and_hovered"
}
