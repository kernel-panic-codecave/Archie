package net.kernelpanicsoft.archie.gui.interaction

/**
 * A discrete input interaction a component emits about itself through its
 * [MutableInteractionSource] - modeled on Jetpack Compose's `Interaction`/`InteractionSource`.
 */
sealed interface Interaction

/** Interactions describing a pointer press. */
sealed interface PressInteraction : Interaction {
    /** The pointer went down inside the component's bounds. */
    data object Press : PressInteraction

    /** The pointer was released after a [Press]. */
    data object Release : PressInteraction

    /** The press ended without a [Release] (e.g. the pointer left the component's bounds). */
    data object Cancel : PressInteraction
}

/** Interactions describing pointer hover. */
sealed interface HoverInteraction : Interaction {
    data object Enter : HoverInteraction
    data object Exit : HoverInteraction
}

/**
 * Interactions describing vanilla keyboard/controller focus - see
 * [net.kernelpanicsoft.archie.gui.modifiers.input.focusable].
 */
sealed interface FocusInteraction : Interaction {
    data object Focus : FocusInteraction
    data object Unfocus : FocusInteraction
}

/** Interactions describing a pointer drag. */
sealed interface DragInteraction : Interaction {
    data object Start : DragInteraction
    data object Stop : DragInteraction
    data object Cancel : DragInteraction
}
