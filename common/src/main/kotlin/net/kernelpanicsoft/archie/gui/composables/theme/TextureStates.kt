package net.kernelpanicsoft.archie.gui.composables.theme

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

    /** Used when the mouse cursor is hovering over the composable. */
    const val HOVERED           = "hovered"

    /** Used when the composable has been activated/checked/clicked (toggle state). */
    const val CLICKED           = "clicked"

    /** Used when the composable is both activated and hovered simultaneously. */
    const val CLICKED_AND_HOVERED = "clicked_and_hovered"
}
