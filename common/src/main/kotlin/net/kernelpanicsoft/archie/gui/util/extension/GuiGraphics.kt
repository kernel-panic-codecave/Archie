package net.kernelpanicsoft.archie.gui.util

import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.RenderType
import org.joml.Matrix4f

/* ─────────────────────────── VertexConsumer ─────────────────────────── */

/**
 * Adds a vertex to this [VertexConsumer] using integer screen coordinates.
 *
 * This is a convenience overload that avoids repeated [Int.toFloat] casts when working
 * with pixel-aligned UI geometry.
 *
 * @param matrix The current pose matrix.
 * @param x      The x position in screen pixels.
 * @param y      The y position in screen pixels.
 * @param z      The z (depth) position.
 */
fun VertexConsumer.addVertex(matrix: Matrix4f, x: Int, y: Int, z: Int): VertexConsumer =
    addVertex(matrix, x.toFloat(), y.toFloat(), z.toFloat())

/* ─────────────────────────── GuiGraphics ─────────────────────────── */

/**
 * Draws a four-corner gradient rectangle on the GUI.
 *
 * Each corner can have an independent ARGB colour, enabling both solid fills
 * (all four colours identical) and arbitrary gradient fills.
 *
 * @param x              X coordinate of the top-left corner.
 * @param y              Y coordinate of the top-left corner.
 * @param width          Width of the rectangle in pixels.
 * @param height         Height of the rectangle in pixels.
 * @param topLeftColor   ARGB colour of the top-left corner.
 * @param topRightColor  ARGB colour of the top-right corner.
 * @param bottomLeftColor  ARGB colour of the bottom-left corner.
 * @param bottomRightColor ARGB colour of the bottom-right corner.
 */
fun GuiGraphics.fillGradient(
    x: Int, y: Int, width: Int, height: Int,
    topLeftColor: Int, topRightColor: Int,
    bottomLeftColor: Int, bottomRightColor: Int,
) = fillGradient(RenderType.gui(), x, y, width, height, topLeftColor, topRightColor, bottomLeftColor, bottomRightColor)

/**
 * Draws a four-corner gradient rectangle with an explicit [RenderType].
 *
 * @param type             The [RenderType] to use for rendering (e.g. [RenderType.gui]).
 * @param x                X coordinate of the top-left corner.
 * @param y                Y coordinate of the top-left corner.
 * @param width            Width of the rectangle in pixels.
 * @param height           Height of the rectangle in pixels.
 * @param topLeftColor     ARGB colour of the top-left corner.
 * @param topRightColor    ARGB colour of the top-right corner.
 * @param bottomLeftColor  ARGB colour of the bottom-left corner.
 * @param bottomRightColor ARGB colour of the bottom-right corner.
 */
fun GuiGraphics.fillGradient(
    type: RenderType,
    x: Int, y: Int, width: Int, height: Int,
    topLeftColor: Int, topRightColor: Int,
    bottomLeftColor: Int, bottomRightColor: Int,
) {
    val buffer = bufferSource().getBuffer(type)
    val matrix = pose().last().pose()
    buffer.addVertex(matrix, x + width, y,          0).setColor(topRightColor)
    buffer.addVertex(matrix, x,          y,          0).setColor(topLeftColor)
    buffer.addVertex(matrix, x,          y + height, 0).setColor(bottomLeftColor)
    buffer.addVertex(matrix, x + width, y + height, 0).setColor(bottomRightColor)
}

/**
 * Draws a hollow rectangle outline using [RenderType.gui].
 *
 * @param x         X coordinate of the top-left corner.
 * @param y         Y coordinate of the top-left corner.
 * @param width     Width of the rectangle in pixels.
 * @param height    Height of the rectangle in pixels.
 * @param color     ARGB colour of the outline.
 * @param thickness Stroke width in pixels (default 1).
 */
fun GuiGraphics.drawRectOutline(
    x: Int, y: Int, width: Int, height: Int,
    color: Int, thickness: Int = 1,
) = drawRectOutline(RenderType.gui(), x, y, width, height, color, thickness)

/**
 * Draws a hollow rectangle outline with an explicit [RenderType].
 *
 * @param type      The [RenderType] to use.
 * @param x         X coordinate of the top-left corner.
 * @param y         Y coordinate of the top-left corner.
 * @param width     Width of the rectangle in pixels.
 * @param height    Height of the rectangle in pixels.
 * @param color     ARGB colour of the outline.
 * @param thickness Stroke width in pixels (default 1).
 */
fun GuiGraphics.drawRectOutline(
    type: RenderType,
    x: Int, y: Int, width: Int, height: Int,
    color: Int, thickness: Int = 1,
) {
    fill(type, x,              y,              x + width,     y + thickness,          color)
    fill(type, x,              y + height - thickness, x + width,     y + height,     color)
    fill(type, x,              y + thickness,  x + thickness, y + height - thickness, color)
    fill(type, x + width - thickness, y + thickness,  x + width,     y + height - thickness, color)
}
