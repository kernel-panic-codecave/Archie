package net.kernelpanicsoft.archie.gui.composables.input

import androidx.compose.runtime.*
import net.kernelpanicsoft.archie.gui.animation.AnimationSpec
import net.kernelpanicsoft.archie.gui.animation.Easings
import net.kernelpanicsoft.archie.gui.animation.animateInt
import net.kernelpanicsoft.archie.gui.composables.theme.TextureStates
import net.kernelpanicsoft.archie.gui.composables.theme.WidgetState
import net.kernelpanicsoft.archie.gui.layout.Alignment
import net.kernelpanicsoft.archie.gui.layout.BoxMeasurePolicy
import net.kernelpanicsoft.archie.gui.layout.Layout
import net.kernelpanicsoft.archie.gui.layout.Renderer
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.modifiers.DebugModifier
import net.kernelpanicsoft.archie.gui.modifiers.sizeIn
import net.kernelpanicsoft.archie.gui.modifiers.position.offset
import net.kernelpanicsoft.archie.gui.nodes.UINode
import net.kernelpanicsoft.archie.gui.theme.LocalTheme
import net.kernelpanicsoft.archie.gui.theme.SimpleThemeState
import net.kernelpanicsoft.archie.gui.theme.ThemeVariants
import net.kernelpanicsoft.archie.gui.util.extension.drawThemeState
import net.kernelpanicsoft.archie.gui.util.extension.invoke
import net.minecraft.client.gui.GuiGraphics
import kotlin.time.Duration.Companion.milliseconds

/**
 * A standard themed, clickable button.
 *
 * Renders the themed [texture] state ([TextureStates.DEFAULT]/[TextureStates.HOVERED]/
 * [TextureStates.CLICKED]/[TextureStates.DISABLED]) behind [content], animating a 1px press
 * offset while held. For fully custom visuals, use [ButtonCore] directly instead.
 *
 * @param onClick  Invoked with the receiving [UINode] when the button is pressed.
 * @param modifier Additional modifiers applied to the outer clickable container.
 * @param enabled  When `false`, the disabled state is drawn and pointer events are ignored.
 * @param texture  The themed texture key to look up via [LocalTheme].
 * @param variant  The theme variant of [texture] to use. See [ThemeVariants].
 * @param content  The button's foreground content (e.g. a [net.kernelpanicsoft.archie.gui.composables.basic.Text]).
 */
@Composable
fun Button(
	onClick: (UINode) -> Unit,
	modifier: Modifier = Modifier,
	enabled: Boolean = true,
	texture: String = "button",
	variant: String = ThemeVariants.DEFAULT,
	content: @Composable () -> Unit = {}
) {
    val theme = LocalTheme.current
    val composableTheme = theme.getComposableTheme(texture)
    val measurePolicy = remember { BoxMeasurePolicy(Alignment.Center) }

    ButtonCore(
        onClick,
        modifier,
        enabled
    ) { isHovered, isPressed ->
        val pressOffset = animateInt(
            targetValue = if (isPressed) 1 else 0,
            spec = AnimationSpec(durationMillis = 90.milliseconds, easing = Easings.OutCubic),
        )

        Layout(
            name = "Button",
            content = content,
            measurePolicy = measurePolicy,
            renderer = object : Renderer
            {
                override fun render(
	                node: UINode,
	                x: Int,
	                y: Int,
	                guiGraphics: GuiGraphics,
	                mouseX: Int,
	                mouseY: Int,
	                partialTick: Float
                ) = guiGraphics {
                    val stateKey = WidgetState.resolve(
                        composableTheme, variant,
                        WidgetState.clicked(isPressed), WidgetState.hovered(isHovered),
                        enabled = enabled,
                    )
                    node.renderState = stateKey
                    val state = composableTheme.getState(stateKey, variant)

                    drawThemeState(state, x, y, node.width, node.height)
                }
            },
            modifier = modifier.apply {
                if (!composableTheme.isNineslice) {
                    with(composableTheme.states[TextureStates.DEFAULT] as SimpleThemeState) {
                        sizeIn(
                            minWidth = width,
                            minHeight = height
                        )
                    }
                }
            }.offset(x = 0, y = pressOffset)
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
 * [Button] instead.
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
 * @param onClick  Invoked with the receiving [UINode] when the button is pressed.
 * @param modifier Additional modifiers applied to the outer clickable container.
 * @param enabled  When `false`, pointer events are ignored and no cursor change occurs.
 * @param content  The button's visual content, receiving `isHovered` and `isPressed` booleans.
 */
@Composable
fun ButtonCore(
	onClick: (UINode) -> Unit,
	modifier: Modifier = Modifier,
	enabled: Boolean = true,
	content: @Composable (isHovered: Boolean, isPressed: Boolean) -> Unit,
) {
    Clickable(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.then(DebugModifier(strs = listOf("Enabled: $enabled"))).then(modifier),
    ) { isHovered, isPressed ->
        content(isHovered, isPressed)
    }
}
