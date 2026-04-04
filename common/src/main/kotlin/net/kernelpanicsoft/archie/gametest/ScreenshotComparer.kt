package net.kernelpanicsoft.archie.gametest

import org.joml.Vector2i
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs

/**
 * Simple screenshot comparison utility using Java 2D.
 * Compares pixels for exact equality or containment matching.
 */
object ScreenshotComparer {
    /**
     * Compare two images for exact equality (all pixels must match).
     * @return true if images are identical, false otherwise
     */
    fun imagesEqual(templateFile: File, captureFile: File): Boolean {
        val template = readImage(templateFile) ?: return false
        val capture = readImage(captureFile) ?: return false

        if (template.width != capture.width || template.height != capture.height) {
            return false
        }

        for (y in 0 until template.height) {
            for (x in 0 until template.width) {
                if (template.getRGB(x, y) != capture.getRGB(x, y)) {
                    return false
                }
            }
        }
        return true
    }

    /**
     * Find a template image within a larger capture image (allowing sub-image matching).
     * Uses a simple pixel-by-pixel correlation approach.
     * @return top-left corner of the matched region, or null if not found
     */
    fun findInImage(templateFile: File, captureFile: File, tolerance: Int = 0): Vector2i? {
        val template = readImage(templateFile) ?: return null
        val capture = readImage(captureFile) ?: return null

        if (template.width > capture.width || template.height > capture.height) {
            return null
        }

        // Try all possible positions
        for (y in 0..(capture.height - template.height)) {
            for (x in 0..(capture.width - template.width)) {
                if (matchesAt(template, capture, x, y, tolerance)) {
                    return Vector2i(x, y)
                }
            }
        }
        return null
    }

    private fun matchesAt(template: BufferedImage, capture: BufferedImage, startX: Int, startY: Int, tolerance: Int): Boolean {
        for (y in 0 until template.height) {
            for (x in 0 until template.width) {
                val templateRgb = template.getRGB(x, y)
                val captureRgb = capture.getRGB(startX + x, startY + y)
                if (!colorsClose(templateRgb, captureRgb, tolerance)) {
                    return false
                }
            }
        }
        return true
    }

    private fun colorsClose(rgb1: Int, rgb2: Int, tolerance: Int): Boolean {
        if (tolerance == 0) return rgb1 == rgb2

        val c1 = Color(rgb1)
        val c2 = Color(rgb2)

        val dr = abs(c1.red - c2.red)
        val dg = abs(c1.green - c2.green)
        val db = abs(c1.blue - c2.blue)
        val da = abs(c1.alpha - c2.alpha)

        return dr <= tolerance && dg <= tolerance && db <= tolerance && da <= tolerance
    }

    private fun readImage(file: File): BufferedImage? {
        return try {
            if (file.exists()) ImageIO.read(file) else null
        } catch (e: Exception) {
            null
        }
    }
}

