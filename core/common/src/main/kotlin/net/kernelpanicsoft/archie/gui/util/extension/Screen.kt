package net.kernelpanicsoft.archie.gui.util.extension

import net.kernelpanicsoft.archie.gui.layout.LayoutNode
import net.kernelpanicsoft.archie.gui.modifiers.input.*
import net.kernelpanicsoft.archie.gui.nodes.UINode
import net.minecraft.client.gui.screens.Screen

// ─────────────────────────────────────────────────────────────────────────────
// Generic traversal
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Recursively dispatches [event] through the [LayoutNode] tree starting at [node].
 *
 * Children are processed in reverse z-index order (highest z first) so that the
 * topmost visible node receives the event first. Propagation stops as soon as
 * [InputEvent.isConsumed] becomes `true`.
 *
 * @param node      The root of the subtree to traverse.
 * @param event     The [InputEvent] being dispatched.
 * @param condition Optional per-node predicate; the [process] callback is only invoked
 *   when this returns `true` for a given node.
 * @param process   The callback invoked on each eligible node.
 */
internal fun <T : InputEvent> Screen.processInputEvent(
    node: LayoutNode,
    event: T,
    condition: (LayoutNode) -> Boolean = { true },
    process: (LayoutNode, T) -> Unit,
) {
    for (child in node.childrenDescendingZ()) {
        if (event.isConsumed) break
        processInputEvent(child, event, condition, process)
    }
    if (!event.isConsumed && condition(node)) {
        process(node, event)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Pointer (mouse) events
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Dispatches a [PointerEvent] of [eventType] through the [node] tree.
 *
 * Only nodes that pass [condition] (default: bounded by the mouse position) receive
 * the event. Pass `global = true` to dispatch to all nodes regardless of bounds.
 *
 * @return The dispatched [PointerEvent] (check [PointerEvent.bypassSuper] to decide
 *   whether to call the vanilla screen's `super` method).
 */
@Suppress("NOTHING_TO_INLINE")
internal inline fun Screen.processPointerEvent(
    node: LayoutNode,
    mouseX: Double,
    mouseY: Double,
    eventType: PointerEventType,
    global: Boolean = false,
    noinline condition: (LayoutNode) -> Boolean = { it.isBounded(mouseX.toInt(), mouseY.toInt()) },
): PointerEvent {
    val event = BasicPointerEvent(eventType, mouseX, mouseY)
    processInputEvent(node, event, if (global) { _ -> true } else condition) { currentNode, currentEvent ->
        currentNode.modifier.foldIn(Unit) { _, el ->
            if (el is OnPointerEventModifier<*> && el.eventType == eventType && (global || !currentEvent.isConsumed))
                @Suppress("UNCHECKED_CAST")
                (el.onEvent as (UINode, PointerEvent) -> Unit)(currentNode, event)
        }
    }
    return event
}

/**
 * Dispatches a [ScrollEvent] through the [node] tree.
 *
 * @return The dispatched [ScrollEvent].
 */
@Suppress("NOTHING_TO_INLINE")
internal inline fun Screen.processScrollEvent(
    node: LayoutNode,
    mouseX: Double,
    mouseY: Double,
    scrollX: Double,
    scrollY: Double,
    eventType: PointerEventType,
    global: Boolean = false,
): ScrollEvent {
    val event = ScrollEvent(eventType, mouseX, mouseY, scrollX, scrollY)
    processInputEvent(
        node, event,
        if (global) { _ -> true } else { n -> n.isBounded(mouseX.toInt(), mouseY.toInt()) },
    ) { currentNode, currentEvent ->
        currentNode.modifier.foldIn(Unit) { _, el ->
            if (el is OnPointerEventModifier<*> && el.eventType == eventType && (global || !currentEvent.isConsumed))
                @Suppress("UNCHECKED_CAST")
                (el.onEvent as (UINode, PointerEvent) -> Unit)(currentNode, event)
        }
    }
    return event
}

/**
 * Dispatches a [DragEvent] through the [node] tree.
 *
 * @return The dispatched [DragEvent].
 */
@Suppress("NOTHING_TO_INLINE")
internal inline fun Screen.processDragEvent(
    node: LayoutNode,
    mouseX: Double,
    mouseY: Double,
    button: Int,
    dragX: Double,
    dragY: Double,
    eventType: PointerEventType,
    global: Boolean = false,
): DragEvent {
    val event = DragEvent(eventType, mouseX, mouseY, button, dragX, dragY)
    processInputEvent(
        node, event,
        if (global) { _ -> true } else { n -> n.isBounded(mouseX.toInt(), mouseY.toInt()) },
    ) { currentNode, currentEvent ->
        currentNode.modifier.foldIn(Unit) { _, el ->
            if (el is OnPointerEventModifier<*> && el.eventType == eventType && (global || !currentEvent.isConsumed))
                @Suppress("UNCHECKED_CAST")
                (el.onEvent as (UINode, PointerEvent) -> Unit)(currentNode, event)
        }
    }
    return event
}

// ─────────────────────────────────────────────────────────────────────────────
// Keyboard events
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Dispatches a [KeyEvent] through the [node] tree, chaining all [OnKeyEventModifier]s.
 *
 * @return The dispatched [KeyEvent].
 */
@Suppress("NOTHING_TO_INLINE")
internal inline fun Screen.processKeyEvent(
    node: LayoutNode,
    keyCode: Int,
    scanCode: Int,
    modifiers: Int,
): KeyEvent {
    val event = KeyEvent(keyCode, scanCode, modifiers)
    processInputEvent(node, event) { currentNode, currentEvent ->
        currentNode.modifier.foldIn(Unit) { _, el ->
            if (el is OnKeyEventModifier && !currentEvent.isConsumed)
                el.onEvent(currentNode, currentEvent)
        }
    }
    return event
}

/**
 * Dispatches a [CharEvent] through the [node] tree, chaining all [OnCharTypedModifier]s.
 *
 * @return The dispatched [CharEvent].
 */
@Suppress("NOTHING_TO_INLINE")
internal inline fun Screen.processCharEvent(
    node: LayoutNode,
    codePoint: Char,
    modifiers: Int,
): CharEvent {
    val event = CharEvent(codePoint, modifiers)
    processInputEvent(node, event) { currentNode, currentEvent ->
        currentNode.modifier.foldIn(Unit) { _, el ->
            if (el is OnCharTypedModifier && !currentEvent.isConsumed)
                el.onEvent(currentNode, currentEvent)
        }
    }
    return event
}
