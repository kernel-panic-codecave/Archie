package net.kernelpanicsoft.archie.gui.modifiers.input

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import net.kernelpanicsoft.archie.gui.focus.BringIntoViewParent
import net.kernelpanicsoft.archie.gui.focus.LocalBringIntoViewParent
import net.kernelpanicsoft.archie.gui.interaction.DragInteraction
import net.kernelpanicsoft.archie.gui.interaction.FocusInteraction
import net.kernelpanicsoft.archie.gui.interaction.HoverInteraction
import net.kernelpanicsoft.archie.gui.interaction.MutableInteractionSource
import net.kernelpanicsoft.archie.gui.interaction.PressInteraction
import net.kernelpanicsoft.archie.gui.interaction.collectIsFocusedAsState
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.nodes.UINode
import org.lwjgl.glfw.GLFW

/** GLFW key codes that activate a focused widget, mirroring vanilla `AbstractWidget` activation. */
internal val ACTIVATION_KEYS: IntArray = intArrayOf(GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER, GLFW.GLFW_KEY_SPACE)

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
 * @property focused            Backing focus state: vanilla writes to it via `setFocused`.
 * @property interactionSource  When set, [FocusInteraction.Focus]/[FocusInteraction.Unfocus] is
 *   emitted alongside every [focused] write, so callers can observe focus the same way as
 *   press/hover/drag (see [collectIsFocusedAsState][net.kernelpanicsoft.archie.gui.interaction.collectIsFocusedAsState]).
 * @property bringIntoViewParent The nearest ancestor `Scrollable`'s [BringIntoViewParent], if
 *   any - called on focus gain so Tab-navigating to a scrolled-out-of-view node brings it back
 *   into the viewport.
 */
internal data class FocusableModifier(
    val focused: MutableState<Boolean>,
    val interactionSource: MutableInteractionSource? = null,
    val bringIntoViewParent: BringIntoViewParent? = null,
) : Modifier.Element<FocusableModifier> {
    override fun mergeWith(other: FocusableModifier): FocusableModifier = other
    override fun toString(): String = "FocusableModifier(focused=${focused.value})"

    /** Writes [value] to [focused] and emits the matching [FocusInteraction], if it actually changed. */
    fun setFocused(value: Boolean) {
        if (focused.value == value) return
        focused.value = value
        interactionSource?.tryEmit(if (value) FocusInteraction.Focus else FocusInteraction.Unfocus)
    }
}

/**
 * Registers this composable as a vanilla keyboard/controller focus-navigation stop - the
 * `net.kernelpanicsoft.archie` equivalent of Compose Foundation's `Modifier.focusable`.
 *
 * Unlike Android's, this can't be backed by an internal `Modifier.Node` (this framework's
 * modifiers are plain immutable data, not stateful nodes), so the focus flag is [remember]ed
 * here instead - transparent to the caller, but it does mean this overload must be called from
 * a `@Composable` context, same as any other modifier factory that needs to hold state.
 *
 * [Clickable][net.kernelpanicsoft.archie.gui.composables.input.Clickable] already applies this
 * (plus Enter/Space activation) for any click-driven widget; reach for this directly only when
 * building an input with a different activation model (e.g. `SliderCore`, which nudges its
 * value on arrow keys instead).
 *
 * @param enabled           When `false`, this node drops out of the focus graph entirely -
 *   mirrors `AbstractWidget.active` keeping a disabled vanilla widget out of Tab order.
 * @param interactionSource Optional sink for [FocusInteraction.Focus]/[FocusInteraction.Unfocus].
 */
@Composable
fun Modifier.focusable(enabled: Boolean = true, interactionSource: MutableInteractionSource? = null): Modifier {
    val focused = remember { mutableStateOf(false) }
    val bringIntoViewParent = LocalBringIntoViewParent.current
    return if (enabled) {
        this then FocusableModifier(focused, interactionSource, bringIntoViewParent)
    } else {
        focused.value = false
        this
    }
}

/**
 * Emits [HoverInteraction] as the pointer enters/exits - matches Compose Foundation's own
 * `Modifier.hoverable(interactionSource, enabled)` signature exactly. Observe the result via
 * [net.kernelpanicsoft.archie.gui.interaction.collectIsHoveredAsState], same as Android; this
 * has no enter/exit callbacks of its own to hook into.
 */
fun Modifier.hoverable(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
): Modifier = this
    .onPointerEvent<UINode>(PointerEventType.ENTER) { _, e ->
        if (enabled) interactionSource.tryEmit(HoverInteraction.Enter)
        e.consume()
    }
    .onPointerEvent<UINode>(PointerEventType.EXIT) { _, e ->
        interactionSource.tryEmit(HoverInteraction.Exit)
        if (enabled) e.consume()
    }

/**
 * Emits [PressInteraction] and invokes [onPress] across a press - the
 * `net.kernelpanicsoft.archie` equivalent of the press-recognition half of Compose
 * Foundation's `Modifier.clickable`. Observe release via
 * [net.kernelpanicsoft.archie.gui.interaction.collectIsPressedAsState] rather than a callback -
 * [onPress] is kept only because, unlike hover, a press is inherently an action (the click
 * itself), not just state to observe.
 */
