package net.kernelpanicsoft.archie.gui.composables.containers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import net.kernelpanicsoft.archie.gui.layout.Alignment
import net.kernelpanicsoft.archie.gui.layout.Layout
import net.kernelpanicsoft.archie.gui.layout.MeasurePolicy
import net.kernelpanicsoft.archie.gui.layout.MeasureResult
import net.kernelpanicsoft.archie.gui.layout.Renderer
import net.kernelpanicsoft.archie.gui.modifiers.Constraints
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.modifiers.input.onDrag
import net.kernelpanicsoft.archie.gui.modifiers.input.onScroll
import net.kernelpanicsoft.archie.gui.modifiers.onGloballyPositioned
import net.kernelpanicsoft.archie.gui.nodes.UINode
import net.kernelpanicsoft.archie.gui.util.extension.blitTinted
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.resources.ResourceLocation

/**
 * Mutable pan/zoom state for a [PannableCanvas] - create via [rememberPannableCanvasState] to
 * survive recomposition.
 */
@Stable
class PannableCanvasState {
	var panX by mutableStateOf(0)
	var panY by mutableStateOf(0)

	/** Current zoom factor, clamped to [MIN_ZOOM]/[MAX_ZOOM] - `1f` is unzoomed. Set via [zoomBy], not directly, so a change always keeps [zoomBy]'s own cursor-anchored point stationary rather than silently re-centering the view. */
	var zoom by mutableStateOf(1f)
		private set

	/** This canvas's own absolute screen position, tracked via [Modifier.onGloballyPositioned] each layout pass - [zoomBy] needs it to convert a cursor's screen position into this canvas's own local space. */
	internal var originX = 0
	internal var originY = 0

	/** This canvas's own fixed viewport size in screen pixels, captured each render pass - together with [originX]/[originY], the visible window a caller (see [NodeTreeView]'s own `onVisibilityChanged`) checks a content-space rectangle against to know whether it's actually scrolled into view right now. */
	internal var viewportWidth = 0
	internal var viewportHeight = 0

	/**
	 * Adjusts [zoom] by [delta] (a scroll wheel notch, typically), keeping whatever content point
	 * currently sits under ([cursorScreenX],[cursorScreenY]) - absolute screen coordinates, e.g. a
	 * [net.kernelpanicsoft.archie.gui.modifiers.input.ScrollEvent]'s own `mouseX`/`mouseY` -
	 * visually stationary, the same "zoom toward the cursor" behavior a map or image viewer uses,
	 * rather than always zooming around the canvas's own top-left corner.
	 */
	fun zoomBy(delta: Float, cursorScreenX: Int, cursorScreenY: Int) {
		val newZoom = (zoom + delta).coerceIn(MIN_ZOOM, MAX_ZOOM)
		if (newZoom == zoom) return
		val localX = cursorScreenX - originX - panX
		val localY = cursorScreenY - originY - panY
		val contentX = localX / zoom
		val contentY = localY / zoom
		panX -= ((contentX * newZoom) - localX).toInt()
		panY -= ((contentY * newZoom) - localY).toInt()
		zoom = newZoom
	}

	companion object {
		const val MIN_ZOOM = 0.4f
		const val MAX_ZOOM = 2.5f

		/** [zoomBy]'s own default step per scroll notch - see [PannableCanvas]'s own `onScroll` wiring. */
		const val ZOOM_STEP = 0.15f
	}
}

@Composable
fun rememberPannableCanvasState(): PannableCanvasState = remember { PannableCanvasState() }

/**
 * A fixed-size viewport ([modifier]'s own size) over a single, arbitrarily large child, panned by
 * click-dragging anywhere inside it - the same "pan around a graph" interaction the vanilla
 * advancements screen (and Thaumcraft's Thaumonomicon, and quest-book mods generally) uses, built
 * on the same [Layout]/[Renderer] primitives [Scrollable] is, minus the scrollbar/lerp animation
 * ([NodeTreeView]'s node graph pans 1:1 with the drag, not eased). Content outside the viewport is
 * clipped via a scissor, matching how [Scrollable] hides its own overflowed content.
 *
 * The scroll wheel adjusts [PannableCanvasState.zoom] by [PannableCanvasState.ZOOM_STEP] per
 * notch, anchored on the cursor (see [PannableCanvasState.zoomBy]) - [content] itself decides what
 * "zoom" actually means for whatever it draws ([PannableCanvas] only owns the shared `zoom`
 * number/pan-math, not any particular rendering of it); [NodeTreeView] is the one consumer that
 * currently does anything with it.
 *
 * @param backgroundTexture When given, tiled across the whole viewport and panned alongside
 *   [content] (the same "infinite paper" backdrop Thaumonomicon-style guide/skill-tree screens use,
 *   analogous to vanilla's own repeating dirt background) - a plain, unpanned background belongs on
 *   [modifier] instead (e.g. via [Panel]/[Surface]), since a texture given here specifically moves.
 * @param backgroundTextureSize [backgroundTexture]'s own tile size in pixels (it's sampled with
 *   wrapping, so it must actually tile at this size - 32 matches vanilla's `options_background`).
 * @param backgroundTint Packed ARGB tint applied to [backgroundTexture] - `-1` draws it unmodified.
 * @param backgroundParallax How fast [backgroundTexture] pans relative to [content] - `1f` is
 *   exact lockstep, below that trails behind (a parallax depth cue), `0f` pins it in place.
 * @param panelTexture/[panelVariant] A themed [Surface] frame drawn over the whole viewport (see
 *   [Surface]'s own `drawOverContent`), wrapping every [PannableCanvas] in a bordered panel by
 *   default. [modifier] applies to this outer frame. Pass `panelVariant = null` to opt out.
 * @param panelContentPadding How far the pannable viewport sits inset from [panelVariant]'s own
 *   edge, so its own corners don't peek through the border's transparent rounded corners. Ignored
 *   when [panelVariant] is `null`. Defaults to this theme's own nine-slice border thickness.
 */
