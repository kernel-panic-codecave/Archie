package net.kernelpanicsoft.archie.gui.composables.input

import androidx.compose.runtime.*
import net.kernelpanicsoft.archie.gui.animation.AnimationSpec
import net.kernelpanicsoft.archie.gui.animation.Easings
import net.kernelpanicsoft.archie.gui.animation.animateInt
import net.kernelpanicsoft.archie.gui.composables.basic.Text
import net.kernelpanicsoft.archie.gui.composables.theme.TextureStates
import net.kernelpanicsoft.archie.gui.composables.theme.WidgetState
import net.kernelpanicsoft.archie.gui.layout.Alignment
import net.kernelpanicsoft.archie.gui.layout.BoxMeasurePolicy
import net.kernelpanicsoft.archie.gui.layout.Layout
import net.kernelpanicsoft.archie.gui.layout.Renderer
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.modifiers.DebugModifier
import net.kernelpanicsoft.archie.gui.modifiers.position.offset
import net.kernelpanicsoft.archie.gui.nodes.UINode
import net.kernelpanicsoft.archie.gui.theme.LocalTheme
import net.kernelpanicsoft.archie.gui.theme.ThemeVariants
import net.kernelpanicsoft.archie.gui.theme.contentPaddingModifier
import net.kernelpanicsoft.archie.gui.theme.intrinsicSizeModifier
import net.kernelpanicsoft.archie.gui.util.KColor
import net.kernelpanicsoft.archie.gui.util.extension.drawThemeState
import net.kernelpanicsoft.archie.gui.util.extension.invoke
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import kotlin.time.Duration.Companion.milliseconds

/**
 * A standard themed, clickable button.
 *
 * Renders the themed [texture] state ([TextureStates.DEFAULT]/[TextureStates.FOCUSED]/
 * [TextureStates.CLICKED]/[TextureStates.DISABLED]) behind [content], animating a 1px press
 * offset while held. For fully custom visuals, use [ButtonCore] directly instead.
 *
 * @param onClick  Invoked with the receiving [UINode] when the button is pressed.
 * @param modifier Additional modifiers applied to the outer clickable container.
 * @param enabled  When `false`, the disabled state is drawn and pointer events are ignored.
 * @param texture  The themed texture key to look up via [LocalTheme].
 * @param variant  The theme variant of [texture] to use. See [ThemeVariants].
 * @param ignoreMinSize Lets a [modifier] size this button below what its theme declares as a
 *   minimum - see [intrinsicSizeModifier]. Without it the theme's floor and the caller's size become
 *   a `min > max` constraint and the button does not lay out at all, which reads as it simply not
 *   drawing. The theme's own content padding is dropped along with the minimum: a theme sizes that
 *   padding for a button of its intended size, and four pixels either side of an eight-pixel one
 *   leaves the content none at all. A button this small is expected to hold a scaled glyph rather
 *   than a label, so it is the caller's business how tightly it sits.
 * @param content  The button's foreground content (e.g. a [net.kernelpanicsoft.archie.gui.composables.basic.Text]).
 */
@Composable
fun Button(
	onClick: (UINode) -> Unit,
	modifier: Modifier = Modifier,
	enabled: Boolean = true,
	texture: String = "button",
	variant: String = ThemeVariants.DEFAULT,
	ignoreMinSize: Boolean = false,
	content: @Composable () -> Unit = {}
) {
    val theme = LocalTheme.current
    val composableTheme = theme.getComposableTheme(texture)
    val measurePolicy = remember { BoxMeasurePolicy(Alignment.Center) }

    ButtonCore(
        onClick,
        modifier,
        enabled
    ) { isHovered, isPressed, isFocused ->
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
                        WidgetState.clicked(isPressed), WidgetState.focused(isHovered || isFocused),
                        enabled = enabled,
                    )
                    node.renderState = stateKey
                    val state = composableTheme.getState(stateKey, variant)

                    drawThemeState(state, x, y, node.width, node.height)
                }
            },
            modifier = modifier
                .then(composableTheme.intrinsicSizeModifier(ignoreMinSize))
                .then(if (ignoreMinSize) Modifier else composableTheme.contentPaddingModifier())
                .offset(x = 0, y = pressOffset)
        )
    }
}

@Composable
fun TextButton(
	text: Component,
	color: KColor = LocalTheme.current.lightTextColor,
	modifier: Modifier = Modifier,
	enabled: Boolean = true,
	texture: String = "button",
	variant: String = ThemeVariants.DEFAULT,
	onClick: (UINode) -> Unit,
) = Button(
   modifier = modifier,
   enabled = enabled,
   texture = texture,
   variant = variant,
   onClick = onClick,
) {
	Text(text, dropShadow = false, color = color)
}


/**
 * A stateless clickable container composable.
 *
 * `ButtonCore` manages hover, pressed and focus state internally and exposes them to
 * [content] via the lambda parameters. It handles cursor changes, the full pointer-event
 * lifecycle, and vanilla keyboard/controller focus navigation - Tab/Shift-Tab and arrow keys
 * (via `Screen.children()`/`nextFocusPath`) can reach and activate it (Enter/Space) exactly
 * like a plain `AbstractWidget`, including through controller-navigation mods such as
 * Controlify. It applies no visual styling of its own - `isFocused` is exposed to [content]
 * so callers can render their own focus indicator (see [Button]'s themed `focused` state).
 *
 * Use [ButtonCore] when you need custom button visuals. For a standard themed button, use
 * [Button] instead.
 *
 * ### Example
 * ```kotlin
 * ButtonCore(onClick = { println("Clicked!") }) { isHovered, isPressed, isFocused ->
 *     Box(
 *         modifier = Modifier.background(if (isHovered) KColor.LIGHT_GRAY else KColor.GRAY)
 *             .size(80, 20)
 *     ) {
 *         Text(Component.literal("Click me"))
 *     }
 * }
 * ```
 *
 * @param onClick  Invoked with the receiving [UINode] when the button is pressed (by mouse,
 *   or by Enter/Space while vanilla-focused).
 * @param modifier Additional modifiers applied to the outer clickable container.
 * @param enabled  When `false`, pointer and activation-key events are ignored and no cursor
 *   change occurs.
 * @param content  The button's visual content, receiving `isHovered`, `isPressed` and
 *   `isFocused` booleans.
 */
@Composable
fun ButtonCore(
	onClick: (UINode) -> Unit,
	modifier: Modifier = Modifier,
	enabled: Boolean = true,
	content: @Composable (isHovered: Boolean, isPressed: Boolean, isFocused: Boolean) -> Unit,
) {
    Clickable(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .then(DebugModifier(strs = listOf("Enabled: $enabled")))
            .then(modifier),
        content = content,
    )
}