fun Modifier.pressable(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
    onPress: (UINode) -> Unit,
): Modifier = this
    .onPointerEvent<UINode>(PointerEventType.PRESS) { node, e ->
        if (!enabled) return@onPointerEvent
        interactionSource.tryEmit(PressInteraction.Press)
        onPress(node)
        e.consume(true)
    }
    .onPointerEvent<UINode>(PointerEventType.GLOBAL_RELEASE) { _, _ ->
        interactionSource.tryEmit(PressInteraction.Release)
    }

/** Which axis a [draggable] gesture tracks - the `net.kernelpanicsoft.archie` equivalent of Compose Foundation's `Orientation`. */
enum class Orientation { Horizontal, Vertical }

/**
 * Reports drag deltas to a single callback - the `net.kernelpanicsoft.archie` equivalent of
 * Compose Foundation's `DraggableState`. Create one with [rememberDraggableState] rather than
 * implementing this directly.
 */
fun interface DraggableState {
    /** Called with the drag delta, in pixels along the gesture's [Orientation], for each drag update. */
    fun dispatchRawDelta(delta: Float)
}

/** Remembers a [DraggableState] that forwards each delta to the latest [onDelta]. */
@Composable
fun rememberDraggableState(onDelta: (Float) -> Unit): DraggableState {
    val onDeltaState = rememberUpdatedState(onDelta)
    return remember { DraggableState { delta -> onDeltaState.value(delta) } }
}

/**
 * Recognizes a drag gesture along [orientation] and reports each movement as a delta to
 * [state] - the `net.kernelpanicsoft.archie` equivalent of Compose Foundation's
 * `Modifier.draggable(state, orientation, enabled, interactionSource, onDragStarted,
 * onDragStopped)` (dropping `startDragImmediately`/`reverseDirection`, which have no
 * equivalent concept here).
 *
 * Reports only a *delta* per movement, not an absolute position - a widget whose drag also
 * needs to jump to an absolute position on the initial press (e.g. `SliderCore`'s "click the
 * track to jump there") needs its own press handling for that press-time jump; [state] only
 * covers the continuous drag that follows, same as Compose Foundation's own `Slider` doesn't
 * build on plain `Modifier.draggable` either, for the same reason.
 */
fun Modifier.draggable(
    state: DraggableState,
    orientation: Orientation,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    onDragStarted: () -> Unit = {},
    onDragStopped: () -> Unit = {},
): Modifier = this
    .onPointerEvent<UINode>(PointerEventType.PRESS) { _, e ->
        if (!enabled) return@onPointerEvent
        interactionSource?.tryEmit(DragInteraction.Start)
        onDragStarted()
        e.consume(true)
    }
    .onDrag<UINode> { _, e ->
        if (!enabled) return@onDrag
        state.dispatchRawDelta((if (orientation == Orientation.Horizontal) e.dragX else e.dragY).toFloat())
        e.consume()
    }
    .onPointerEvent<UINode>(PointerEventType.GLOBAL_RELEASE) { _, _ ->
        interactionSource?.tryEmit(DragInteraction.Stop)
        onDragStopped()
    }

/**
 * Marks this composable as a togglable boolean control (checkbox/switch) - the
 * `net.kernelpanicsoft.archie` equivalent of Compose Foundation's `Modifier.toggleable`.
 *
 * Combines [focusable], [hoverable], and [pressable] into one modifier applied directly to the
 * widget's own node - calling [onValueChange] with `!value` on press or Enter/Space while
 * focused - so a toggle control doesn't need [Clickable][net.kernelpanicsoft.archie.gui.composables.input.Clickable]'s
 * extra wrapping container just to participate in focus/hover/press.
 */
@Composable
fun Modifier.toggleable(
    value: Boolean,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    onValueChange: (Boolean) -> Unit,
): Modifier {
    val isFocused by interactionSource.collectIsFocusedAsState()
    return this
        .focusable(enabled = enabled, interactionSource = interactionSource)
        .onKeyEvent { _, e ->
            if (enabled && isFocused && e.keyCode in ACTIVATION_KEYS) {
                onValueChange(!value)
                e.consume(bypassSuperCall = true)
            }
        }
        .hoverable(interactionSource, enabled = enabled)
        .pressable(interactionSource, enabled = enabled, onPress = { onValueChange(!value) })
}

/**
 * Marks this composable as a selectable option within a mutually exclusive group (radio
 * button/tab) - the `net.kernelpanicsoft.archie` equivalent of Compose Foundation's
 * `Modifier.selectable`.
 *
 * Same shape as [toggleable], but calls [onClick] unconditionally rather than toggling a
 * boolean - "clicking an already-selected option is a no-op" is the caller's own
 * responsibility (e.g. `RadioButtonCore` passing `onSelect = { if (!selected) onSelect() }`),
 * matching real Android's `selectable` the same way.
 */
@Composable
fun Modifier.selectable(
    selected: Boolean,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    onClick: () -> Unit,
): Modifier {
    val isFocused by interactionSource.collectIsFocusedAsState()
    return this
        .focusable(enabled = enabled, interactionSource = interactionSource)
        .onKeyEvent { _, e ->
            if (enabled && isFocused && e.keyCode in ACTIVATION_KEYS) {
                onClick()
                e.consume(bypassSuperCall = true)
            }
        }
        .hoverable(interactionSource, enabled = enabled)
        .pressable(interactionSource, enabled = enabled, onPress = { onClick() })
}
