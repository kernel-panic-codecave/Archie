package net.kernelpanicsoft.archie.gui.composables.input

import androidx.compose.runtime.*
import net.kernelpanicsoft.archie.gui.animation.AnimationSpec
import net.kernelpanicsoft.archie.gui.animation.Easings
import net.kernelpanicsoft.archie.gui.animation.animatePulse
import net.kernelpanicsoft.archie.gui.composables.theme.WidgetState
import net.kernelpanicsoft.archie.gui.interaction.MutableInteractionSource
import net.kernelpanicsoft.archie.gui.interaction.collectIsFocusedAsState
import net.kernelpanicsoft.archie.gui.interaction.collectIsHoveredAsState
import net.kernelpanicsoft.archie.gui.layout.Alignment
import net.kernelpanicsoft.archie.gui.layout.BoxMeasurePolicy
import net.kernelpanicsoft.archie.gui.layout.Layout
import net.kernelpanicsoft.archie.gui.layout.Renderer
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.modifiers.input.toggleable
import net.kernelpanicsoft.archie.gui.nodes.UINode
import net.kernelpanicsoft.archie.gui.theme.LocalTheme
import net.kernelpanicsoft.archie.gui.theme.ThemeVariants
import net.kernelpanicsoft.archie.gui.theme.intrinsicSizeModifier
import net.kernelpanicsoft.archie.gui.util.extension.drawThemeState
import net.kernelpanicsoft.archie.gui.util.extension.invoke
import net.minecraft.client.gui.GuiGraphics
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

/**
 * A standard themed checkbox.
 *
 * Renders the themed [texture] state - a combined checked+hovered state is used when both
 * apply and the theme defines it. Built on top of [CheckboxCore]; use that directly for
 * fully custom visuals.
 *
 * @param checked         The current checked state.
 * @param modifier        Additional modifiers applied to the outer container.
 * @param enabled         When `false`, the disabled state is drawn, pointer/activation-key
 *   events are ignored, and this drops out of the vanilla focus graph entirely.
 * @param texture         The themed texture key to look up via [LocalTheme].
 * @param variant         The theme variant of [texture] to use. See [ThemeVariants].
 * @param onCheckedChange Called with the new checked value when the user clicks.
 */
@Composable
fun Checkbox(
    checked: Boolean = false,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    texture: String = "checkbox",
    variant: String = ThemeVariants.DEFAULT,
    onCheckedChange: (Boolean) -> Unit,
) {
    val theme = LocalTheme.current
    val composableTheme = theme.getComposableTheme(texture)
    val sizeModifier = composableTheme.intrinsicSizeModifier()
    // A brief overshoot-then-settle pop whenever `checked` flips, rather than the check mark
    // snapping in/out instantly - purely cosmetic, so it's scaled around the node's own center
    // instead of touching layout size.
    val pop = animatePulse(
        key = checked,
        spec = AnimationSpec(durationMillis = 160.milliseconds, easing = Easings.OutBack),
    )

    CheckboxCore(
        checked = checked,
        enabled = enabled,
        onCheckedChange = onCheckedChange,
    ) { checkboxModifier, isHovered, isFocused ->
        Layout(
            name = "Checkbox",
            measurePolicy = BoxMeasurePolicy(Alignment.Center),
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
                        WidgetState.clicked(checked), WidgetState.focused(isHovered || isFocused),
                        enabled = enabled,
                    )
                    node.renderState = stateKey
                    val state = composableTheme.getState(stateKey, variant)

                    val drawWidth = (node.width * pop).roundToInt()
                    val drawHeight = (node.height * pop).roundToInt()
                    drawThemeState(
                        state,
                        x + (node.width - drawWidth) / 2,
                        y + (node.height - drawHeight) / 2,
                        drawWidth,
                        drawHeight,
                    )
                }
            },
            modifier = checkboxModifier.then(sizeModifier).then(modifier)
        )
    }
}

/**
 * A stateless, unstyled toggle composable, built on [Modifier.toggleable].
 *
 * `CheckboxCore` manages hover/focus state internally and hands [content] the [Modifier] it
 * needs to apply to its own node to participate in pointer/focus input - it's a vanilla
 * keyboard/controller focus-navigation stop that toggles on Enter/Space while focused, the
 * same as a mouse click. All visual styling (textures, colours, checked indicator) is the
 * responsibility of [content]. Use this as the base for custom or theme-driven checkbox
 * implementations.
 *
 * ### Example
 * ```kotlin
 * var checked by remember { mutableStateOf(false) }
 * CheckboxCore(checked = checked, onCheckedChange = { checked = it }) { modifier, isHovered, isFocused ->
 *     Box(modifier = modifier.size(16, 16).background(if (checked) KColor.GREEN else KColor.GRAY))
 * }
 * ```
 *
 * @param checked         The current checked state.
 * @param enabled         When `false`, pointer and activation-key events are ignored and this
 *   drops out of the vanilla focus graph entirely.
 * @param onCheckedChange Called with the new checked value when the user clicks.
 * @param content         The visual content; receives the [Modifier] to apply to its own node,
 *   plus `isHovered`/`isFocused` for styling.
 */
@Composable
fun CheckboxCore(
    checked: Boolean = false,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
    content: @Composable (modifier: Modifier, isHovered: Boolean, isFocused: Boolean) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()

    val toggleableModifier = Modifier.toggleable(
        value = checked,
        enabled = enabled,
        interactionSource = interactionSource,
        onValueChange = onCheckedChange,
    )

    content(toggleableModifier, isHovered, isFocused)
}
