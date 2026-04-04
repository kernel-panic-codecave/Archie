package net.kernelpanicsoft.archie.gametest

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Manages screenshot file I/O, template storage, and directory organization.
 */
object ScreenshotManager {
    private fun getScreenshotBaseDir(): Path {
        val baseDir = Paths.get("build", "gametests", "screenshots")
        Files.createDirectories(baseDir)
        return baseDir
    }

    /**
     * Get directory for captured test screenshots.
     */
    fun getCaptureDir(): Path {
        val dir = getScreenshotBaseDir().resolve("captures")
        Files.createDirectories(dir)
        return dir
    }

    /**
     * Get directory for screenshot templates (baselines).
     */
    fun getTemplateDir(): Path {
        val dir = getScreenshotBaseDir().resolve("templates")
        Files.createDirectories(dir)
        return dir
    }

    /**
     * Resolve a template image file by name.
     * Searches in template directory with .png extension.
     */
    fun resolveTemplate(templateName: String): Path {
        val filename = if (templateName.endsWith(".png")) templateName else "$templateName.png"
        return getTemplateDir().resolve(filename)
    }

    /**
     * Generate a unique capture filename for a test screenshot.
     */
    fun generateCapturePath(testId: String, screenshotName: String): Path {
        val sanitized = (testId + "_" + screenshotName)
            .replace(Regex("[^a-zA-Z0-9_\\-.]"), "_")
        val filename = if (sanitized.endsWith(".png")) sanitized else "$sanitized.png"
        return getCaptureDir().resolve(filename)
    }
}

