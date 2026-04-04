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
import net.kernelpanicsoft.archie.gui.nodes.AUINode
import net.minecraft.client.Minecraft
import org.lwjgl.glfw.GLFW

private object CursorCache {
    val handCursor: Long by lazy { GLFW.glfwCreateStandardCursor(GLFW.GLFW_HAND_CURSOR) }
}

private fun setHandCursor(enabled: Boolean) {
    val window = Minecraft.getInstance().window.window
    GLFW.glfwSetCursor(window, if (enabled) CursorCache.handCursor else 0L)
}

/**
 * Low-level unstyled clickable container used by higher-level inputs.
 */
@Composable
fun Clickable(
    onClick: (AUINode) -> Unit,
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
            .onPointerEvent<AUINode>(PointerEventType.ENTER) { _, e ->
                if (!enabled) return@onPointerEvent
                hovered = true
                if (showHandCursor) setHandCursor(true)
                e.consume()
            }
            .onPointerEvent<AUINode>(PointerEventType.EXIT) { _, e ->
                hovered = false
                pressed = false
                if (showHandCursor) setHandCursor(false)
                if (enabled) e.consume()
            }
            .onPointerEvent<AUINode>(PointerEventType.PRESS) { node, e ->
                if (!enabled) return@onPointerEvent
                pressed = true
                onClick(node)
                e.consume(true)
            }
            .onPointerEvent<AUINode>(PointerEventType.GLOBAL_RELEASE) { _, _ ->
                pressed = false
            }
            .then(modifier),
        contentAlignment = Alignment.Center,
    ) {
        content(hovered, pressed)
    }
}

