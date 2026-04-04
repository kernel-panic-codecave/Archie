package net.kernelpanicsoft.archie.gui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.kernelpanicsoft.archie.Archie
import net.kernelpanicsoft.archie.resourcepacks.SerializationReloadListener
import net.kernelpanicsoft.archie.serialization.SerializationManager
import net.kernelpanicsoft.archie.gui.util.KColor
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.PreparableReloadListener
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.util.profiling.ProfilerFiller

/**
 * Constants for the built-in theme variant names.
 *
 * Pass these as the `mode` parameter of [ThemeData] to switch between light and dark variants.
 */
object ThemeVariants {
    /** The default (light) theme variant. */
    const val DEFAULT = ""
    /** The dark theme variant. */
    const val DARK = "dark"
}

@Serializable
data class ThemeManifest(
    val variants: Set<String> = setOf(ThemeVariants.DEFAULT),
    @SerialName("default_variant") val defaultVariant: String = ThemeVariants.DEFAULT,
    val aliases: Map<String, String> = emptyMap(),
)

class ThemeManifestResourceListener :
    SerializationReloadListener<ThemeManifest>(
        format = SerializationManager.json,
        serializer = ThemeManifest.serializer(),
        directory = "archie_themes",
        fileExtension = ".theme.json",
    ),
    PreparableReloadListener {

    companion object {
        internal val MANIFESTS = mutableMapOf<ResourceLocation, ThemeManifest>()

        fun resolveMode(namespace: String, type: String, requestedMode: String): String {
            val key = ResourceLocation.fromNamespaceAndPath(namespace, type.ifEmpty { "default" })
            val manifest = MANIFESTS[key] ?: return requestedMode
            val canonical = manifest.aliases[requestedMode] ?: requestedMode
            return if (canonical in manifest.variants) canonical else manifest.defaultVariant
        }
    }

    override fun apply(
        prepared: Map<ResourceLocation, ThemeManifest>,
        resourceManager: ResourceManager,
        profiler: ProfilerFiller,
    ) {
        MANIFESTS.clear()
        MANIFESTS.putAll(prepared)
    }
}

/**
 * Immutable data holder describing the active theme context for composables.
 *
 * Provided through [LocalTheme] to all composables under a [Theme] wrapper.
 *
 * @property mode      The active variant name (e.g. [ThemeVariants.DARK]). Empty string = default.
 * @property type      The platform type (e.g. `"java"`). Used as a path prefix for theme files.
 * @property namespace The resource namespace to look up theme definitions in.
 */
@Immutable
data class ThemeData(
    val mode: String,
    val type: String,
    val darkTextColor: KColor,
    val lightTextColor: KColor,
    val namespace: String = Archie.MOD_ID,
) {
    val resolvedMode: String
        get() = ThemeManifestResourceListener.resolveMode(namespace, type, mode)

    /**
     * Resolves the [ComposableTheme] for the given composable name using the active namespace,
     * type, and global mode.
     *
     * @param composable The composable theme name (e.g. `"button"`, `"slot"`).
     * @return The [ComposableTheme] definition loaded from resources.
     */
    fun getComposableTheme(composable: String): ComposableTheme {
        val mode = resolvedMode
        if (mode.isNotEmpty()) {
            val globalVariantLocation = composableThemeLocation(namespace, type, mode, composable)
            ThemeResourceListener.COMPOSABLES[globalVariantLocation]?.let { return it }
        }
        return ComposableTheme[composableThemeLocation(namespace, type, composable)]
    }
}

/** Provides the current [ThemeData] to composables in the tree. */
val LocalTheme = compositionLocalOf { ThemeData(ThemeVariants.DEFAULT, "java", KColor.DARK_GRAY, KColor.WHITE, Archie.MOD_ID) }

/**
 * Builds the [ResourceLocation] used to look up a composable's theme definition.
 *
 * The resulting path is `<namespace>:<composable>` with an optional `<type>/` prefix when
 * [type] is non-empty (e.g. `archie:java/button`).
 */
@Suppress("NOTHING_TO_INLINE")
inline fun composableThemeLocation(
    namespace: String,
    type: String,
    composable: String,
): ResourceLocation = ResourceLocation.fromNamespaceAndPath(namespace, composable)
    .run { if (type.isNotEmpty()) withPrefix("$type/") else this }

@Suppress("NOTHING_TO_INLINE")
inline fun composableThemeLocation(
    namespace: String,
    type: String,
    mode: String,
    composable: String,
): ResourceLocation {
    val prefix = buildString {
        if (type.isNotEmpty()) append(type).append('/')
        if (mode.isNotEmpty()) append(mode).append('/')
    }
    return ResourceLocation.fromNamespaceAndPath(namespace, composable)
        .run { if (prefix.isNotEmpty()) withPrefix(prefix) else this }
}

/**
 * Sets the active [ThemeData] for all composables in [content].
 *
 * @param mode      The variant name to activate (`""` for default, `"dark"` for dark mode).
 * @param type      The platform type (`"java"` by default).
 * @param namespace The resource namespace for theme files.
 * @param content   The composable tree that will receive the theme.
 */
@Composable
fun Theme(
    mode: String = ThemeVariants.DEFAULT,
    type: String = "java",
    darkTextColor: KColor = KColor.DARK_GRAY,
    lightTextColor: KColor = KColor.WHITE,
    namespace: String = Archie.MOD_ID,
    content: @Composable () -> Unit,
) = CompositionLocalProvider(LocalTheme provides ThemeData(mode, type, darkTextColor, lightTextColor, namespace)) { content() }

/**
 * Sets the active theme using a pre-built [ThemeData].
 */
@Composable
fun Theme(data: ThemeData, content: @Composable () -> Unit) =
    CompositionLocalProvider(LocalTheme provides data) { content() }
