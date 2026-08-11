package net.kernelpanicsoft.archie.gui.composables.input

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import net.kernelpanicsoft.archie.gui.layout.Alignment
import net.kernelpanicsoft.archie.gui.layout.Box
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.modifiers.input.PointerEventType
import net.kernelpanicsoft.archie.gui.modifiers.input.onPointerEvent
import net.kernelpanicsoft.archie.gui.nodes.UINode
import net.kernelpanicsoft.archie.util.minecraftClient
import net.minecraft.client.Minecraft
import org.lwjgl.glfw.GLFW

private object CursorCache {
    val handCursor: Long by lazy { GLFW.glfwCreateStandardCursor(GLFW.GLFW_HAND_CURSOR) }
}

private fun setHandCursor(enabled: Boolean) {
    // GLFW calls must happen on the render thread; DisposableEffect callbacks run on the
    // recomposition dispatcher, so hop over via Minecraft's thread-safe task queue.
    minecraftClient.execute {
        val window = minecraftClient.window.window
        GLFW.glfwSetCursor(window, if (enabled) CursorCache.handCursor else 0L)
    }
}

/**
 * Low-level unstyled clickable container used by higher-level inputs like [ButtonCore].
 *
 * Tracks hover/press state and fires [onClick] on press (not release), showing the system
 * hand cursor on hover when [showHandCursor] is `true`. Applies no visual styling itself -
 * that is entirely up to [content].
 *
 * @param onClick        Invoked with the receiving [UINode] on press.
 * @param modifier       Additional modifiers applied to the outer [Box].
 * @param enabled        When `false`, pointer events are ignored and no cursor change occurs.
 * @param showHandCursor Whether to switch to the hand cursor while hovered.
 * @param content        The visual content; receives `isHovered`/`isPressed` for styling.
 */
@Composable
fun Clickable(
	onClick: (UINode) -> Unit,
	modifier: Modifier = Modifier,
	enabled: Boolean = true,
	showHandCursor: Boolean = true,
	content: @Composable (isHovered: Boolean, isPressed: Boolean) -> Unit,
) {
    var hovered by remember { mutableStateOf(false) }
    var pressed by remember { mutableStateOf(false) }

    DisposableEffect(enabled, hovered, showHandCursor) {
        if ((!enabled || !hovered) && showHandCursor) setHandCursor(false)
        onDispose {
            if (showHandCursor) setHandCursor(false)
        }
    }

    Box(
        modifier = Modifier
            .onPointerEvent<UINode>(PointerEventType.ENTER) { _, e ->
                if (!enabled) return@onPointerEvent
                hovered = true
                if (showHandCursor) setHandCursor(true)
                e.consume()
            }
            .onPointerEvent<UINode>(PointerEventType.EXIT) { _, e ->
                hovered = false
                pressed = false
                if (showHandCursor) setHandCursor(false)
                if (enabled) e.consume()
            }
            .onPointerEvent<UINode>(PointerEventType.PRESS) { node, e ->
                if (!enabled) return@onPointerEvent
                pressed = true
                onClick(node)
                e.consume(true)
            }
            .onPointerEvent<UINode>(PointerEventType.GLOBAL_RELEASE) { _, _ ->
                pressed = false
            }
            .then(modifier),
        contentAlignment = Alignment.Center,
    ) {
        content(hovered, pressed)
    }
}

