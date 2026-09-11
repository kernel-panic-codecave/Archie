package net.kernelpanicsoft.archie.gui.composables.basic

import androidx.compose.runtime.Composable
import dev.architectury.fluid.FluidStack
import net.kernelpanicsoft.archie.gui.composables.theme.TextureStates
import net.kernelpanicsoft.archie.gui.layout.Layout
import net.kernelpanicsoft.archie.gui.layout.MeasureResult
import net.kernelpanicsoft.archie.gui.layout.Renderer
import dev.architectury.hooks.fluid.FluidStackHooks
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.modifiers.sizeIn
import net.kernelpanicsoft.archie.gui.nodes.UINode
import net.kernelpanicsoft.archie.gui.theme.LocalTheme
import net.kernelpanicsoft.archie.gui.theme.ThemeVariants
import net.kernelpanicsoft.archie.gui.util.extension.drawThemeState
import net.kernelpanicsoft.archie.gui.util.extension.invoke
import net.kernelpanicsoft.archie.gui.util.extension.scissor
import net.minecraft.client.gui.GuiGraphics

private const val FLUID_TANK_MIN_WIDTH = 18
private const val FLUID_TANK_MIN_HEIGHT = 54
private const val FLUID_TANK_INSET = 1

/**
 * A themed fluid-level indicator (looked up in the current theme as `"fluid_tank"`): a tank
 * frame sprite with the real fluid texture and tint (via [FluidStackHooks]) filling it bottom-up to
 * `fluid.amount / capacity`.
 *
 * The fluid sprite is **tiled** at its own size, anchored to the bottom of the tank, and clipped
 * with a scissor at the waterline. Stretching one 16px texture over a tank several times taller
 * smears it into vertical streaks that read as a gradient rather than as a fluid. See
 * The appearance comes from Architectury's [FluidStackHooks] rather than from either loader's own
 * API: Fabric's `FluidRenderHandlerRegistry` and NeoForge's `IClientFluidTypeExtensions` expose the
 * same two facts through unrelated types, and Architectury already bridges them.
 *
 * @param fluid    The fluid and amount to display; an empty stack renders just the tank frame.
 * @param capacity The tank's total capacity; a non-positive value renders as empty rather than
 *   dividing by zero.
 * @param modifier Additional modifiers applied to the outer container.
 * @param variant  The theme variant used for the tank frame texture.
 */
@Composable
fun FluidTank(
	fluid: FluidStack,
	capacity: Long,
	modifier: Modifier = Modifier,
	variant: String = ThemeVariants.DEFAULT,
)
{
	val theme = LocalTheme.current.getComposableTheme("fluid_tank")
	val sizeModifier = Modifier.sizeIn(minWidth = FLUID_TANK_MIN_WIDTH, minHeight = FLUID_TANK_MIN_HEIGHT)

	Layout(
		name = "FluidTank",
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

				if (fluid.isEmpty || capacity <= 0L) return@guiGraphics

				val fraction = (fluid.amount.toDouble() / capacity.toDouble()).coerceIn(0.0, 1.0).toFloat()
				val sprite = FluidStackHooks.getStillTexture(fluid.fluid) ?: return@guiGraphics

				val innerX = x + FLUID_TANK_INSET
				val innerY = y + FLUID_TANK_INSET
				val innerW = (node.width - FLUID_TANK_INSET * 2).coerceAtLeast(0)
				val innerH = (node.height - FLUID_TANK_INSET * 2).coerceAtLeast(0)
				val fillH = (innerH * fraction).toInt()
				val fillY = innerY + innerH - fillH

				if (innerW <= 0 || fillH <= 0) return@guiGraphics

				val tint = FluidStackHooks.getColor(fluid.fluid)
				val a = ((tint ushr 24) and 0xFF) / 255f
				val r = ((tint ushr 16) and 0xFF) / 255f
				val g = ((tint ushr 8) and 0xFF) / 255f
				val b = (tint and 0xFF) / 255f

				// Tiled at the sprite's own size rather than stretched over the interior: a tank is
				// usually much taller than the 16px texture, and stretching smears it into vertical
				// streaks that read as a gradient rather than as a fluid.
				val tileWidth = sprite.contents().width()
				val tileHeight = sprite.contents().height()
				if (tileWidth <= 0 || tileHeight <= 0) return@guiGraphics

				val bottom = innerY + innerH
				scissor(innerX, fillY, innerX + innerW, bottom) {
					// Anchored to the bottom so the tiling stays put as the level moves - anchoring
					// to the surface instead would slide the whole pattern on every change. The
					// scissor trims the partial tiles at the waterline and the right edge, so the
					// loops can overrun both.
					// Vanilla's blit order is (x, y, blitOffset, width, height, sprite, ...).
					var tileY = bottom - tileHeight
					while (tileY + tileHeight > fillY) {
						var tileX = innerX
						while (tileX < innerX + innerW) {
							blit(tileX, tileY, 0, tileWidth, tileHeight, sprite, r, g, b, a)
							tileX += tileWidth
						}
						tileY -= tileHeight
					}
				}
			}
		},
	)
}
