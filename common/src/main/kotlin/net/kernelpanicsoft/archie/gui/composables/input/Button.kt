package net.kernelpanicsoft.archie.gui.composables.input

import androidx.compose.runtime.*
import net.kernelpanicsoft.archie.gui.layout.Box
import net.kernelpanicsoft.archie.gui.layout.Alignment
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.modifiers.DebugModifier
import net.kernelpanicsoft.archie.gui.modifiers.input.PointerEventType
import net.kernelpanicsoft.archie.gui.modifiers.input.onPointerEvent
import net.kernelpanicsoft.archie.gui.nodes.AUINode
import net.minecraft.client.Minecraft
import org.lwjgl.glfw.GLFW

/**
 * A stateless clickable container composable.
 *
 * `ButtonCore` manages hover and pressed state internally and exposes them to [content]
 * via the lambda parameters. It handles cursor changes and the full pointer-event lifecycle,
 * but applies no visual styling of its own — that is left entirely to [content].
 *
 * Use [ButtonCore] when you need custom button visuals. For a standard themed button, use
 * the higher-level `Button` composable in your theme-aware composables package.
 *
 * ### Example
 * ```kotlin
 * ButtonCore(onClick = { println("Clicked!") }) { isHovered, isPressed ->
 *     Box(
 *         modifier = Modifier.background(if (isHovered) KColor.LIGHT_GRAY else KColor.GRAY)
 *             .size(80, 20)
 *     ) {
 *         Text(Component.literal("Click me"))
 *     }
 * }
 * ```
 *
 * @param onClick  Invoked with the receiving [AUINode] when the button is pressed.
 * @param modifier Additional modifiers applied to the outer [Box].
 * @param enabled  When `false`, pointer events are ignored and no cursor change occurs.
 * @param content  The button's visual content, receiving `isHovered` and `isPressed` booleans.
 */
@Composable
fun ButtonCore(
    onClick: (AUINode) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable (isHovered: Boolean, isPressed: Boolean) -> Unit,
) {
    var hovered by remember { mutableStateOf(false) }
    var pressed  by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .then(DebugModifier(strs = listOf("Hovered: $hovered", "Clicked: $pressed", "Enabled: $enabled")))
            .onPointerEvent<AUINode>(PointerEventType.ENTER) { _, e ->
                if (!enabled) return@onPointerEvent
                hovered = true
                GLFW.glfwSetCursor(Minecraft.getInstance().window.window, GLFW.glfwCreateStandardCursor(GLFW.GLFW_HAND_CURSOR))
                e.consume()
            }
            .onPointerEvent<AUINode>(PointerEventType.EXIT) { _, e ->
                if (!enabled) return@onPointerEvent
                hovered = false
                GLFW.glfwSetCursor(Minecraft.getInstance().window.window, GLFW.glfwCreateStandardCursor(GLFW.GLFW_ARROW_CURSOR))
                e.consume()
            }
            .onPointerEvent<AUINode>(PointerEventType.PRESS) { node, e ->
                if (enabled) { pressed = true; onClick(node); e.consume() }
            }
            .onPointerEvent<AUINode>(PointerEventType.GLOBAL_RELEASE) { _, _ ->
                if (enabled) pressed = false
            }
            .then(modifier),
        contentAlignment = Alignment.Center,
    ) {
        content(hovered, pressed)
    }
}
