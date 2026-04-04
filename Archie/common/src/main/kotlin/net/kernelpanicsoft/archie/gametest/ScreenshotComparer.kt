package net.kernelpanicsoft.archie.gametest

import org.joml.Vector2i
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs

/**
 * Screenshot comparison utility with support for both exact and fuzzy matching.
 * Based on Fabric's TestScreenshotComparisonAlgorithms implementation.
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

        val templateRgb = template.getRGB(0, 0, template.width, template.height, null, 0, template.width)
        val captureRgb = capture.getRGB(0, 0, capture.width, capture.height, null, 0, capture.width)

        return templateRgb.contentEquals(captureRgb)
    }

    /**
     * Find a template image within a larger capture image (allowing sub-image matching).
     * Uses exact pixel-by-pixel matching by default.
     * @return top-left corner of the matched region, or null if not found
     */
    fun findInImage(templateFile: File, captureFile: File, tolerance: Int = 0): Vector2i? {
        val template = readImage(templateFile) ?: return null
        val capture = readImage(captureFile) ?: return null

        if (template.width > capture.width || template.height > capture.height) {
            return null
        }

        val algorithm = if (tolerance > 0) {
            MeanSquaredDifferenceAlgorithm(tolerance / 255.0f)
        } else {
            ExactScreenshotComparisonAlgorithm
        }

        val templateRgb = template.getRGB(0, 0, template.width, template.height, null, 0, template.width)
        val captureRgb = capture.getRGB(0, 0, capture.width, capture.height, null, 0, capture.width)

        val templateRawImage = RawImageImpl(template.width, template.height, templateRgb)
        val captureRawImage = RawImageImpl(capture.width, capture.height, captureRgb)

        return algorithm.findColor(captureRawImage, templateRawImage)
    }

    /**
     * Find a template image using exact pixel matching.
     */
    fun findInImageExact(templateFile: File, captureFile: File): Vector2i? {
        return findInImage(templateFile, captureFile, tolerance = 0)
    }

    /**
     * Find a template image using fuzzy matching with configurable threshold.
     * @param maxMeanSquaredDifference tolerance threshold (0.0-1.0)
     */
    fun findInImageFuzzy(templateFile: File, captureFile: File, maxMeanSquaredDifference: Float = 0.005f): Vector2i? {
        val template = readImage(templateFile) ?: return null
        val capture = readImage(captureFile) ?: return null

        if (template.width > capture.width || template.height > capture.height) {
            return null
        }

        val algorithm = MeanSquaredDifferenceAlgorithm(maxMeanSquaredDifference)

        val templateRgb = template.getRGB(0, 0, template.width, template.height, null, 0, template.width)
        val captureRgb = capture.getRGB(0, 0, capture.width, capture.height, null, 0, capture.width)

        val templateRawImage = RawImageImpl(template.width, template.height, templateRgb)
        val captureRawImage = RawImageImpl(capture.width, capture.height, captureRgb)

        return algorithm.findColor(captureRawImage, templateRawImage)
    }

    private fun readImage(file: File): BufferedImage? {
        return try {
            if (file.exists()) ImageIO.read(file) else null
        } catch (e: Exception) {
            null
        }
    }
}



