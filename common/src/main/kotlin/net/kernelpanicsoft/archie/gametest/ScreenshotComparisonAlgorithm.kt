package net.kernelpanicsoft.archie.gametest

import java.nio.file.Path

/**
 * Comparison algorithm interface for screenshot matching.
 * Supports both exact and fuzzy matching with configurable thresholds.
 */
interface ScreenshotComparisonAlgorithm {
    /**
     * Find a template pattern in a larger capture image using color data.
     * @return top-left corner of matched region, or null if not found
     */
    fun findColor(haystack: RawImage<IntArray>, needle: RawImage<IntArray>): org.joml.Vector2i?

    /**
     * Find a template pattern in a larger capture image using grayscale data.
     * @return top-left corner of matched region, or null if not found
     */
    fun findGrayscale(haystack: RawImage<ByteArray>, needle: RawImage<ByteArray>): org.joml.Vector2i?

    /**
     * Raw image data holder for comparison operations.
     */
    interface RawImage<DATA> {
        fun width(): Int
        fun height(): Int
        fun data(): DATA
    }
}

/**
 * Exact pixel matching algorithm - all pixels must match exactly.
 */
object ExactScreenshotComparisonAlgorithm : ScreenshotComparisonAlgorithm {
    override fun findColor(haystack: ScreenshotComparisonAlgorithm.RawImage<IntArray>, needle: ScreenshotComparisonAlgorithm.RawImage<IntArray>): org.joml.Vector2i? {
        val haystackData = haystack.data()
        val needleData = needle.data()
        val haystackWidth = haystack.width()
        val needleWidth = needle.width()
        val needleHeight = needle.height()

        if (needleWidth > haystackWidth || needleHeight > haystack.height()) {
            return null
        }

        for (needleY in 0..(haystack.height() - needleHeight)) {
            for (needleX in 0..(haystackWidth - needleWidth)) {
                var match = true
                for (y in 0 until needleHeight) {
                    for (x in 0 until needleWidth) {
                        val haystackColor = haystackData[(needleY + y) * haystackWidth + needleX + x]
                        val needleColor = needleData[y * needleWidth + x]
                        if (haystackColor != needleColor) {
                            match = false
                            break
                        }
                    }
                    if (!match) break
                }
                if (match) {
                    return org.joml.Vector2i(needleX, needleY)
                }
            }
        }
        return null
    }

    override fun findGrayscale(haystack: ScreenshotComparisonAlgorithm.RawImage<ByteArray>, needle: ScreenshotComparisonAlgorithm.RawImage<ByteArray>): org.joml.Vector2i? {
        val haystackData = haystack.data()
        val needleData = needle.data()
        val haystackWidth = haystack.width()
        val needleWidth = needle.width()
        val needleHeight = needle.height()

        if (needleWidth > haystackWidth || needleHeight > haystack.height()) {
            return null
        }

        for (needleY in 0..(haystack.height() - needleHeight)) {
            for (needleX in 0..(haystackWidth - needleWidth)) {
                var match = true
                for (y in 0 until needleHeight) {
                    for (x in 0 until needleWidth) {
                        val haystackLuminance = haystackData[(needleY + y) * haystackWidth + needleX + x]
                        val needleLuminance = needleData[y * needleWidth + x]
                        if (haystackLuminance != needleLuminance) {
                            match = false
                            break
                        }
                    }
                    if (!match) break
                }
                if (match) {
                    return org.joml.Vector2i(needleX, needleY)
                }
            }
        }
        return null
    }
}

/**
 * Mean squared difference algorithm - allows fuzzy matching within a tolerance threshold.
 * Based on Fabric's TestScreenshotComparisonAlgorithms implementation.
 */
data class MeanSquaredDifferenceAlgorithm(val maxMeanSquaredDifference: Float = 0.005f) : ScreenshotComparisonAlgorithm {
    override fun findColor(haystack: ScreenshotComparisonAlgorithm.RawImage<IntArray>, needle: ScreenshotComparisonAlgorithm.RawImage<IntArray>): org.joml.Vector2i? {
        val haystackData = haystack.data()
        val needleData = needle.data()
        val haystackWidth = haystack.width()
        val needleWidth = needle.width()
        val needleHeight = needle.height()

        if (needleWidth > haystackWidth || needleHeight > haystack.height()) {
            return null
        }

        // Threshold calculation to avoid floating point in inner loop
        val threshold = (maxMeanSquaredDifference * needleWidth * needleHeight * 3 * 255 * 255).toLong()

        for (needleY in 0..(haystack.height() - needleHeight)) {
            for (needleX in 0..(haystackWidth - needleWidth)) {
                var sumSquaredDifference = 0L
                var match = true

                for (y in 0 until needleHeight) {
                    for (x in 0 until needleWidth) {
                        val haystackColor = haystackData[(needleY + y) * haystackWidth + needleX + x]
                        val haystackRed = (haystackColor shr 16) and 0xFF
                        val haystackGreen = (haystackColor shr 8) and 0xFF
                        val haystackBlue = haystackColor and 0xFF

                        val needleColor = needleData[y * needleWidth + x]
                        val needleRed = (needleColor shr 16) and 0xFF
                        val needleGreen = (needleColor shr 8) and 0xFF
                        val needleBlue = needleColor and 0xFF

                        val diffRed = haystackRed - needleRed
                        val diffGreen = haystackGreen - needleGreen
                        val diffBlue = haystackBlue - needleBlue

                        sumSquaredDifference += (diffRed * diffRed + diffGreen * diffGreen + diffBlue * diffBlue).toLong()

                        if (sumSquaredDifference >= threshold) {
                            match = false
                            break
                        }
                    }
                    if (!match) break
                }

                if (match) {
                    return org.joml.Vector2i(needleX, needleY)
                }
            }
        }
        return null
    }

    override fun findGrayscale(haystack: ScreenshotComparisonAlgorithm.RawImage<ByteArray>, needle: ScreenshotComparisonAlgorithm.RawImage<ByteArray>): org.joml.Vector2i? {
        val haystackData = haystack.data()
        val needleData = needle.data()
        val haystackWidth = haystack.width()
        val needleWidth = needle.width()
        val needleHeight = needle.height()

        if (needleWidth > haystackWidth || needleHeight > haystack.height()) {
            return null
        }

        val threshold = (maxMeanSquaredDifference * needleWidth * needleHeight * 255 * 255).toLong()

        for (needleY in 0..(haystack.height() - needleHeight)) {
            for (needleX in 0..(haystackWidth - needleWidth)) {
                var sumSquaredDifference = 0L
                var match = true

                for (y in 0 until needleHeight) {
                    for (x in 0 until needleWidth) {
                        val haystackLuminance = haystackData[(needleY + y) * haystackWidth + needleX + x].toInt() and 0xFF
                        val needleLuminance = needleData[y * needleWidth + x].toInt() and 0xFF
                        val diff = haystackLuminance - needleLuminance

                        sumSquaredDifference += (diff * diff).toLong()

                        if (sumSquaredDifference >= threshold) {
                            match = false
                            break
                        }
                    }
                    if (!match) break
                }

                if (match) {
                    return org.joml.Vector2i(needleX, needleY)
                }
            }
        }
        return null
    }
}

/**
 * Raw image data implementation for comparison operations.
 */
data class RawImageImpl<DATA>(val width: Int, val height: Int, val data: DATA) : ScreenshotComparisonAlgorithm.RawImage<DATA> {
    override fun width() = width
    override fun height() = height
    override fun data() = data
}

