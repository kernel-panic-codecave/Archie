package net.kernelpanicsoft.archie.gui.focus

import net.kernelpanicsoft.archie.gui.layout.LayoutNode
import net.kernelpanicsoft.archie.gui.modifiers.input.CharEvent
import net.kernelpanicsoft.archie.gui.modifiers.input.FocusableModifier
import net.kernelpanicsoft.archie.gui.modifiers.input.KeyEvent
import net.kernelpanicsoft.archie.gui.modifiers.input.OnCharTypedModifier
import net.kernelpanicsoft.archie.gui.modifiers.input.OnKeyEventModifier
import net.minecraft.client.gui.ComponentPath
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.navigation.FocusNavigationEvent
import net.minecraft.client.gui.navigation.ScreenRectangle

/**
 * Bridges one [FocusableModifier]-carrying [LayoutNode] into vanilla Minecraft's
 * `GuiEventListener` focus graph, so vanilla's Tab/Shift-Tab and arrow-key navigation - and
 * anything else that walks `GuiEventListener`, e.g. Controlify's controller navigation -
 * reaches Compose content the same way it reaches an ordinary `AbstractWidget`.
 *
 * `ComposeScreen`/`ComposeContainerScreen` rebuild a fresh list of adapters on every
 * `children()` call, since the [LayoutNode] tree can change between frames. Vanilla's own
 * `ContainerEventHandler.handleTabNavigation` looks up the previously-focused
 * `GuiEventListener` by `indexOf` in that freshly rebuilt list, so [equals]/[hashCode] are
 * keyed on the wrapped node's identity rather than the adapter's - two adapters wrapping the
 * same node must compare equal across separate `children()` calls, or Tab would never be able
 * to tell "the currently focused thing" apart from "a brand new list entry" and always restart
 * from the beginning.
 */
class LayoutNodeFocusAdapter(val node: LayoutNode) : GuiEventListener {

    private val focusable: FocusableModifier?
        get() = node.get<FocusableModifier>()

    override fun equals(other: Any?): Boolean = other is LayoutNodeFocusAdapter && other.node === node
    override fun hashCode(): Int = System.identityHashCode(node)

    override fun getRectangle(): ScreenRectangle {
        val (x, y) = node.absoluteCoords
        return ScreenRectangle(x, y, node.width, node.height)
    }

    override fun isFocused(): Boolean = focusable?.focused?.value == true

    override fun setFocused(focused: Boolean) {
        val f = focusable ?: return
        f.setFocused(focused)
        // A redundant call while already focused (e.g. ComponentPath.Path.applyFocus calling
        // this twice for one logical focus change) is harmless here - bringing an
        // already-visible node into view again is a no-op.
        if (focused) f.bringIntoViewParent?.bringIntoView(node)
    }

    // The GuiEventListener default always returns null - AbstractWidget overrides it the same
    // way, to actually offer itself as a leaf when not already focused. Without this override,
    // ContainerEventHandler's Tab/arrow-key search (which calls nextFocusPath on every
    // candidate from children(), not just membership-checks it) would never select anything
    // here, silently leaving Tab navigation a no-op.
    override fun nextFocusPath(event: FocusNavigationEvent): ComponentPath? =
        if (!isFocused()) ComponentPath.leaf(this) else null

    /** Forwards to [node]'s own [OnKeyEventModifier] chain - the same one the broadcast key dispatch uses. */
    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        val event = KeyEvent(keyCode, scanCode, modifiers)
        node.modifier.foldIn(Unit) { _, el ->
            if (el is OnKeyEventModifier && !event.isConsumed) el.onEvent(node, event)
        }
        return event.isConsumed
    }

    /** Forwards to [node]'s own [OnCharTypedModifier] chain - the same one the broadcast char dispatch uses. */
    override fun charTyped(codePoint: Char, modifiers: Int): Boolean {
        val event = CharEvent(codePoint, modifiers)
        node.modifier.foldIn(Unit) { _, el ->
            if (el is OnCharTypedModifier && !event.isConsumed) el.onEvent(node, event)
        }
        return event.isConsumed
    }
}

/**
 * Collects every [FocusableModifier]-carrying [LayoutNode] in [root]'s subtree, each wrapped
 * as a [LayoutNodeFocusAdapter], in composition (depth-first, declaration) order - the order
 * vanilla's Tab-navigation (`ContainerEventHandler.handleTabNavigation`) walks by default.
 *
 * Backs `ComposeScreen`/`ComposeContainerScreen`'s `children()` override, the one hook that
 * lets vanilla's built-in Tab/Shift-Tab/arrow-key navigation - and anything else that walks
 * `GuiEventListener`, e.g. Controlify - see Compose content at all.
 */
internal fun collectFocusableChildren(root: LayoutNode): List<GuiEventListener> = buildList {
    fun visit(node: LayoutNode) {
        if (node.get<FocusableModifier>() != null) add(LayoutNodeFocusAdapter(node))
        node.children.toList().forEach(::visit)
    }
    visit(root)
}
