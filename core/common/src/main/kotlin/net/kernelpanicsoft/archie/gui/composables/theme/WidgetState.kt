package net.kernelpanicsoft.archie.gui.composables.theme

import net.kernelpanicsoft.archie.gui.theme.ComposableTheme

/**
 * Resolves a stateful composable's [TextureStates] key from an ordered set of independent
 * boolean state axes (hovered, checked, pressed, ...), replacing the hand-written `when` chain
 * every stateful composable (`Checkbox`, `Radio`, `Switch`, `Button`, `Slider`, `Tab`) used to
 * maintain separately - which had drifted out of sync with each other (e.g. `Tab`'s combined
 * hover state only fired via its `selected` axis, not its `pressed` axis, unlike every other
 * component - see [resolve]'s "Tab" note).
 *
 * ### Example
 * ```kotlin
 * val stateKey = WidgetState.resolve(
 *     composableTheme, variant,
 *     WidgetState.clicked(checked), WidgetState.focused(hovered || vanillaFocused),
 *     enabled = enabled,
 * )
 * node.renderState = stateKey
 * guiGraphics.drawThemeState(composableTheme.getState(stateKey, variant), x, y, node.width, node.height)
 * ```
 */
object WidgetState {
    /**
     * One named boolean axis of a widget's interaction state (e.g. "focused" paired with
     * whether the widget currently is), in the priority order [resolve] should consider it -
     * pass axes to [resolve] most-significant first (typically the "activated" axis - checked/
     * selected/pressed - before "focused").
     */
    data class Axis(val name: String, val active: Boolean)

    /**
     * An [Axis] for [TextureStates.HOVERED] - whether the widget is currently highlighted,
     * meaning either the mouse is hovering it *or* it holds vanilla keyboard/controller focus
     * (see `Modifier.focusable`). Both drive the same texture state: there's one "this is the
     * thing about to be interacted with" visual regardless of which input method put it there,
     * so callers that support both pass a single merged boolean (e.g. `isHovered || isFocused`)
     * rather than two independent axes.
     */
    fun focused(active: Boolean) = Axis(TextureStates.HOVERED, active)

    /** An [Axis] for [TextureStates.CLICKED] - a checkbox/switch/radio's checked-or-selected state, a button/tab's pressed-or-selected state, or a slider's dragging state. */
    fun clicked(active: Boolean) = Axis(TextureStates.CLICKED, active)

    /**
     * Resolves the [TextureStates] key for [theme]/[variant] given [enabled] and [axes] (in
     * descending priority order - see [Axis]).
     *
     * - If [enabled] is `false`, returns [TextureStates.DISABLED] if [theme] defines it for
     *   [variant] (via [ComposableTheme.hasState]), otherwise falls through as if disabled
     *   weren't a factor - matching every existing chain's behavior of only branching on
     *   `!enabled` where a `disabled` theme state actually exists to show.
     * - Otherwise, tries the most specific composite key first: every currently-active axis's
     *   [Axis.name], joined by `"_and_"` in priority order (e.g. `"clicked_and_hovered"` for
     *   [clicked]+[focused] both active). If [theme] doesn't define that combination, falls
     *   back one axis at a time - by priority, i.e. trying each individual active axis's own
     *   key alone, highest priority first - stopping at the first one [theme] defines.
     * - Returns [TextureStates.DEFAULT] if no active axis (alone or combined) has a defined
     *   state, or if no axis is active at all.
     *
     * This graceful per-axis fallback (rather than jumping straight from the full composite to
     * [TextureStates.DEFAULT]) generalizes what `Button`'s chain alone used to do by hand
     * (falling through a missing "clicked" state to "hovered" - `button.json` defines no
     * "clicked" state at all) - every caller gets it for free, without needing its own
     * `hasState` check.
     *
     * **Tab note:** `TabContainer.kt`'s old chain computed its "clicked" axis from
     * `selected || isPressed`, but only paired it with `hovered` into the combined state when
     * specifically `selected` was true - a pressed-but-unselected-and-hovered tab silently lost
     * its hover visual. Callers migrating to this resolver should pass a single `clicked` axis
     * (`selected || isPressed`) and a separate `focused` axis as normal; [resolve] then treats
     * both uniformly like every other component, which is a deliberate behavior fix, not an
     * incidental one.
     */
    fun resolve(theme: ComposableTheme, variant: String, vararg axes: Axis, enabled: Boolean = true): String {
        if (!enabled && theme.hasState(TextureStates.DISABLED, variant)) return TextureStates.DISABLED

        val active = axes.filter { it.active }
        if (active.isEmpty()) return TextureStates.DEFAULT

        val compositeKey = active.joinToString("_and_") { it.name }
        if (theme.hasState(compositeKey, variant)) return compositeKey

        active.forEach { axis ->
            if (theme.hasState(axis.name, variant)) return axis.name
        }

        return TextureStates.DEFAULT
    }
}
