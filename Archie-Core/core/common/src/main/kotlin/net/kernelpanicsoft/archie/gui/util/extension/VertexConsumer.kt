package net.kernelpanicsoft.archie.gui.util.extension

import com.mojang.blaze3d.vertex.VertexConsumer
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