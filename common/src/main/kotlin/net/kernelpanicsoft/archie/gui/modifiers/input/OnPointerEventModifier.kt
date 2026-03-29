package net.kernelpanicsoft.archie.gui.modifiers.input

import androidx.compose.runtime.Stable
import net.kernelpanicsoft.archie.gui.nodes.AUINode
import net.kernelpanicsoft.archie.gui.modifiers.Modifier

/** Identifies the type of pointer (mouse) interaction that triggers an event handler. */
enum class PointerEventType {
    /** The primary or secondary mouse button was pressed inside the node's bounds. */
    PRESS,
    /**
     * A mouse button was pressed anywhere on the screen regardless of bounds.
     * Useful for detecting clicks outside a focused element.
     */
    GLOBAL_PRESS,
    /** A mouse button was released inside the node's bounds. */
    RELEASE,
    /** A mouse button was released anywhere on the screen. */
    GLOBAL_RELEASE,
    /** The mouse cursor moved while inside the node's bounds. */
    MOVE,
    /** The mouse cursor entered the node's bounds from outside. */
    ENTER,
    /** The mouse cursor left the node's bounds. */
    EXIT,
    /** The mouse wheel was scrolled over the node. */
    SCROLL,
    /** The mouse was dragged (button held + moved) over the node. */
    DRAG,
    /** The mouse was dragged anywhere on the screen. */
    GLOBAL_DRAG,
}

/** Milliseconds a press must be held to qualify as a long-click. */
const val LONG_CLICK_THRESHOLD = 500

/** Milliseconds within which two successive presses qualify as a double-click. */
const val DOUBLE_CLICK_THRESHOLD = 300

/**
 * A [Modifier.Element] that registers a callback for a specific [PointerEventType].
 *
 * When multiple [OnPointerEventModifier] elements with the same [eventType] exist on a node,
 * the **last** one replaces earlier ones (they do not chain). Use [combinedClickable] if
 * you need multiple click behaviours on a single node.
 *
 * @param T        The concrete [AUINode] subtype the handler expects.
 * @param eventType The pointer event that triggers [onEvent].
 * @param onEvent  The handler invoked with the receiving node and the event.
 */
data class OnPointerEventModifier<T : AUINode>(
    val eventType: PointerEventType,
    val onEvent: (T, PointerEvent) -> Unit,
) : Modifier.Element<OnPointerEventModifier<*>> {
    override fun mergeWith(other: OnPointerEventModifier<*>): OnPointerEventModifier<*> = other
    override fun toString(): String = "OnPointerEventModifier(eventType=${eventType.name})"
}

/**
 * Registers a handler for the given pointer [type] on this composable.
 *
 * @param T       The expected [AUINode] subtype; use [AUINode] for the generic case.
 * @param type    The [PointerEventType] to listen for.
 * @param onEvent The callback invoked with (node, event) when the event occurs.
 */
@Stable
fun <T : AUINode> Modifier.onPointerEvent(
    type: PointerEventType,
    onEvent: (T, PointerEvent) -> Unit,
): Modifier = this then OnPointerEventModifier(type, onEvent)

/**
 * Registers a scroll event handler on this composable.
 *
 * @param global       When `true`, the handler fires for scroll events anywhere on screen.
 * @param onScrollEvent The callback invoked with (node, [ScrollEvent]).
 */
@Suppress("UNCHECKED_CAST")
@Stable
fun <T : AUINode> Modifier.onScroll(
    global: Boolean = false,
    onScrollEvent: (T, ScrollEvent) -> Unit,
): Modifier = this then OnPointerEventModifier(
    if (global) PointerEventType.GLOBAL_PRESS else PointerEventType.SCROLL,
    onScrollEvent as (T, PointerEvent) -> Unit,
)

/**
 * Registers a drag event handler on this composable.
 *
 * @param global      When `true`, the handler fires for drag events anywhere on screen.
 * @param onDragEvent The callback invoked with (node, [DragEvent]).
 */
@Suppress("UNCHECKED_CAST")
@Stable
fun <T : AUINode> Modifier.onDrag(
    global: Boolean = false,
    onDragEvent: (T, DragEvent) -> Unit,
): Modifier = this then OnPointerEventModifier(
    if (global) PointerEventType.GLOBAL_DRAG else PointerEventType.DRAG,
    onDragEvent as (T, PointerEvent) -> Unit,
)

/**
 * Adds multiple click-type handlers to a composable in a single modifier.
 *
 * At least one of the three callbacks must be non-null.
 *
 * @param onLongClick   Invoked when the node is held for more than [LONG_CLICK_THRESHOLD] ms.
 * @param onDoubleClick Invoked when two presses occur within [DOUBLE_CLICK_THRESHOLD] ms.
 * @param onClick       Invoked on a normal single click (mouse release).
 */
@Stable
fun <T : AUINode> Modifier.combinedClickable(
    onLongClick: ((T, PointerEvent) -> Unit)? = null,
    onDoubleClick: ((T, PointerEvent) -> Unit)? = null,
    onClick: ((T, PointerEvent) -> Unit)? = null,
): Modifier {
    require(onClick != null || onLongClick != null || onDoubleClick != null) {
        "You must specify at least one click handler"
    }
    var mod = this

    if (onLongClick != null) {
        var clickStart = 0L
        mod = mod
            .onPointerEvent<T>(PointerEventType.PRESS) { _, _ -> clickStart = System.currentTimeMillis() }
            .onPointerEvent<T>(PointerEventType.RELEASE) { node, event ->
                if (clickStart != 0L && (System.currentTimeMillis() - clickStart) > LONG_CLICK_THRESHOLD) {
                    clickStart = 0L
                    onLongClick(node, event)
                }
            }
    }
    if (onDoubleClick != null) {
        var clickStart = 0L
        mod = mod.onPointerEvent<T>(PointerEventType.PRESS) { node, event ->
            if (clickStart != 0L && (System.currentTimeMillis() - clickStart) < DOUBLE_CLICK_THRESHOLD) {
                clickStart = 0L
                return@onPointerEvent onDoubleClick(node, event)
            }
            clickStart = System.currentTimeMillis()
        }
    }
    if (onClick != null) mod = mod.onPointerEvent(PointerEventType.RELEASE, onClick)

    return mod
}
