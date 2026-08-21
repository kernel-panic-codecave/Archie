package net.kernelpanicsoft.archie.gui.util.extension

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferUploader
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexFormat
import net.kernelpanicsoft.archie.gui.layout.IntRect
import net.kernelpanicsoft.archie.gui.theme.SimpleThemeState
import net.kernelpanicsoft.archie.gui.theme.ThemeState
import net.kernelpanicsoft.archie.util.minecraftClient
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.client.resources.metadata.gui.GuiSpriteScaling
import net.minecraft.resources.ResourceLocation

private fun ResourceLocation.isAtlasSprite(): Boolean =
    !path.startsWith("textures/") && !path.endsWith(".png")

/**
 * Draws a ThemeState to the screen - tinted by whatever [RenderSystem.setShaderColor] currently
 * has active ([net.kernelpanicsoft.archie.gui.composables.containers.Tinted] is what sets it),
 * automatically and for every caller: the plain, opaque-white default (no `Tinted` wrapper active
 * anywhere above this call) draws exactly as this always has, and anything else routes through a
 * genuinely different draw call, not just a different pose/uniform state applied on top of the
 * usual one. Vanilla's own atlas-sprite blit path ([GuiGraphics.blitSprite], used below for any
 * atlas-sprite texture, `node_frame` included) draws through an *immediate* [Tesselator] call
 * using a plain, colorless `position_tex` shader - confirmed directly against `GuiGraphics`'s own
 * decompiled source, not assumed - that never samples the `ColorModulator` uniform
 * [RenderSystem.setShaderColor] sets at all, unlike ordinary text rendering (which goes through
 * the normal batched `RenderType` pipeline and does sample it) - the reason a `Tinted` wrapper
 * around, say, a [net.kernelpanicsoft.archie.gui.composables.containers.NodeAnimation] fade would
 * otherwise visibly reach a node's own text but not its themed background. [innerBlitTinted] and
 * friends below port vanilla's own private nine-slice/tile blitting (`GuiGraphics.blitNineSlicedSprite`/
 * `blitTiledSprite`, package-private and therefore unreachable from here) line-for-line, just
 * threading an explicit tint through every one of their own leaf draw calls instead of the
 * colorless one vanilla's own private `innerBlit` overload uses - a plain single-quad stretch
 * would otherwise visibly distort anything nine-sliced (`node_frame`'s own border art, say) for
 * as long as it stays tinted, not just a passing frame or two. A raw (non-atlas)
 * [SimpleThemeState.texture] has no [TextureAtlasSprite]/nine-slice metadata to port a tinted path
 * against, but still reaches [innerBlitTinted] directly via [blitTinted] below - the same UV
 * normalization (`(uOffset + 0) / textureWidth`, etc.) vanilla's own untinted
 * `blit(ResourceLocation, x,y,width,height, uOffset,vOffset,uWidth,vHeight, textureWidth,textureHeight)`
 * overload uses on its own way down to its *own* private `innerBlit`, confirmed directly against
 * `GuiGraphics`'s own decompiled source rather than assumed.
 */
fun GuiGraphics.drawThemeState(state: ThemeState, x: Int, y: Int, width: Int, height: Int) {
    state as SimpleThemeState
    val color = RenderSystem.getShaderColor()
    val tinted = color[0] != 1f || color[1] != 1f || color[2] != 1f || color[3] != 1f
    if (tinted && state.texture.isAtlasSprite()) {
        val sprite = minecraftClient.guiSprites.getSprite(state.texture)
        when (val scaling = minecraftClient.guiSprites.getSpriteScaling(sprite)) {
            is GuiSpriteScaling.NineSlice -> blitNineSlicedSpriteTinted(sprite, scaling, x, y, width, height, color[0], color[1], color[2], color[3])
            is GuiSpriteScaling.Tile -> blitTiledSpriteTinted(sprite, x, y, width, height, 0, 0, scaling.width(), scaling.height(), scaling.width(), scaling.height(), color[0], color[1], color[2], color[3])
            else -> blitSpriteTinted(sprite, x, y, width, height, color[0], color[1], color[2], color[3])
        }
    } else if (tinted) {
        blitTinted(state, x, y, color[0], color[1], color[2], color[3])
    } else if (state.texture.isAtlasSprite()) {
        blitSprite(state.texture, x, y, width, height)
    } else {
        blit(state, x, y)
    }
}

