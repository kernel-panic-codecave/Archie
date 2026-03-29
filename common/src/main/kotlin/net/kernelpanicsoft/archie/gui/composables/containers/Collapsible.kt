package net.kernelpanicsoft.archie.gui.composables.containers

import androidx.compose.runtime.*
import com.mojang.math.Axis
import kotlinx.coroutines.delay
import net.kernelpanicsoft.archie.gui.composables.basic.Spacer
import net.kernelpanicsoft.archie.gui.composables.basic.Text
import net.kernelpanicsoft.archie.gui.layout.*
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.modifiers.appearance.BackgroundModifier
import net.kernelpanicsoft.archie.gui.modifiers.fillMaxHeight
import net.kernelpanicsoft.archie.gui.modifiers.input.PointerEventType
import net.kernelpanicsoft.archie.gui.modifiers.input.onPointerEvent
import net.kernelpanicsoft.archie.gui.modifiers.position.MarginModifier
import net.kernelpanicsoft.archie.gui.modifiers.position.MarginValues
import net.kernelpanicsoft.archie.gui.modifiers.position.PaddingModifier
import net.kernelpanicsoft.archie.gui.modifiers.position.PaddingValues
import net.kernelpanicsoft.archie.gui.modifiers.size
import net.kernelpanicsoft.archie.gui.nodes.AUINode
import net.kernelpanicsoft.archie.gui.util.KColor
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import kotlin.math.abs

/**
 * A container that can be expanded or collapsed by clicking its header.
 *
 * The header displays [title] with an animated arrow indicator that rotates 90° when the
 * section is open. A vertical separator bar is shown to the left of the expanded content.
 *
 * ### Example
 * ```kotlin
 * Collapsible(title = Component.literal("Advanced Settings")) {
 *     // content shown when expanded
 *     Text(Component.literal("Option A"))
 * }
 * ```
 *
 * @param title            The text displayed in the collapsible header.
 * @param modifier         Modifiers applied to the outer [Column] container.
 * @param initiallyExpanded Whether the section starts in the expanded state.
 * @param onToggled        Called when the expanded state changes; receives the new state.
 * @param content          The composable content shown when expanded.
 */
@Composable
fun Collapsible(
    title: Component,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
    onToggled: (isExpanded: Boolean) -> Unit = {},
    content: @Composable () -> Unit,
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4)) {
        Row(
            modifier = Modifier.onPointerEvent<AUINode>(PointerEventType.PRESS) { _, e ->
                expanded = !expanded
                onToggled(expanded)
                e.consume()
            },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5),
        ) {
            CollapsibleArrow(isExpanded = expanded)
            Text(text = title.copy().withStyle(Style.EMPTY.withUnderlined(true)))
        }

        if (expanded) {
            Row(horizontalArrangement = Arrangement.spacedBy(5)) {
                Spacer(
                    modifier = Modifier
                        .then(PaddingModifier(PaddingValues(left = 5)))
                        .size(1, 0)
                        .fillMaxHeight()
                        .then(BackgroundModifier(KColor.GRAY.argb, KColor.GRAY.argb))
                )
                Box(modifier = Modifier.then(PaddingModifier(PaddingValues(left = 5)))) {
                    content()
                }
            }
        }
    }
}

/** Animated arrow icon that rotates when the collapsible section opens or closes. */
@Composable
private fun CollapsibleArrow(isExpanded: Boolean) {
    var rotation by remember { mutableStateOf(if (isExpanded) 90f else 0f) }
    val target = if (isExpanded) 90f else 0f

    LaunchedEffect(target) {
        while (abs(target - rotation) > 0.1f) {
            rotation += (target - rotation) * 0.25f
            delay(16)
        }
        rotation = target
    }

    Layout(
        name = "CollapsibleArrow",
        measurePolicy = { _, _, _ -> MeasureResult(8, 8) {} },
        renderer = object : Renderer {
            override fun render(
                node: AUINode, x: Int, y: Int,
                guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float,
            ) {
                guiGraphics.pose().pushPose()
                guiGraphics.pose().translate(x + node.width / 2f, y + node.height / 2f, 0f)
                guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(rotation))
                guiGraphics.pose().translate(-(x + node.width / 2f), -(y + node.height / 2f), 0f)
                guiGraphics.drawString(Minecraft.getInstance().font, ">", x + 1, y, KColor.WHITE.argb)
                guiGraphics.pose().popPose()
            }
        },
    )
}
