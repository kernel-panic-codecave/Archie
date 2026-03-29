package net.kernelpanicsoft.archie.gui.composables.input

import androidx.compose.runtime.*
import net.kernelpanicsoft.archie.gui.composables.theme.TextureStates
import net.kernelpanicsoft.archie.gui.layout.Box
import net.kernelpanicsoft.archie.gui.layout.Alignment
import net.kernelpanicsoft.archie.gui.layout.BoxMeasurePolicy
import net.kernelpanicsoft.archie.gui.layout.Layout
import net.kernelpanicsoft.archie.gui.layout.Renderer
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.modifiers.DebugModifier
import net.kernelpanicsoft.archie.gui.modifiers.input.PointerEventType
import net.kernelpanicsoft.archie.gui.modifiers.input.onPointerEvent
import net.kernelpanicsoft.archie.gui.modifiers.sizeIn
import net.kernelpanicsoft.archie.gui.nodes.AUINode
import net.kernelpanicsoft.archie.gui.theme.LocalTheme
import net.kernelpanicsoft.archie.gui.util.extension.drawThemeState
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import org.lwjgl.glfw.GLFW

@Composable
fun Button(
    onClick: (AUINode) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    texture: String = "button",
    content: @Composable () -> Unit = {}
) {
    val theme = LocalTheme.current
    val composableTheme = theme.getComposableTheme(texture)

    ButtonCore(
        onClick,
        modifier,
        enabled
    ) { isHovered, isPressed ->
        Layout(
            name = "Button",
            content = content,
            measurePolicy = BoxMeasurePolicy(Alignment.Center),
            renderer = object : Renderer
            {
                override fun render(
                    node: AUINode,
                    x: Int,
                    y: Int,
                    guiGraphics: GuiGraphics,
                    mouseX: Int,
                    mouseY: Int,
                    partialTick: Float
                ) {
                    val state = composableTheme.getState(
                        when {
                            !enabled -> TextureStates.DISABLED
                            isPressed && composableTheme.hasState(
                                TextureStates.CLICKED,
                                theme.mode
                            ) -> TextureStates.CLICKED

                            isHovered -> TextureStates.HOVERED
                            else -> TextureStates.DEFAULT
                        },
                        theme.mode
                    )

                    guiGraphics.drawThemeState(state, x, y, node.width, node.height)

                    return super.render(
                        node,
                        x,
                        y,
                        guiGraphics,
                        mouseX,
                        mouseY,
                        partialTick
                    )
                }
            },
            modifier = modifier.apply {
                if (!composableTheme.isNinepatch) with(composableTheme.states["default"]!!) {
                    sizeIn(
                        minWidth = textureSize.width,
                        minHeight = textureSize.height
                    )
                }
            }
        )
    }
}


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