/**
 * Tinted port of vanilla's own private `GuiGraphics.innerBlit(ResourceLocation, x1,x2,y1,y2,
 * blitOffset, minU,maxU,minV,maxV, red,green,blue,alpha)` - the same immediate
 * [Tesselator]/`position_tex_color`-shader approach that overload itself uses (confirmed against
 * `GuiGraphics`'s own decompiled source), reimplemented here only because the real one is
 * package-private and unreachable outside `net.minecraft.client.gui`. [z] stands in for that
 * overload's own `blitOffset`, always `0` from every caller below - matching [drawThemeState]'s
 * own untinted callers, none of which ever pass one either.
 */
private fun GuiGraphics.innerBlitTinted(
    atlasLocation: ResourceLocation,
    x1: Int, x2: Int, y1: Int, y2: Int,
    minU: Float, maxU: Float, minV: Float, maxV: Float,
    red: Float, green: Float, blue: Float, alpha: Float,
    z: Float = 0f,
) {
    RenderSystem.setShaderTexture(0, atlasLocation)
    RenderSystem.setShader(GameRenderer::getPositionTexColorShader)
    RenderSystem.enableBlend()
    val matrix = pose().last().pose()
    val buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR)
    buffer.addVertex(matrix, x1.toFloat(), y1.toFloat(), z).setUv(minU, minV).setColor(red, green, blue, alpha)
    buffer.addVertex(matrix, x1.toFloat(), y2.toFloat(), z).setUv(minU, maxV).setColor(red, green, blue, alpha)
    buffer.addVertex(matrix, x2.toFloat(), y2.toFloat(), z).setUv(maxU, maxV).setColor(red, green, blue, alpha)
    buffer.addVertex(matrix, x2.toFloat(), y1.toFloat(), z).setUv(maxU, minV).setColor(red, green, blue, alpha)
    BufferUploader.drawWithShader(buffer.buildOrThrow())
    RenderSystem.disableBlend()
}

/** Tinted port of vanilla's own private `GuiGraphics.blitSprite(TextureAtlasSprite, textureWidth, textureHeight, uPosition, vPosition, x, y, blitOffset, uWidth, vHeight)` - one sub-region of [sprite]'s own [textureWidth]x[textureHeight] reference frame, mapped onto a [uWidth]x[vHeight] box on screen. */
private fun GuiGraphics.blitSpriteTinted(
    sprite: TextureAtlasSprite, textureWidth: Int, textureHeight: Int, uPosition: Int, vPosition: Int,
    x: Int, y: Int, uWidth: Int, vHeight: Int,
    red: Float, green: Float, blue: Float, alpha: Float,
) {
    if (uWidth != 0 && vHeight != 0) {
        innerBlitTinted(
            sprite.atlasLocation(), x, x + uWidth, y, y + vHeight,
            sprite.getU(uPosition.toFloat() / textureWidth), sprite.getU((uPosition + uWidth).toFloat() / textureWidth),
            sprite.getV(vPosition.toFloat() / textureHeight), sprite.getV((vPosition + vHeight).toFloat() / textureHeight),
            red, green, blue, alpha,
        )
    }
}

