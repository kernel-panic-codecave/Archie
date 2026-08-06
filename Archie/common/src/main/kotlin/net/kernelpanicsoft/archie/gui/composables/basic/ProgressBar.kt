package net.kernelpanicsoft.archie.gui.composables.basic

import androidx.compose.runtime.Composable
import net.kernelpanicsoft.archie.gui.composables.theme.TextureStates
import net.kernelpanicsoft.archie.gui.layout.Layout
import net.kernelpanicsoft.archie.gui.layout.MeasureResult
import net.kernelpanicsoft.archie.gui.layout.Renderer
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.modifiers.sizeIn
import net.kernelpanicsoft.archie.gui.nodes.UINode
import net.kernelpanicsoft.archie.gui.theme.LocalTheme
import net.kernelpanicsoft.archie.gui.theme.ThemeVariants
import net.kernelpanicsoft.archie.gui.util.extension.drawThemeState
import net.kernelpanicsoft.archie.gui.util.extension.invoke
import net.minecraft.client.gui.GuiGraphics

internal const val FILL_BAR_MIN_WIDTH = 90
internal const val FILL_BAR_MIN_HEIGHT = 16

/** Which edge of a [ProgressBar]/[net.kernelpanicsoft.archie.gui.composables.basic.EnergyBar] the fill grows from. */
enum class ProgressDirection
{
	LEFT_TO_RIGHT,
	RIGHT_TO_LEFT,
	TOP_TO_BOTTOM,
	BOTTOM_TO_TOP,
}

/**
 * Shared rendering core for [ProgressBar] and [net.kernelpanicsoft.archie.gui.composables.basic.EnergyBar]:
 * a themed track sprite (looked up as [themeName] in the current theme) filled with a solid
 * color up to [progress].
 */
@Composable
internal fun ThemedFillBar(
	themeName: String,
	progress: Float,
	modifier: Modifier,
	direction: ProgressDirection,
	fillColor: Int,
	variant: String,
)
{
	val clamped = progress.coerceIn(0f, 1f)
	val theme = LocalTheme.current.getComposableTheme(themeName)
	val sizeModifier = Modifier.sizeIn(minWidth = FILL_BAR_MIN_WIDTH, minHeight = FILL_BAR_MIN_HEIGHT)

	Layout(
		name = themeName,
		measurePolicy = { _, _, constraints -> MeasureResult(constraints.minWidth, constraints.minHeight) {} },
		modifier = sizeModifier.then(modifier),
		renderer = object : Renderer
		{
			override fun render(
				node: UINode, x: Int, y: Int,
				guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float,
			) = guiGraphics {
				val state = theme.getState(TextureStates.DEFAULT, variant)
				drawThemeState(state, x, y, node.width, node.height)

				var fx = x
				var fy = y
				var fw = node.width
				var fh = node.height
				when (direction)
				{
					ProgressDirection.LEFT_TO_RIGHT -> fw = (node.width * clamped).toInt()
					ProgressDirection.RIGHT_TO_LEFT ->
					{
						fw = (node.width * clamped).toInt()
						fx = x + node.width - fw
					}

					ProgressDirection.TOP_TO_BOTTOM -> fh = (node.height * clamped).toInt()
					ProgressDirection.BOTTOM_TO_TOP ->
					{
						fh = (node.height * clamped).toInt()
						fy = y + node.height - fh
					}
				}
				if (fw > 0 && fh > 0) fill(fx, fy, fx + fw, fy + fh, fillColor)
			}
		},
	)
}

/**
 * A themed linear progress indicator: an empty-track sprite from the current theme (looked up as
 * `"progress_bar"`), filled with a solid color up to [progress].
 *
 * There's no built-in animation or recomposition trigger here - drive [progress] from an
 * observed block entity field (see [net.kernelpanicsoft.archie.gui.blockentity.observeProperty])
 * for a live machine-processing indicator.
 *
 * @param progress  Fraction complete, clamped to `0f..1f`.
 * @param modifier  Additional modifiers applied to the outer container.
 * @param direction Which edge the fill grows from.
 * @param fillColor ARGB color of the filled portion.
 * @param variant   The theme variant used for the track texture.
 */
@Composable
fun ProgressBar(
	progress: Float,
	modifier: Modifier = Modifier,
	direction: ProgressDirection = ProgressDirection.LEFT_TO_RIGHT,
	fillColor: Int = 0xFF6BA8FF.toInt(),
	variant: String = ThemeVariants.DEFAULT,
) = ThemedFillBar("progress_bar", progress, modifier, direction, fillColor, variant)
