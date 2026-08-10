package net.kernelpanicsoft.archie.gui.composables.input

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import net.kernelpanicsoft.archie.gui.animation.AnimationSpec
import net.kernelpanicsoft.archie.gui.animation.Easings
import net.kernelpanicsoft.archie.gui.animation.animateInt
import net.kernelpanicsoft.archie.gui.composables.theme.TextureStates
import net.kernelpanicsoft.archie.gui.composables.theme.WidgetState
import net.kernelpanicsoft.archie.gui.layout.BoxMeasurePolicy
import net.kernelpanicsoft.archie.gui.layout.Layout
import net.kernelpanicsoft.archie.gui.layout.Renderer
import net.kernelpanicsoft.archie.gui.layout.Alignment
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.modifiers.sizeIn
import net.kernelpanicsoft.archie.gui.nodes.UINode
import net.kernelpanicsoft.archie.gui.theme.LocalTheme
import net.kernelpanicsoft.archie.gui.theme.ThemeVariants
import net.kernelpanicsoft.archie.gui.util.extension.drawThemeState
import net.kernelpanicsoft.archie.gui.util.extension.invoke
import net.minecraft.client.gui.GuiGraphics
import kotlin.time.Duration.Companion.milliseconds

private const val SWITCH_MIN_WIDTH = 34
private const val SWITCH_MIN_HEIGHT = 18
private const val SWITCH_PADDING = 2
private const val SWITCH_THUMB_SIZE = 14

/** Clamps a raw thumb x-offset so the thumb stays within the track, respecting [SWITCH_PADDING]. */
internal fun resolveSwitchThumbOffset(thumbOffset: Int, trackWidth: Int): Int {
    val minOffset = SWITCH_PADDING
    val maxOffset = (trackWidth - SWITCH_THUMB_SIZE - SWITCH_PADDING).coerceAtLeast(minOffset)
    return thumbOffset.coerceIn(minOffset, maxOffset)
}

/**
 * Low-level switch primitive exposing hover/press state and checked state to custom visuals.
 */
@Composable
fun SwitchCore(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable (isHovered: Boolean, isPressed: Boolean, checked: Boolean) -> Unit,
) {
    Clickable(
        onClick = { onCheckedChange(!checked) },
        enabled = enabled,
        modifier = modifier,
    ) { hovered, pressed ->
        content(hovered, pressed, checked)
    }
}

/**
 * Simple styled switch control suitable for toggling boolean settings.
 *
 * Renders the themed [trackTexture] state - a combined checked+hovered state is used when
 * both apply and the theme defines it - with the themed [thumbTexture] drawn on top,
 * animating between its off/on positions.
 *
 * @param checked        The current checked state.
 * @param onCheckedChange Called with the new checked value when the user clicks.
 * @param modifier       Additional modifiers applied to the outer container.
 * @param enabled        When `false`, pointer events are ignored and the [TextureStates.DISABLED] state is shown.
 * @param trackTexture   The themed texture key for the track, looked up via [LocalTheme].
 * @param thumbTexture   The themed texture key for the thumb, looked up via [LocalTheme].
 * @param variant        The theme variant of both textures to use. See [ThemeVariants].
 */
@Composable
fun Switch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    trackTexture: String = "switch_track",
    thumbTexture: String = "switch_thumb",
    variant: String = ThemeVariants.DEFAULT,
) {
    val theme = LocalTheme.current
    val trackTheme = theme.getComposableTheme(trackTexture)
    val thumbTheme = theme.getComposableTheme(thumbTexture)
    val measurePolicy = remember { BoxMeasurePolicy(Alignment.CenterStart) }
    val sizeModifier = Modifier.sizeIn(minWidth = SWITCH_MIN_WIDTH, minHeight = SWITCH_MIN_HEIGHT)
    val thumbOffset = animateInt(
        targetValue = if (checked) SWITCH_MIN_WIDTH - SWITCH_THUMB_SIZE - SWITCH_PADDING else SWITCH_PADDING,
        spec = AnimationSpec(durationMillis = 140.milliseconds, easing = Easings.OutCubic),
    )

    SwitchCore(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        modifier = sizeModifier.then(modifier),
    ) { hovered, _, currentChecked ->
        Layout(
            name = "Switch",
            measurePolicy = measurePolicy,
            modifier = sizeModifier,
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
                    val trackStateKey = WidgetState.resolve(
                        trackTheme, variant,
                        WidgetState.clicked(currentChecked), WidgetState.hovered(hovered),
                        enabled = enabled,
                    )
                    node.renderState = trackStateKey
                    val trackState = trackTheme.getState(trackStateKey, variant)
                    val thumbState = thumbTheme.getState(
                        WidgetState.resolve(thumbTheme, variant, enabled = enabled),
                        variant
                    )

                    drawThemeState(trackState, x, y, node.width, node.height)

                    val thumbX = x + resolveSwitchThumbOffset(thumbOffset, node.width)
                    val thumbY = y + ((node.height - SWITCH_THUMB_SIZE) / 2)
                    drawThemeState(thumbState, thumbX, thumbY, SWITCH_THUMB_SIZE, SWITCH_THUMB_SIZE)
                }
            },
        )
    }
}