/** Tinted port of vanilla's own private `GuiGraphics.blitSprite(TextureAtlasSprite, x, y, blitOffset, width, height)` - [sprite]'s own full extent, stretched to [width]x[height]. */
private fun GuiGraphics.blitSpriteTinted(sprite: TextureAtlasSprite, x: Int, y: Int, width: Int, height: Int, red: Float, green: Float, blue: Float, alpha: Float) {
    if (width != 0 && height != 0) {
        innerBlitTinted(sprite.atlasLocation(), x, x + width, y, y + height, sprite.getU0(), sprite.getU1(), sprite.getV0(), sprite.getV1(), red, green, blue, alpha)
    }
}

/** Tinted port of vanilla's own private `GuiGraphics.blitTiledSprite` - repeats [sprite]'s own [spriteWidth]x[spriteHeight] sub-region (at [uPosition]/[vPosition] within its [nineSliceWidth]x[nineSliceHeight] reference frame) across a [width]x[height] area, clipping the last tile in each row/column rather than stretching it. */
private fun GuiGraphics.blitTiledSpriteTinted(
    sprite: TextureAtlasSprite, x: Int, y: Int, width: Int, height: Int, uPosition: Int, vPosition: Int,
    spriteWidth: Int, spriteHeight: Int, nineSliceWidth: Int, nineSliceHeight: Int,
    red: Float, green: Float, blue: Float, alpha: Float,
) {
    if (width <= 0 || height <= 0) return
    require(spriteWidth > 0 && spriteHeight > 0) { "Tiled sprite texture size must be positive, got ${spriteWidth}x${spriteHeight}" }
    var i = 0
    while (i < width) {
        val j = minOf(spriteWidth, width - i)
        var k = 0
        while (k < height) {
            val l = minOf(spriteHeight, height - k)
            blitSpriteTinted(sprite, nineSliceWidth, nineSliceHeight, uPosition, vPosition, x + i, y + k, j, l, red, green, blue, alpha)
            k += spriteHeight
        }
        i += spriteWidth
    }
}

/**
 * Tinted port of vanilla's own private `GuiGraphics.blitNineSlicedSprite` - splits [sprite] into
 * up to nine regions per [nineSlice]'s own border widths (the four corners drawn as-is, the four
 * edges tiled along their long axis, the center tiled across both) exactly as vanilla's own does,
 * line-for-line, just calling this file's own tinted [blitSpriteTinted]/[blitTiledSpriteTinted]
 * instead of vanilla's colorless private ones.
 */
private fun GuiGraphics.blitNineSlicedSpriteTinted(
    sprite: TextureAtlasSprite, nineSlice: GuiSpriteScaling.NineSlice, x: Int, y: Int, width: Int, height: Int,
    red: Float, green: Float, blue: Float, alpha: Float,
) {
    val border = nineSlice.border()
    val i = minOf(border.left(), width / 2)
    val j = minOf(border.right(), width / 2)
    val k = minOf(border.top(), height / 2)
    val l = minOf(border.bottom(), height / 2)
    val nw = nineSlice.width()
    val nh = nineSlice.height()
    when {
        width == nw && height == nh -> {
            blitSpriteTinted(sprite, nw, nh, 0, 0, x, y, width, height, red, green, blue, alpha)
        }
        height == nh -> {
            blitSpriteTinted(sprite, nw, nh, 0, 0, x, y, i, height, red, green, blue, alpha)
            blitTiledSpriteTinted(sprite, x + i, y, width - j - i, height, i, 0, nw - j - i, nh, nw, nh, red, green, blue, alpha)
            blitSpriteTinted(sprite, nw, nh, nw - j, 0, x + width - j, y, j, height, red, green, blue, alpha)
        }
        width == nw -> {
            blitSpriteTinted(sprite, nw, nh, 0, 0, x, y, width, k, red, green, blue, alpha)
            blitTiledSpriteTinted(sprite, x, y + k, width, height - l - k, 0, k, nw, nh - l - k, nw, nh, red, green, blue, alpha)
            blitSpriteTinted(sprite, nw, nh, 0, nh - l, x, y + height - l, width, l, red, green, blue, alpha)
        }
        else -> {
            blitSpriteTinted(sprite, nw, nh, 0, 0, x, y, i, k, red, green, blue, alpha)
            blitTiledSpriteTinted(sprite, x + i, y, width - j - i, k, i, 0, nw - j - i, k, nw, nh, red, green, blue, alpha)
            blitSpriteTinted(sprite, nw, nh, nw - j, 0, x + width - j, y, j, k, red, green, blue, alpha)
            blitSpriteTinted(sprite, nw, nh, 0, nh - l, x, y + height - l, i, l, red, green, blue, alpha)
            blitTiledSpriteTinted(sprite, x + i, y + height - l, width - j - i, l, i, nh - l, nw - j - i, l, nw, nh, red, green, blue, alpha)
            blitSpriteTinted(sprite, nw, nh, nw - j, nh - l, x + width - j, y + height - l, j, l, red, green, blue, alpha)
            blitTiledSpriteTinted(sprite, x, y + k, i, height - l - k, 0, k, i, nh - l - k, nw, nh, red, green, blue, alpha)
            blitTiledSpriteTinted(sprite, x + i, y + k, width - j - i, height - l - k, i, k, nw - j - i, nh - l - k, nw, nh, red, green, blue, alpha)
            blitTiledSpriteTinted(sprite, x + width - j, y + k, i, height - l - k, nw - j, k, j, nh - l - k, nw, nh, red, green, blue, alpha)
        }
    }
}

