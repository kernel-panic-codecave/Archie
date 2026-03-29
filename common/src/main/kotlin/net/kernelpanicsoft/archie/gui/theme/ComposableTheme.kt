package net.kernelpanicsoft.archie.gui.theme

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import dev.architectury.registry.ReloadListenerRegistry
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.kernelpanicsoft.archie.Archie
import net.kernelpanicsoft.archie.gui.composables.theme.TextureStates
import net.kernelpanicsoft.archie.gui.layout.Size
import net.kernelpanicsoft.archie.serialization.serializers.SResourceLocation
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.resources.PreparableReloadListener
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener
import net.minecraft.util.profiling.ProfilerFiller

/* ─────────────────────── Theme state data classes ─────────────────────── */

/**
 * Sealed base for the two kinds of composable theme state: [SimpleThemeState] (fixed-size
 * sprite) and [NinePatchThemeState] (stretchable nine-patch).
 */
@Serializable
sealed interface ThemeState {
    /** Resource location of the texture atlas. */
    val texture: SResourceLocation
    /** Full pixel size of the texture atlas. */
    @SerialName("texture_size") val textureSize: Size
    /** Horizontal UV offset within the atlas. */
    val u: Int
    /** Vertical UV offset within the atlas. */
    val v: Int
}

/**
 * A fixed-size sprite slice within a texture atlas.
 *
 * @property width   Rendered width in pixels.
 * @property height  Rendered height in pixels.
 * @property uWidth  Width of the source region in the atlas.
 * @property vHeight Height of the source region in the atlas.
 */
@Serializable
data class SimpleThemeState(
    override val texture: SResourceLocation,
    @SerialName("texture_size") override val textureSize: Size,
    override val u: Int = 0,
    override val v: Int = 0,
    val width: Int,
    val height: Int,
    @SerialName("u_width")  val uWidth: Int,
    @SerialName("v_height") val vHeight: Int,
) : ThemeState

/**
 * A nine-patch stretchable texture state.
 *
 * @property cornersSize Size of each fixed corner patch.
 * @property centerSize  Size of the stretchable centre patch.
 * @property repeat      When `true`, the centre is tiled; otherwise it is stretched.
 */
@Serializable
data class NinePatchThemeState(
    override val texture: SResourceLocation,
    @SerialName("texture_size") override val textureSize: Size,
    override val u: Int = 0,
    override val v: Int = 0,
    val repeat: Boolean = false,
    @SerialName("corners_size") val cornersSize: Size,
    @SerialName("center_size")  val centerSize: Size,
) : ThemeState

/** A map of state-name → [ThemeState] for a single variant of a composable. */
@Serializable
data class StatefulTheme(val states: Map<String, ThemeState>)

/**
 * The full theme definition for a single composable type (e.g. `button`, `slot`).
 *
 * Contains base [states], optional named [variants] (e.g. `"dark"`), and a flag
 * indicating whether the composable uses nine-patch rendering.
 *
 * @property isNinepatch When `true`, all states use [NinePatchThemeState].
 * @property states      Base state map (always contains at least `"default"`).
 * @property variants    Named variant overrides (e.g. `"dark"` → its own state map).
 */
@Serializable
data class ComposableTheme(
    @SerialName("ninepatch") val isNinepatch: Boolean = true,
    val states: Map<String, ThemeState>,
    val variants: Map<String, StatefulTheme> = emptyMap(),
) {
    companion object {
        /**
         * Retrieves the [ComposableTheme] for [loc] from the loaded registry.
         *
         * @throws IllegalStateException if no theme was loaded for [loc].
         */
        operator fun get(loc: ResourceLocation): ComposableTheme =
            ThemeResourceListener.COMPOSABLES[loc]
                ?: throw IllegalStateException("No theme found for composable: $loc")
    }

    /**
     * Returns the [ThemeState] for [stateName] in [variantName], falling back to the base
     * state map and ultimately the `"default"` state.
     */
    @Suppress("NOTHING_TO_INLINE")
    inline fun getState(stateName: String, variantName: String): ThemeState =
        variants[variantName]?.states?.get(stateName)
            ?: states[stateName]
            ?: states[TextureStates.DEFAULT]!!

    /**
     * Returns `true` if [stateName] exists in [variantName] or the base state map.
     */
    @Suppress("NOTHING_TO_INLINE")
    inline fun hasState(stateName: String, variantName: String?): Boolean =
        (variantName?.let { variants[it] }?.states?.get(stateName) ?: states[stateName]) != null
}

/* ─────────────────────── Reload listener ─────────────────────── */

/**
 * A client resource-reload listener that loads [ComposableTheme] definitions from
 * `assets/<namespace>/archie_themes/` directories in resource packs.
 *
 * Theme files are JSON objects matching the structure of [ComposableTheme]. Register via
 * [ReloadListenerRegistry.register] during `initClient()`.
 *
 * ### File format
 * ```json
 * {
 *   "ninepatch": true,
 *   "states": {
 *     "default": {
 *       "texture": "archie:textures/gui/java/button.png",
 *       "texture_size": { "width": 64, "height": 192 },
 *       "corners_size": { "width": 3, "height": 3 },
 *       "center_size":  { "width": 58, "height": 58 }
 *     },
 *     "hovered": { "v": 64 }
 *   }
 * }
 * ```
 */
