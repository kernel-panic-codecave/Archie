package net.kernelpanicsoft.archie.gui.composables.input

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import net.kernelpanicsoft.archie.gui.animation.AnimationSpec
import net.kernelpanicsoft.archie.gui.animation.Easings
import net.kernelpanicsoft.archie.gui.animation.animatePulse
import net.kernelpanicsoft.archie.gui.composables.basic.Label
import net.kernelpanicsoft.archie.gui.composables.basic.Text
import net.kernelpanicsoft.archie.gui.composables.theme.TextureStates
import net.kernelpanicsoft.archie.gui.composables.theme.WidgetState
import net.kernelpanicsoft.archie.gui.interaction.MutableInteractionSource
import net.kernelpanicsoft.archie.gui.interaction.collectIsFocusedAsState
import net.kernelpanicsoft.archie.gui.interaction.collectIsHoveredAsState
import net.kernelpanicsoft.archie.gui.layout.Alignment
import net.kernelpanicsoft.archie.gui.layout.Arrangement
import net.kernelpanicsoft.archie.gui.layout.BoxMeasurePolicy
import net.kernelpanicsoft.archie.gui.layout.Column
import net.kernelpanicsoft.archie.gui.layout.Layout
import net.kernelpanicsoft.archie.gui.layout.Renderer
import net.kernelpanicsoft.archie.gui.layout.Row
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.modifiers.input.selectable
import net.kernelpanicsoft.archie.gui.nodes.UINode
import net.kernelpanicsoft.archie.gui.theme.LocalTheme
import net.kernelpanicsoft.archie.gui.theme.ThemeVariants
import net.kernelpanicsoft.archie.gui.theme.intrinsicSizeModifier
import net.kernelpanicsoft.archie.gui.util.extension.drawThemeState
import net.kernelpanicsoft.archie.gui.util.extension.invoke
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

/**
 * Low-level unstyled radio-button behavior, built on [Modifier.selectable].
 *
 * Calls [onSelect] on press only when not already [selected] (clicking an already-selected
 * radio option is a no-op, matching standard radio-group semantics) - the same on Enter/Space
 * while this is a vanilla keyboard/controller focus-navigation stop. Applies no visuals - that
 * is up to [content].
 *
 * @param selected Whether this option is currently selected.
 * @param onSelect Invoked when this (unselected) option is clicked.
 * @param enabled  When `false`, pointer events are ignored.
 * @param content  The visual content; receives the [Modifier] to apply to its own node, plus
 *   hover/focus state and [selected].
 */
@Composable
fun RadioButtonCore(
    selected: Boolean,
    onSelect: () -> Unit,
    enabled: Boolean = true,
    content: @Composable (modifier: Modifier, isHovered: Boolean, isFocused: Boolean, selected: Boolean) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()

    val selectableModifier = Modifier.selectable(
        selected = selected,
        enabled = enabled,
        interactionSource = interactionSource,
        onClick = { if (!selected) onSelect() },
    )

    content(selectableModifier, isHovered, isFocused, selected)
}

/**
 * A standard themed radio button with a filled center dot when [selected].
 *
 * Renders the themed [texture] state - a combined selected+hovered state is used when both
 * apply and the theme defines it. See [RadioGroup] for a labeled option list.
 *
 * @param selected  Whether this option is currently selected.
 * @param onSelect  Invoked when this (unselected) option is clicked.
 * @param modifier  Additional modifiers applied to the outer container.
 * @param enabled   When `false`, pointer events are ignored and the [TextureStates.DISABLED] state is shown.
 * @param texture   The themed texture key to look up via [LocalTheme].
 * @param variant   The theme variant of [texture] to use. See [ThemeVariants].
 */
@Composable
fun RadioButton(
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    texture: String = "radio",
    variant: String = ThemeVariants.DEFAULT,
) {
    val theme = LocalTheme.current
    val composableTheme = theme.getComposableTheme(texture)
    val measurePolicy = remember { BoxMeasurePolicy(Alignment.Center) }
    val sizeModifier = composableTheme.intrinsicSizeModifier()
    // A brief overshoot-then-settle pop whenever `selected` flips, rather than the dot
    // snapping in/out instantly - purely cosmetic, so it's scaled around the node's own
    // center instead of touching layout size.
    val pop = animatePulse(
        key = selected,
        spec = AnimationSpec(durationMillis = 160.milliseconds, easing = Easings.OutBack),
    )

    RadioButtonCore(
        selected = selected,
        onSelect = onSelect,
        enabled = enabled,
    ) { radioModifier, hovered, focused, currentSelected ->
        Layout(
            name = "RadioButton",
            measurePolicy = measurePolicy,
            modifier = radioModifier.then(sizeModifier).then(modifier),
            renderer = object : Renderer {
                override fun render(
	                node: UINode,
	                x: Int,
	                y: Int,
	                guiGraphics: GuiGraphics,
	                mouseX: Int,
	                mouseY: Int,
	                partialTick: Float,
                ) = guiGraphics {
                    val stateKey = WidgetState.resolve(
                        composableTheme, variant,
                        WidgetState.clicked(currentSelected), WidgetState.focused(hovered || focused),
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
        )
    }
}

/** A single labeled choice within a [RadioGroup]. */
data class RadioOption<T>(
    val value: T,
    val label: Component,
    val enabled: Boolean = true,
)

/**
 * A vertical list of labeled, mutually exclusive [RadioButton]s.
 *
 * @param options       The selectable options, in display order.
 * @param selected      The currently selected value, or `null` if none is selected.
 * @param onSelected    Called with an option's value when it is selected.
 * @param modifier      Additional modifiers applied to the outer [Column].
 * @param optionSpacing Vertical spacing between options, in pixels.
 */
@Composable
fun <T> RadioGroup(
    options: List<RadioOption<T>>,
    selected: T?,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    optionSpacing: Int = 3,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(optionSpacing)) {
        options.forEach { option ->
            Row(horizontalArrangement = Arrangement.spacedBy(4), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = option.value == selected,
                    enabled = option.enabled,
                    onSelect = { onSelected(option.value) },
                )
                Label(option.label)
            }
        }
    }
}