@Composable
fun PannableCanvas(
	modifier: Modifier = Modifier,
	state: PannableCanvasState = rememberPannableCanvasState(),
	backgroundTexture: ResourceLocation? = null,
	backgroundTextureSize: Int = 32,
	backgroundTint: Int = -1,
	backgroundParallax: Float = 1f,
	panelTexture: String = "surface",
	panelVariant: String? = "inset_transparent",
	panelContentPadding: Int = 2,
	content: @Composable () -> Unit,
) {
	val inset = if (panelVariant != null) panelContentPadding else 0
	val measurePolicy = remember(inset) {
		MeasurePolicy { _, measurables, constraints ->
			val placeable = measurables.firstOrNull()?.measure(Constraints(minWidth = 0, maxWidth = Int.MAX_VALUE, minHeight = 0, maxHeight = Int.MAX_VALUE))
			val rawWidth = if (constraints.maxWidth != Int.MAX_VALUE) constraints.maxWidth else (placeable?.width ?: 0)
			val rawHeight = if (constraints.maxHeight != Int.MAX_VALUE) constraints.maxHeight else (placeable?.height ?: 0)
			val width = (rawWidth - inset * 2).coerceAtLeast(0)
			val height = (rawHeight - inset * 2).coerceAtLeast(0)
			MeasureResult(width, height) {
				placeable?.placeAt(state.panX, state.panY)
			}
		}
	}

	val canvas: @Composable (Modifier) -> Unit = remember {{ canvasModifier ->
		Layout(
			name = "PannableCanvas",
			measurePolicy = measurePolicy,
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
				)
				{
					state.viewportWidth = node.width
					state.viewportHeight = node.height
					guiGraphics.enableScissor(x, y, x + node.width, y + node.height)
					if (backgroundTexture != null)
					{
						val uOffset =
							Math.floorMod(-(state.panX * backgroundParallax).toInt(), backgroundTextureSize)
								.toFloat()
						val vOffset =
							Math.floorMod(-(state.panY * backgroundParallax).toInt(), backgroundTextureSize)
								.toFloat()
						if (backgroundTint == -1)
						{
							guiGraphics.blit(
								backgroundTexture,
								x,
								y,
								uOffset,
								vOffset,
								node.width,
								node.height,
								backgroundTextureSize,
								backgroundTextureSize
							)
						}
						else
						{
							val red = ((backgroundTint ushr 16) and 0xFF) / 255f
							val green = ((backgroundTint ushr 8) and 0xFF) / 255f
							val blue = (backgroundTint and 0xFF) / 255f
							val alpha = ((backgroundTint ushr 24) and 0xFF) / 255f
							guiGraphics.blitTinted(
								backgroundTexture,
								x,
								y,
								uOffset,
								vOffset,
								node.width,
								node.height,
								backgroundTextureSize,
								backgroundTextureSize,
								red,
								green,
								blue,
								alpha,
							)
						}
					}
				}

				override fun renderAfterChildren(
					node: UINode,
					x: Int,
					y: Int,
					guiGraphics: GuiGraphics,
					mouseX: Int,
					mouseY: Int,
					partialTick: Float
				)
				{
					guiGraphics.disableScissor()
				}
			},
			modifier = canvasModifier
				.onGloballyPositioned { state.originX = it.x; state.originY = it.y }
				.onDrag<UINode> { _, event -> state.panX += event.dragX.toInt(); state.panY += event.dragY.toInt() }
				.onScroll<UINode> { _, event ->
					state.zoomBy(
						(event.scrollY * PannableCanvasState.ZOOM_STEP).toFloat(),
						event.mouseX.toInt(),
						event.mouseY.toInt()
					)
				},
			content = content,
		)
	}}

	if (panelVariant != null) {
		Surface(modifier = modifier, texture = panelTexture, variant = panelVariant, drawOverContent = true, contentAlignment = Alignment.Center) {
			canvas(Modifier)
		}
	} else {
		canvas(modifier)
	}
}
