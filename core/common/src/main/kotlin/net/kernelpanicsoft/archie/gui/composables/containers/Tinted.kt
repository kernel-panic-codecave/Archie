package net.kernelpanicsoft.archie.gui.composables.containers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.mojang.blaze3d.systems.RenderSystem
import net.kernelpanicsoft.archie.gui.layout.Alignment
import net.kernelpanicsoft.archie.gui.layout.BoxMeasurePolicy
import net.kernelpanicsoft.archie.gui.layout.Layout
import net.kernelpanicsoft.archie.gui.layout.Renderer
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.nodes.UINode
import net.minecraft.client.gui.GuiGraphics

/**
 * The currently active ambient tint, as a stack of packed ARGB ints (bottom entry always
 * `0xFFFFFFFF`, fully opaque white/no tint - a sentinel [Tinted] never pops, so `last()` is always
 * safe to read even with no [Tinted] wrapper active at all anywhere in the current frame).
 * [RenderSystem]'s own shader color is global GPU state, not scoped to any one node or
 * composition, so a matching module-level stack - rather than a plain "set then reset to opaque"
 * pair - is what makes nested [Tinted]s (an already-tinted subtree containing another [Tinted] of
 * its own) compose correctly: the inner one's own `renderAfterChildren` restores the *outer*
 * tint it actually interrupted, not an unconditional opaque reset that would incorrectly clobber
 * it. Safe as plain mutable (not thread-confined) state since Minecraft's own rendering is
 * strictly single-threaded and render/renderAfterChildren always nest correctly by construction
 * (the same reason [LayoutNode][net.kernelpanicsoft.archie.gui.layout.LayoutNode]'s own recursive
 * render walk needs no locking either).
 */
private val tintStack = ArrayDeque<Int>().apply { addLast(-1) }

/** [color]'s own red/green/blue/alpha channels as `0f..1f` floats, in that order. */
private fun Int.toRgbaFloats(): FloatArray = floatArrayOf(
	((this ushr 16) and 0xFF) / 255f,
	((this ushr 8) and 0xFF) / 255f,
	(this and 0xFF) / 255f,
	((this ushr 24) and 0xFF) / 255f,
)

/** [a]'s own channels multiplied by [b]'s own, re-packed as ARGB - how a nested [Tinted] composes with whatever ambient tint it's already inside. */
private fun multiplyArgb(a: Int, b: Int): Int {
	val af = a.toRgbaFloats()
	val bf = b.toRgbaFloats()
	fun mul(i: Int) = ((af[i] * bf[i]) * 255f).toInt().coerceIn(0, 255)
	return (mul(3) shl 24) or (mul(0) shl 16) or (mul(1) shl 8) or mul(2)
}

/**
 * Tints [content]'s own entire rendered subtree - background sprites, text, everything it draws,
 * not merely something drawn over top of it - by [tint] (a packed ARGB int; `-1`/`0xFFFFFFFF` is
 * fully opaque white, i.e. no tint at all), via [RenderSystem.setShaderColor]. This is what makes
 * every themed composable's own [net.kernelpanicsoft.archie.gui.util.extension.drawThemeState]
 * call automatically tint-aware (it reads back whatever [RenderSystem.getShaderColor] currently
 * has active) without each one needing to know or care that fading/greying is even happening -
 * the same reason vanilla's own hurt-flash/fade-to-black effects use this exact mechanism rather
 * than drawing an overlay quad, which could only ever dim/tint what's underneath, never genuinely
 * desaturate or fade something translucent-looking in and out.
 *
 * [tint] is re-evaluated fresh every render frame, not memoized against recomposition - a caller
 * wanting a continuously time-varying tint (a pulse, say) can simply read
 * [System.currentTimeMillis] inside it directly, the same pattern every other time-driven visual
 * in this GUI framework already uses (a connector's own flow/wave phase, say), since a composable
 * body only re-runs on state change, not every frame, but a `Renderer.render` callback does.
 *
 * Nests correctly via [tintStack] - an already-tinted subtree containing another [Tinted]
 * multiplies the two together ([multiplyArgb]) rather than one clobbering the other, and each
 * [Tinted]'s own `renderAfterChildren` restores exactly the ambient tint it interrupted, not an
 * unconditional reset to opaque. [RenderSystem]'s own shader color is global GPU state, so an
 * explicit [GuiGraphics.flush] both before changing it and after restoring it is required, not
 * optional - without the first, anything already queued (but not yet actually rasterized) from
 * an earlier sibling would unexpectedly pick up this node's own tint once its buffer's own later
 * flush finally runs; without the second, this node's own tint would just as unexpectedly leak
 * onto whatever's queued right after it. The exact same class of hazard (Mojang's buffered
 * [MultiBufferSource][net.minecraft.client.renderer.MultiBufferSource] flushing whatever it's
 * holding in its own RenderType-keyed order, not necessarily the order draw calls were issued in)
 * that made an explicit flush the fix for the modal backdrop dimming its own dialog panel instead
 * of just what's behind it - see [net.kernelpanicsoft.archie.gui.ComposeContainerScreen]'s own
 * `render`.
 */
@Composable
fun Tinted(tint: () -> Int, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
	val measurePolicy = remember { BoxMeasurePolicy(Alignment.TopStart) }
	Layout(
		name = "Tinted",
		measurePolicy = measurePolicy,
		renderer = object : Renderer {
			override fun render(node: UINode, x: Int, y: Int, guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
				val combined = multiplyArgb(tintStack.last(), tint())
				tintStack.addLast(combined)
				if (combined != tintStack[tintStack.size - 2]) {
					guiGraphics.flush()
					val rgba = combined.toRgbaFloats()
					RenderSystem.setShaderColor(rgba[0], rgba[1], rgba[2], rgba[3])
				}
			}

			override fun renderAfterChildren(node: UINode, x: Int, y: Int, guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
				val popped = tintStack.removeLast()
				val restored = tintStack.last()
				if (popped != restored) {
					guiGraphics.flush()
					val rgba = restored.toRgbaFloats()
					RenderSystem.setShaderColor(rgba[0], rgba[1], rgba[2], rgba[3])
				}
			}
		},
		modifier = modifier,
		content = content,
	)
}