/** A helper extension to blit a SimpleThemeState without manually extracting all its properties. */
fun GuiGraphics.blit(state: SimpleThemeState, x: Int, y: Int) {
    this.blit(
        state.texture,
        x,
        y,
        state.width,
        state.height,
        state.u.toFloat(),
        state.v.toFloat(),
        state.uWidth,
        state.vHeight,
        state.textureSize.width,
        state.textureSize.height,
    )
}

/**
 * Tinted counterpart to [blit] (the raw/non-atlas [SimpleThemeState] overload) - normalizes
 * [state]'s own pixel-space `u`/`v`/`uWidth`/`vHeight`/`textureSize` into `0f..1f` UVs exactly as
 * vanilla's own untinted `blit(ResourceLocation, x,y,width,height, uOffset,vOffset,uWidth,vHeight,
 * textureWidth,textureHeight)` overload does on its own way down to its private `innerBlit`, then
 * calls this file's own [innerBlitTinted] directly instead - no [TextureAtlasSprite] involved at
 * any point, matching vanilla's own raw-texture chain.
 */
private fun GuiGraphics.blitTinted(state: SimpleThemeState, x: Int, y: Int, red: Float, green: Float, blue: Float, alpha: Float) {
    innerBlitTinted(
        state.texture,
        x, x + state.width, y, y + state.height,
        state.u.toFloat() / state.textureSize.width, (state.u + state.uWidth).toFloat() / state.textureSize.width,
        state.v.toFloat() / state.textureSize.height, (state.v + state.vHeight).toFloat() / state.textureSize.height,
        red, green, blue, alpha,
    )
}

/** Tinted counterpart to vanilla's own untinted `blit(ResourceLocation, x,y, uOffset,vOffset, width,height, textureWidth,textureHeight)` overload. */
fun GuiGraphics.blitTinted(
    atlasLocation: ResourceLocation, x: Int, y: Int, uOffset: Float, vOffset: Float,
    width: Int, height: Int, textureWidth: Int, textureHeight: Int,
    red: Float, green: Float, blue: Float, alpha: Float,
) {
    innerBlitTinted(
        atlasLocation,
        x, x + width, y, y + height,
        uOffset / textureWidth, (uOffset + width) / textureWidth,
        vOffset / textureHeight, (vOffset + height) / textureHeight,
        red, green, blue, alpha,
    )
}

