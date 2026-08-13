package net.kernelpanicsoft.archie.gui.composables.input

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import net.kernelpanicsoft.archie.gui.animation.AnimationSpec
import net.kernelpanicsoft.archie.gui.animation.Easings
import net.kernelpanicsoft.archie.gui.animation.animateInt
import net.kernelpanicsoft.archie.gui.composables.theme.TextureStates
import net.kernelpanicsoft.archie.gui.composables.theme.WidgetState
import net.kernelpanicsoft.archie.gui.interaction.MutableInteractionSource
import net.kernelpanicsoft.archie.gui.interaction.collectIsFocusedAsState
import net.kernelpanicsoft.archie.gui.interaction.collectIsHoveredAsState
import net.kernelpanicsoft.archie.gui.layout.BoxMeasurePolicy
import net.kernelpanicsoft.archie.gui.layout.Layout
import net.kernelpanicsoft.archie.gui.layout.Renderer
import net.kernelpanicsoft.archie.gui.layout.Alignment
import net.kernelpanicsoft.archie.gui.layout.Size
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.modifiers.input.toggleable
import net.kernelpanicsoft.archie.gui.modifiers.sizeIn
import net.kernelpanicsoft.archie.gui.nodes.UINode
import net.kernelpanicsoft.archie.gui.theme.ComposableTheme
import net.kernelpanicsoft.archie.gui.theme.LocalTheme
import net.kernelpanicsoft.archie.gui.theme.ThemeVariants
import net.kernelpanicsoft.archie.gui.util.extension.drawThemeState
import net.kernelpanicsoft.archie.gui.util.extension.invoke
import net.minecraft.client.gui.GuiGraphics
import kotlin.time.Duration.Companion.milliseconds

/** Fallback track size when the "switch_track" theme doesn't declare its own [ComposableTheme.minSize]. */
private const val SWITCH_MIN_WIDTH = 34
private const val SWITCH_MIN_HEIGHT = 18
/** Fallback gap between the thumb and the track's edge, when "switch_track" doesn't declare its own [ComposableTheme.contentPadding]. */
private const val SWITCH_PADDING = 2
/** Fallback (square) thumb size when the "switch_thumb" theme doesn't declare its own [ComposableTheme.minSize]. */
private const val SWITCH_THUMB_SIZE = 14

/** Clamps a raw thumb x-offset so the [thumbWidth]-wide thumb stays within the track, respecting [padding]. */
internal fun resolveSwitchThumbOffset(thumbOffset: Int, trackWidth: Int, thumbWidth: Int = SWITCH_THUMB_SIZE, padding: Int = SWITCH_PADDING): Int {
    val minOffset = padding
    val maxOffset = (trackWidth - thumbWidth - padding).coerceAtLeast(minOffset)
    return thumbOffset.coerceIn(minOffset, maxOffset)
}

/**
 * Low-level switch primitive exposing hover/focus state and checked state to custom visuals.
 * Built on [Modifier.toggleable], so it's a vanilla keyboard/controller focus-navigation stop
 * that toggles on Enter/Space while focused, the same as a mouse click.
 */
@Composable
fun SwitchCore(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    content: @Composable (modifier: Modifier, isHovered: Boolean, isFocused: Boolean, checked: Boolean) -> Unit,
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

    content(toggleableModifier, isHovered, isFocused, checked)
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
    // A theme's track/thumb art isn't always native to SWITCH_MIN_WIDTH/HEIGHT and
    // SWITCH_THUMB_SIZE's proportions (tuned for the default look) - min_size on the
    // "switch_track"/"switch_thumb" theme entries lets a theme declare its own real dimensions
    // instead of getting force-stretched into ones it wasn't designed for. Likewise,
    // content_padding's horizontal component overrides how far the thumb sits from the track's
    // edge - some art (e.g. a track drawn flush to its own bounds) wants the thumb sitting
    // right in the corner instead of inset by the default gap.
    val trackSize = trackTheme.minSize ?: Size(SWITCH_MIN_WIDTH, SWITCH_MIN_HEIGHT)
    val thumbSize = thumbTheme.minSize ?: Size(SWITCH_THUMB_SIZE, SWITCH_THUMB_SIZE)
    val padding = trackTheme.contentPadding?.horizontal ?: SWITCH_PADDING
    val sizeModifier = Modifier.sizeIn(minWidth = trackSize.width, minHeight = trackSize.height)
    val thumbOffset = animateInt(
        targetValue = if (checked) trackSize.width - thumbSize.width - padding else padding,
        spec = AnimationSpec(durationMillis = 140.milliseconds, easing = Easings.OutCubic),
    )

    SwitchCore(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
    ) { switchModifier, hovered, focused, currentChecked ->
        Layout(
            name = "Switch",
            measurePolicy = measurePolicy,
            modifier = switchModifier.then(sizeModifier).then(modifier),
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
                        WidgetState.clicked(currentChecked), WidgetState.focused(hovered || focused),
                        enabled = enabled,
                    )
                    node.renderState = trackStateKey
                    val trackState = trackTheme.getState(trackStateKey, variant)
                    val thumbState = thumbTheme.getState(
                        WidgetState.resolve(thumbTheme, variant, enabled = enabled),
                        variant
                    )

                    drawThemeState(trackState, x, y, node.width, node.height)

                    val thumbX = x + resolveSwitchThumbOffset(thumbOffset, node.width, thumbSize.width, padding)
                    val thumbY = y + ((node.height - thumbSize.height) / 2)
                    drawThemeState(thumbState, thumbX, thumbY, thumbSize.width, thumbSize.height)
                }
            },
        )
    }
}

