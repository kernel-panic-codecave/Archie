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

data class RadioOption<T>(
    val value: T,
    val label: Component,
    val enabled: Boolean = true,
)

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

