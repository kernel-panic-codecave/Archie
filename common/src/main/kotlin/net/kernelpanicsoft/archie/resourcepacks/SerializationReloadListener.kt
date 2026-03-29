package net.kernelpanicsoft.archie.resourcepacks

import com.mojang.logging.LogUtils
import kotlinx.serialization.KSerializer
import kotlinx.serialization.StringFormat
import net.minecraft.resources.FileToIdConverter
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.SimplePreparableReloadListener
import net.minecraft.util.profiling.ProfilerFiller
import org.slf4j.Logger
import java.io.InputStreamReader

/**
 * An abstract [SimplePreparableReloadListener] that automatically discovers resource files
 * under a given [directory] and deserializes them using kotlinx.serialization.
 *
 * Override [apply] to store or process the resulting map of [ResourceLocation] to [T] after
 * each reload. Register instances of subclasses with Architectury's `ReloadListenerRegistry`
 * during `initClient()`.
 *
 * ### Example
 * ```kotlin
 * class MyDataListener : SerializationReloadListener<MyData>(
 *     format    = Json { ignoreUnknownKeys = true },
 *     serializer = MyData.serializer(),
 *     directory = "my_data",
 *     fileExtension = ".json",
 * ) {
 *     override fun apply(prepared: Map<ResourceLocation, MyData>, ...) {
 *         MyDataRegistry.ENTRIES.clear()
 *         MyDataRegistry.ENTRIES += prepared
 *     }
 * }
 * ```
 *
 * @param T The data class type that each resource file deserializes into.
 * @param format The kotlinx.serialization [StringFormat] to use (e.g. `Json`, `Toml`).
 * @param serializer The [KSerializer] for [T].
 * @param directory The resource-pack directory to scan (e.g. `"archie_themes"`).
 * @param fileExtension The file extension to match, including the leading dot (e.g. `".json"`).
 */
abstract class SerializationReloadListener<T>(
    private val format: StringFormat,
    private val serializer: KSerializer<T>,
    private val directory: String,
    private val fileExtension: String,
) : SimplePreparableReloadListener<Map<ResourceLocation, T>>() {

    companion object {
        private val LOGGER: Logger = LogUtils.getLogger()
    }

    /**
     * Scans [resourceManager] for all files matching [directory] / * [fileExtension],
     * deserializes each one, and returns the resulting map keyed by entry id.
     *
     * Errors in individual files are logged and that file is skipped; other entries still load.
     */
    override fun prepare(
        resourceManager: ResourceManager,
        profiler: ProfilerFiller,
    ): Map<ResourceLocation, T> {
        val dataMap = mutableMapOf<ResourceLocation, T>()
        val fileToIdConverter = FileToIdConverter(directory, fileExtension)

        for ((fileLocation, resource) in fileToIdConverter.listMatchingResources(resourceManager)) {
            val resourceId = fileToIdConverter.fileToId(fileLocation)
            try {
                InputStreamReader(resource.open()).use { reader ->
                    dataMap[resourceId] = format.decodeFromString(serializer, reader.readText())
                }
            } catch (e: Exception) {
                LOGGER.error("Couldn't parse data file {} from {}", resourceId, fileLocation, e)
            }
        }
        return dataMap
    }
}
