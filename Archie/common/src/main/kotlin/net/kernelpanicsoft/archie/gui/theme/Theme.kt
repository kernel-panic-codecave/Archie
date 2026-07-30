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
import net.kernelpanicsoft.archie.util.div
import net.kernelpanicsoft.archie.util.rem
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

/**
 * Resource-pack-defined metadata for a theme (namespace + type), declaring which variant
 * names are valid and how requested modes should resolve to them.
 *
 * @property variants       The set of variant names this theme actually defines resources for.
 * @property defaultVariant The variant to fall back to when a requested mode isn't in [variants].
 * @property aliases        Maps a requested mode name to a canonical variant name before
 *   validating it against [variants] (e.g. letting a pack expose `"night"` as an alias for `"dark"`).
 */
@Serializable
data class ThemeManifest(
    val variants: Set<String> = setOf(ThemeVariants.DEFAULT),
    @SerialName("default_variant") val defaultVariant: String = ThemeVariants.DEFAULT,
    val aliases: Map<String, String> = emptyMap(),
)

/**
 * Loads `*.theme.json` [ThemeManifest] resources from the `archie_themes` directory on
 * resource pack reload, keyed by their resource location.
 *
 * [resolveMode] is what [ThemeData.resolvedMode] calls to turn a requested variant name into
 * one the active resource pack(s) actually support.
 */
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

        /**
         * Resolves [requestedMode] against the [ThemeManifest] loaded for [namespace]/[type],
         * applying [ThemeManifest.aliases] then falling back to [ThemeManifest.defaultVariant]
         * if the (possibly aliased) mode isn't in [ThemeManifest.variants].
         *
         * Returns [requestedMode] unchanged when no manifest is registered for that
         * namespace/type (i.e. the theme doesn't declare one).
         */
        fun resolveMode(namespace: String, type: String, requestedMode: String): String {
            val key = ResourceLocation.fromNamespaceAndPath(namespace, type.ifEmpty { "default" })
            val manifest = MANIFESTS[key] ?: return requestedMode
            val canonical = manifest.aliases[requestedMode] ?: requestedMode
            return if (canonical in manifest.variants) canonical else manifest.defaultVariant
        }
    }

    /** Replaces the registered [MANIFESTS] with the newly loaded [prepared] set. */
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
 * @property mode           The active variant name (e.g. [ThemeVariants.DARK]). Empty string = default.
 * @property type           The platform type (e.g. `"java"`). Used as a path prefix for theme files.
 * @property darkTextColor  The text color used on light/bright surfaces.
 * @property lightTextColor The text color used on dark surfaces.
 * @property namespace      The resource namespace to look up theme definitions in.
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
 * Builds the [ResourceLocation] used to look up a composable's default-variant theme
 * definition.
 *
 * The resulting path is `<namespace>:<composable>` with an optional `<type>/` prefix when
 * [type] is non-empty (e.g. `archie:java/button`).
 */
@Suppress("NOTHING_TO_INLINE")
inline fun composableThemeLocation(
    namespace: String,
    type: String,
    composable: String,
): ResourceLocation = if (type.isNotEmpty()) namespace % type / composable else namespace % composable

/**
 * Builds the [ResourceLocation] used to look up a composable's theme definition for a
 * specific non-default [mode] (variant).
 *
 * The resulting path is `<namespace>:<composable>` with an optional `<type>/` and `<mode>/`
 * prefix, in that order, for each that is non-empty (e.g. `archie:java/dark/button`).
 */
@Suppress("NOTHING_TO_INLINE")
inline fun composableThemeLocation(
    namespace: String,
    type: String,
    mode: String,
    composable: String,
): ResourceLocation {
    var ret = namespace % composable
    if (mode.isNotEmpty()) ret = mode / ret
    if (type.isNotEmpty()) ret = type / ret
    return ret
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
