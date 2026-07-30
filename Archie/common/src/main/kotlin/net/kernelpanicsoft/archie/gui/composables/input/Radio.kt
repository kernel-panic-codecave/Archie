package net.kernelpanicsoft.archie.gui.composables.input

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import net.kernelpanicsoft.archie.gui.composables.basic.Text
import net.kernelpanicsoft.archie.gui.layout.Alignment
import net.kernelpanicsoft.archie.gui.layout.Arrangement
import net.kernelpanicsoft.archie.gui.layout.BoxMeasurePolicy
import net.kernelpanicsoft.archie.gui.layout.Column
import net.kernelpanicsoft.archie.gui.layout.Layout
import net.kernelpanicsoft.archie.gui.layout.Renderer
import net.kernelpanicsoft.archie.gui.layout.Row
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.modifiers.sizeIn
import net.kernelpanicsoft.archie.gui.nodes.AUINode
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component

private const val RADIO_SIZE = 12

/**
 * Low-level unstyled radio-button behavior, built on [Clickable].
 *
 * Calls [onSelect] on press only when not already [selected] (clicking an already-selected
 * radio option is a no-op, matching standard radio-group semantics). Applies no visuals -
 * that is up to [content].
 *
 * @param selected Whether this option is currently selected.
 * @param onSelect Invoked when this (unselected) option is clicked.
 * @param modifier Additional modifiers applied to the outer clickable container.
 * @param enabled  When `false`, pointer events are ignored.
 * @param content  The visual content; receives hover/press state and [selected].
 */
@Composable
fun RadioButtonCore(
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable (isHovered: Boolean, isPressed: Boolean, selected: Boolean) -> Unit,
) {
    Clickable(
        onClick = { if (!selected) onSelect() },
        enabled = enabled,
        modifier = modifier,
    ) { hovered, pressed ->
        content(hovered, pressed, selected)
    }
}

/** A standard 12x12 boxed radio button with a filled center dot when [selected]. See [RadioGroup] for a labeled option list. */
@Composable
fun RadioButton(
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val measurePolicy = remember { BoxMeasurePolicy(Alignment.Center) }
    RadioButtonCore(
        selected = selected,
        onSelect = onSelect,
        enabled = enabled,
        modifier = Modifier.sizeIn(minWidth = RADIO_SIZE, minHeight = RADIO_SIZE).then(modifier),
    ) { hovered, _, currentSelected ->
        Layout(
            name = "RadioButton",
            measurePolicy = measurePolicy,
            renderer = object : Renderer {
                override fun render(
                    node: AUINode,
                    x: Int,
                    y: Int,
                    guiGraphics: GuiGraphics,
                    mouseX: Int,
                    mouseY: Int,
                    partialTick: Float,
                ) {
                    val borderColor = when {
                        !enabled -> 0xFF777777.toInt()
                        hovered -> 0xFFFFFFFF.toInt()
                        else -> 0xFFB8B8B8.toInt()
                    }
                    guiGraphics.fill(x, y, x + node.width, y + node.height, borderColor)
                    guiGraphics.fill(x + 1, y + 1, x + node.width - 1, y + node.height - 1, 0xFF2D2D2D.toInt())
                    if (currentSelected) {
                        guiGraphics.fill(x + 3, y + 3, x + node.width - 3, y + node.height - 3, 0xFF6BA8FF.toInt())
                    }
                    super.render(node, x, y, guiGraphics, mouseX, mouseY, partialTick)
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
                Text(option.label, dropShadow = false)
            }
        }
    }
}