class ThemeResourceListener :
    SimpleJsonResourceReloadListener(Gson(), "archie_themes"),
    PreparableReloadListener {

    companion object {
        /** All registered [ComposableTheme]s keyed by their [ResourceLocation]. */
        internal val COMPOSABLES = mutableMapOf<ResourceLocation, ComposableTheme>()
    }

    override fun apply(
        objs: Map<ResourceLocation?, JsonElement?>,
        resourceManager: ResourceManager,
        profiler: ProfilerFiller,
    ) {
        COMPOSABLES.clear()
        for ((location, el) in objs) {
            if (location == null || el !is JsonObject) continue
            try {
                val isNinepatch = el["ninepatch"]?.asBoolean != false
                val statesObj = el["states"]?.asJsonObject
                    ?: throw IllegalStateException("Theme must have a valid states object: $location")
                val defaultObj = statesObj["default"]?.asJsonObject
                    ?: throw IllegalStateException("Theme must have a \"default\" state: $location")

                val defaultState = if (isNinepatch)
                    parseNinePatch(location, "default", defaultObj)
                else
                    parseSimple(location, "default", defaultObj)

                val states = mutableMapOf<String, ThemeState>()
                for ((name, stateEl) in statesObj.entrySet()) {
                    if (stateEl !is JsonObject) continue
                    states[name] = if (isNinepatch)
                        parseNinePatch(location, name, stateEl, defaultState as? NinePatchThemeState)
                    else
                        parseSimple(location, name, stateEl, defaultState as? SimpleThemeState)
                }

                val variants = mutableMapOf<String, StatefulTheme>()
                el["variants"]?.asJsonObject?.entrySet()?.forEach { (variantName, variantEl) ->
                    if (variantEl !is JsonObject) return@forEach
                    val vs = mutableMapOf<String, ThemeState>()
                    variantEl.entrySet().forEach { (sName, sEl) ->
                        if (sEl !is JsonObject) return@forEach
                        vs[sName] = if (isNinepatch)
                            parseNinePatch(location, sName, sEl, defaultState as? NinePatchThemeState)
                        else
                            parseSimple(location, sName, sEl, defaultState as? SimpleThemeState)
                    }
                    variants[variantName] = StatefulTheme(vs)
                }

                COMPOSABLES[location] = ComposableTheme(isNinepatch, states, variants)
                Archie.LOGGER.info(
                    "Theme \"{}\" loaded ({} states, {} variants{})",
                    location, states.size, variants.size,
                    if (isNinepatch) ", nine-patch" else "",
                )
            } catch (e: Exception) {
                Archie.LOGGER.warn("Error processing theme at {}: {}", location, e.message, e)
            }
        }
    }

    private fun baseFields(loc: ResourceLocation, name: String, el: JsonObject, default: ThemeState?): Map<String, Any> {
        val texture = default?.texture
            ?: el["texture"]?.asString?.let { ResourceLocation.parse(it) }
            ?: throw IllegalStateException("Missing texture for state \"$name\" in: $loc")
        val textureSize = el.parseSize("texture_size")
            ?: default?.textureSize
            ?: throw IllegalStateException("Missing texture_size for state \"$name\" in: $loc")
        return mapOf(
            "texture" to texture,
            "textureSize" to textureSize,
            "u" to (el["u"]?.asInt ?: default?.u ?: 0),
            "v" to (el["v"]?.asInt ?: default?.v ?: 0),
        )
    }

    private fun parseNinePatch(loc: ResourceLocation, name: String, el: JsonObject, default: NinePatchThemeState? = null): NinePatchThemeState {
        val base = baseFields(loc, name, el, default)
        val repeat = el["repeat"]?.asBoolean ?: default?.repeat ?: false
        val patchSize = el.parseSize("patch_size")
        val corners = el.parseSize("corners_size") ?: default?.cornersSize ?: patchSize
            ?: throw IllegalStateException("Missing corners_size for state \"$name\" in: $loc")
        val center = el.parseSize("center_size") ?: default?.centerSize ?: patchSize
            ?: throw IllegalStateException("Missing center_size for state \"$name\" in: $loc")
        return NinePatchThemeState(
            base["texture"] as ResourceLocation,
            base["textureSize"] as Size,
            base["u"] as Int,
            base["v"] as Int,
            repeat, corners, center,
        )
    }

    private fun parseSimple(loc: ResourceLocation, name: String, el: JsonObject, default: SimpleThemeState? = null): SimpleThemeState {
        val base = baseFields(loc, name, el, default)
        val width  = el["width"]?.asInt  ?: default?.width  ?: throw IllegalStateException("Missing width for state \"$name\" in: $loc")
        val height = el["height"]?.asInt ?: default?.height ?: throw IllegalStateException("Missing height for state \"$name\" in: $loc")
        return SimpleThemeState(
            base["texture"] as ResourceLocation,
            base["textureSize"] as Size,
            base["u"] as Int,
            base["v"] as Int,
            width, height,
            el["uWidth"]?.asInt  ?: default?.uWidth  ?: width,
            el["vHeight"]?.asInt ?: default?.vHeight ?: height,
        )
    }

    private fun JsonObject.parseSize(key: String): Size? {
        val obj = this[key]?.asJsonObject ?: return null
        val w = obj["width"]?.asInt  ?: return null
        val h = obj["height"]?.asInt ?: return null
        return Size(w, h)
    }
}
