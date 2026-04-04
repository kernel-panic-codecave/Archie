package net.kernelpanicsoft.archie.gui.composables.containers

import androidx.compose.runtime.*
import com.mojang.math.Axis
import net.kernelpanicsoft.archie.gui.animation.AnimationSpec
import net.kernelpanicsoft.archie.gui.animation.Easings
import net.kernelpanicsoft.archie.gui.animation.animateFloat
import net.kernelpanicsoft.archie.gui.animation.animateInt
import net.kernelpanicsoft.archie.gui.composables.basic.Spacer
import net.kernelpanicsoft.archie.gui.composables.basic.Text
import net.kernelpanicsoft.archie.gui.layout.*
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.modifiers.appearance.BackgroundModifier
import net.kernelpanicsoft.archie.gui.modifiers.fillMaxHeight
import net.kernelpanicsoft.archie.gui.modifiers.input.PointerEventType
import net.kernelpanicsoft.archie.gui.modifiers.input.onPointerEvent
import net.kernelpanicsoft.archie.gui.modifiers.position.PaddingModifier
import net.kernelpanicsoft.archie.gui.modifiers.position.PaddingValues
import net.kernelpanicsoft.archie.gui.modifiers.position.offset
import net.kernelpanicsoft.archie.gui.modifiers.size
import net.kernelpanicsoft.archie.gui.nodes.AUINode
import net.kernelpanicsoft.archie.gui.util.KColor
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import kotlin.math.roundToInt

private const val COLLAPSIBLE_VISIBILITY_EPSILON = 0.01f

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
    val expandProgress = animateFloat(
        targetValue = if (expanded) 1f else 0f,
        spec = AnimationSpec(durationMillis = 220, easing = Easings.OutCubic),
    )

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

        if (expanded || expandProgress > COLLAPSIBLE_VISIBILITY_EPSILON) {
            Layout(
                name = "CollapsibleContent",
                measurePolicy = { _, measurables, constraints ->
                    if (measurables.isEmpty()) return@Layout MeasureResult(0, 0) {}

                    val placeable = measurables.first().measure(
                        constraints.copy(minHeight = 0, maxHeight = Int.MAX_VALUE),
                    )
                    val visibleHeight = (placeable.height * expandProgress)
                        .roundToInt()
                        .coerceAtLeast(0)

                    MeasureResult(placeable.width, visibleHeight) {
                        placeable.placeAt(0, 0)
                    }
                },
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
                        guiGraphics.enableScissor(x, y, x + node.width, y + node.height)
                    }

                    override fun renderAfterChildren(
                        node: AUINode,
                        x: Int,
                        y: Int,
                        guiGraphics: GuiGraphics,
                        mouseX: Int,
                        mouseY: Int,
                        partialTick: Float,
                    ) {
                        guiGraphics.disableScissor()
                    }
                },
            ) {
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
}

/** Animated arrow icon that rotates when the collapsible section opens or closes. */
@Composable
private fun CollapsibleArrow(isExpanded: Boolean) {
    val rotation = animateFloat(
        targetValue = if (isExpanded) 90f else 0f,
        spec = AnimationSpec(durationMillis = 260, easing = Easings.OutBack),
    )

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