/** Fills a rectangle with a 4-corner color gradient using the default GUI [RenderType]. */
fun GuiGraphics.fillGradient(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    topLeftColor: Int,
    topRightColor: Int,
    bottomLeftColor: Int,
    bottomRightColor: Int,
) = fillGradient(
    RenderType.gui(),
    x,
    y,
    width,
    height,
    topLeftColor,
    topRightColor,
    bottomLeftColor,
    bottomRightColor,
)

/**
 * Fills a rectangle with a 4-corner color gradient, unlike vanilla's [GuiGraphics.fillGradient]
 * (top-to-bottom only), by directly emitting one quad with a per-vertex ARGB color to [type].
 */
fun GuiGraphics.fillGradient(
    type: RenderType,
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    topLeftColor: Int,
    topRightColor: Int,
    bottomLeftColor: Int,
    bottomRightColor: Int,
) {
    val buffer = bufferSource().getBuffer(type)
    val matrix = pose().last().pose()

    buffer.addVertex(matrix, x + width, y, 0).setColor(topRightColor)
    buffer.addVertex(matrix, x, y, 0).setColor(topLeftColor)
    buffer.addVertex(matrix, x, y + height, 0).setColor(bottomLeftColor)
    buffer.addVertex(matrix, x + width, y + height, 0).setColor(bottomRightColor)
}

/** Draws an unfilled rectangle outline of [thickness] pixels using the default GUI [RenderType]. */
fun GuiGraphics.drawRectOutline(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    color: Int,
    thickness: Int = 1,
) = drawRectOutline(RenderType.gui(), x, y, width, height, color, thickness)

/** Draws an unfilled rectangle outline of [thickness] pixels as four filled edge strips. */
fun GuiGraphics.drawRectOutline(
    type: RenderType,
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    color: Int,
    thickness: Int = 1,
) {
    fill(type, x, y, x + width, y + thickness, color)
    fill(type, x, y + height - thickness, x + width, y + height, color)

    fill(type, x, y + thickness, x + thickness, y + height - thickness, color)
    fill(type, x + width - thickness, y + thickness, x + width, y + height - thickness, color)
}

/**
 * Runs [block] with this [GuiGraphics]'s [PoseStack][com.mojang.blaze3d.vertex.PoseStack] pushed,
 * popping it again afterwards (including when [block] throws). Saves the manual
 * `pose().pushPose()` / `pose().popPose()` pairing renderers otherwise need around
 * translate/scale/rotate calls.
 */
fun <T> GuiGraphics.pose(block: PoseStack.() -> T): T
{
    val pose = pose()
    pose.pushPose()
    try
    {
        return pose.block()
    }
    finally
    {
        pose.popPose()
    }
}

/**
 * Runs [block] with scissoring enabled to the `[minX, minY, maxX, maxY)` rectangle, disabling
 * it again afterwards. Saves the manual `enableScissor(...)` / `disableScissor()` pairing
 * renderers otherwise need around clipped content.
 */
fun <T> GuiGraphics.scissor(minX: Int, minY: Int, maxX: Int, maxY: Int, block: () -> T): T
{
    enableScissor(minX, minY, maxX, maxY)
    try
    {
        return block()
    }
    finally
    {
        disableScissor()
    }
}

/** Overload of [scissor] taking the clip bounds as an [IntRect]. */
fun <T> GuiGraphics.scissor(rect: IntRect, block: () -> T): T
{
    val (minX: Int, minY: Int, maxX: Int, maxY: Int) = rect
    return scissor(minX, minY, maxX, maxY, block)
}

/**
 * Lets a [GuiGraphics] receiver be invoked like `guiGraphics { ... }`, running [block] with
 * `this` as the receiver. Used throughout the built-in composables' `Renderer` implementations
 * to avoid repeating the `guiGraphics.` prefix on every draw call.
 */
operator fun GuiGraphics.invoke(block: GuiGraphics.() -> Unit): Unit = block()
