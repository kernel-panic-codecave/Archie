package net.kernelpanicsoft.archie.gui.composables.basic

import androidx.compose.runtime.Composable
import net.kernelpanicsoft.archie.gui.layout.Layout
import net.kernelpanicsoft.archie.gui.layout.MeasureResult
import net.kernelpanicsoft.archie.gui.layout.Renderer
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.modifiers.DebugModifier
import net.kernelpanicsoft.archie.gui.nodes.AUINode
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.resources.ResourceLocation

/**
 * Renders a sprite region from a texture atlas using UV coordinates.
 *
 * The composable sizes itself to the minimum constraints provided by its parent and blits
 * the specified region from the texture at [loc] scaled to fill the node's bounds.
 *
 * ### Example
 * ```kotlin
 * Texture(
 *     loc = Archie["textures/gui/widgets.png"],
 *     uOffset = 0f, vOffset = 0f,
 *     u = 16, v = 16,
 *     textureWidth = 256, textureHeight = 256,
 *     modifier = Modifier.size(16, 16),
 * )
 * ```
 *
 * @param loc           Resource location of the texture file.
 * @param uOffset       Horizontal UV start offset within the texture (in texture pixels).
 * @param vOffset       Vertical UV start offset within the texture (in texture pixels).
 * @param u             Width of the source region in texture pixels.
 * @param v             Height of the source region in texture pixels.
 * @param textureWidth  Total width of the texture atlas in pixels.
 * @param textureHeight Total height of the texture atlas in pixels.
 * @param modifier      Additional modifiers applied to the layout node.
 */
@Composable
fun Texture(
    loc: ResourceLocation,
    uOffset: Float,
    vOffset: Float,
    u: Int,
    v: Int,
    textureWidth: Int,
    textureHeight: Int,
    modifier: Modifier = Modifier,
) {
    Layout(
        name = "Texture",
        measurePolicy = { _, _, constraints ->
            MeasureResult(constraints.minWidth, constraints.minHeight) {}
        },
        renderer = object : Renderer {
            override fun render(
                node: AUINode, x: Int, y: Int,
                guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float,
            ) {
                guiGraphics.blit(loc, x, y, node.width, node.height, uOffset, vOffset, u, v, textureWidth, textureHeight)
                super.render(node, x, y, guiGraphics, mouseX, mouseY, partialTick)
            }
        },
        modifier = Modifier.then(DebugModifier(strs = listOf(loc.toString()))).then(modifier),
    )
}
