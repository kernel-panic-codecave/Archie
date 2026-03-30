package net.kernelpanicsoft.archie.gui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import net.kernelpanicsoft.archie.Archie
import net.minecraft.resources.ResourceLocation

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
    val darkTextColor: Int,
    val lightTextColor: Int,
    val namespace: String = Archie.MOD_ID,
) {
    /**
     * Resolves the [ComposableTheme] for the given composable name using the active namespace,
     * type, and mode.
     *
     * @param composable The composable theme name (e.g. `"button"`, `"slot"`).
     * @return The [ComposableTheme] definition loaded from resources.
     */
    @Suppress("NOTHING_TO_INLINE")
    inline fun getComposableTheme(composable: String): ComposableTheme =
        ComposableTheme[composableThemeLocation(namespace, type, composable)]
}

/** Provides the current [ThemeData] to composables in the tree. */
val LocalTheme = compositionLocalOf { ThemeData(ThemeVariants.DEFAULT, "java", 0x404040, 0xFFFFFF, Archie.MOD_ID) }

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
    darkTextColor: Int = 0x404040,
    lightTextColor: Int = 0xFFFFFF,
    namespace: String = Archie.MOD_ID,
    content: @Composable () -> Unit,
) = CompositionLocalProvider(LocalTheme provides ThemeData(mode, type, darkTextColor, lightTextColor, namespace)) { content() }

/**
 * Sets the active theme using a pre-built [ThemeData].
 */
@Composable
fun Theme(data: ThemeData, content: @Composable () -> Unit) =
    CompositionLocalProvider(LocalTheme provides data) { content() }
