package net.kernelpanicsoft.archie.gui.theme

import dev.architectury.registry.ReloadListenerRegistry
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.kernelpanicsoft.archie.Archie
import net.kernelpanicsoft.archie.gui.composables.theme.TextureStates
import net.kernelpanicsoft.archie.gui.layout.Size
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.modifiers.sizeIn
import net.kernelpanicsoft.archie.resourcepacks.SerializationReloadListener
import net.kernelpanicsoft.archie.serialization.SerializationManager
import net.kernelpanicsoft.archie.serialization.serializers.SResourceLocation
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.PreparableReloadListener
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.util.profiling.ProfilerFiller

/* ─────────────────────── Theme state data classes ─────────────────────── */

/** Sealed base for composable theme states rendered as sprites. */
@Serializable
sealed interface ThemeState {
    /** Resource location of the sprite atlas or source image. */
    val texture: SResourceLocation
    /** Full pixel size of the source image when UV rendering is used. */
    @SerialName("texture_size") val textureSize: Size
    /** Horizontal UV offset within the atlas. */
    val u: Int
    /** Vertical UV offset within the atlas. */
    val v: Int
}

/**
 * A fixed-size sprite slice within a sprite atlas or source image.
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

/** A map of state-name → [ThemeState] for a single variant of a composable. */
@Serializable
data class StatefulTheme(val states: Map<String, ThemeState>)

/** Raw JSON shape of a theme file, deserialized as-is and resolved by [ThemeResourceListener] into a [ComposableTheme]. */
@Serializable
data class RawComposableTheme(
    val states: Map<String, RawThemeState> = emptyMap(),
    val variants: Map<String, Map<String, RawThemeState>> = emptyMap(),
    @SerialName("min_size") val minSize: Size? = null,
)

/** Raw JSON shape of a single theme state; fields left `null` inherit from the state's `"default"` entry. */
@Serializable
data class RawThemeState(
    val texture: String? = null,
    @SerialName("texture_size") val textureSize: Size? = null,
    val u: Int? = null,
    val v: Int? = null,
    val width: Int? = null,
    val height: Int? = null,
    @SerialName("u_width") val uWidthSnake: Int? = null,
    @SerialName("v_height") val vHeightSnake: Int? = null,
    val uWidth: Int? = null,
    val vHeight: Int? = null,
)

@Serializable
private data class GuiTextureMetadata(
    val gui: GuiMetadataSection? = null,
)

@Serializable
private data class GuiMetadataSection(
    val scaling: GuiScalingMetadata? = null,
)

@Serializable
private data class GuiScalingMetadata(
    val type: String? = null,
)

/**
 * The full theme definition for a single composable type (e.g. `button`, `slot`).
 *
 * Contains base [states] and optional named [variants] (e.g. `"dark"`).
 *
 * @property isNineslice Whether [states]' default texture is nine-slice scaled, per its
 *   `.mcmeta` sprite metadata. When `false` and [minSize] isn't set, composables using this
 *   theme get a minimum size matching the sprite's own pixel dimensions instead of stretching
 *   arbitrarily.
 * @property states      Base state map (always contains at least `"default"`).
 * @property variants    Named variant overrides (e.g. `"dark"` → its own state map).
 * @property minSize     Explicit intrinsic minimum size composables using this theme should
 *   enforce, regardless of [isNineslice] - e.g. a nine-slice button texture still wants a
 *   sensible minimum clickable area even though its sprite can stretch to any size. Takes
 *   priority over the sprite-size fallback described under [isNineslice] when set.
 */
@Serializable
data class ComposableTheme(
    val isNineslice: Boolean = false,
    val states: Map<String, ThemeState>,
    val variants: Map<String, StatefulTheme> = emptyMap(),
    val minSize: Size? = null,
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

/**
 * The [Modifier.sizeIn] floor a composable using this theme should apply to its own layout
 * node: [ComposableTheme.minSize] when the theme declares one, otherwise the `"default"`
 * state's own sprite dimensions for a non-nine-slice texture (which can't stretch without
 * distorting), otherwise no floor at all - a nine-slice texture with no explicit [ComposableTheme.minSize]
 * is free to shrink or stretch to fit its content.
 */
fun ComposableTheme.intrinsicSizeModifier(): Modifier {
    minSize?.let { return Modifier.sizeIn(minWidth = it.width, minHeight = it.height) }
    if (isNineslice) return Modifier
    val default = states[TextureStates.DEFAULT] as SimpleThemeState
    return Modifier.sizeIn(minWidth = default.width, minHeight = default.height)
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
 *   "states": {
 *     "default": {
 *       "texture": "archie:java/button",
 *       "texture_size": { "width": 64, "height": 64 },
 *       "width": 64,
 *       "height": 20
 *     },
 *     "focused": { "texture": "archie:java/button_highlighted" }
 *   },
 *   "min_size": { "width": 50, "height": 20 }
 * }
 * ```
 */
class ThemeResourceListener :
    SerializationReloadListener<RawComposableTheme>(
        format = SerializationManager.json,
        serializer = RawComposableTheme.serializer(),
        directory = "archie_themes",
        fileExtension = ".json",
    ),
    PreparableReloadListener {

    companion object {
        /** All registered [ComposableTheme]s keyed by their [ResourceLocation]. */
        internal val COMPOSABLES = mutableMapOf<ResourceLocation, ComposableTheme>()
    }

    /** Excludes `*.theme.json` [ThemeManifest] files, which this listener does not parse. */
    override fun shouldLoadResource(fileLocation: ResourceLocation): Boolean =
        !fileLocation.path.endsWith(".theme.json")

    override fun apply(
        objs: Map<ResourceLocation, RawComposableTheme>,
        resourceManager: ResourceManager,
        profiler: ProfilerFiller,
    ) {
        COMPOSABLES.clear()
        for ((location, root) in objs) {
            if (location.path.endsWith(".theme")) continue
            try {
                val statesObj = root.states
                if (statesObj.isEmpty()) {
                    throw IllegalStateException("Theme must have a valid states object: $location")
                }
                val defaultObj = statesObj[TextureStates.DEFAULT]
                    ?: throw IllegalStateException("Theme must have a \"default\" state: $location")

                val defaultState = parseSimple(location, "default", defaultObj)

                val states = mutableMapOf<String, ThemeState>()
                for ((name, stateEl) in statesObj) {
                    states[name] = parseSimple(location, name, stateEl, defaultState)
                }

                val variants = mutableMapOf<String, StatefulTheme>()
                root.variants.forEach { (variantName, variantEl) ->
                    val vs = mutableMapOf<String, ThemeState>()
                    variantEl.forEach { (sName, sEl) ->
                        vs[sName] = parseSimple(location, sName, sEl, defaultState)
                    }
                    variants[variantName] = StatefulTheme(vs)
                }

                val isNineslice = resourceManager.isNineSliceTexture(defaultState.texture)
                COMPOSABLES[location] = ComposableTheme(isNineslice, states, variants, root.minSize)
                Archie.LOGGER.info(
                    "Theme \"{}\" loaded ({} states, {} variants, nineslice={}, minSize={})",
                    location, states.size, variants.size, isNineslice, root.minSize,
                )
            } catch (e: Exception) {
                Archie.LOGGER.warn("Error processing theme at {}: {}", location, e.message, e)
            }
        }
    }

    private data class BaseFields(
        val texture: ResourceLocation,
        val textureSize: Size,
        val u: Int,
        val v: Int,
    )

    private fun baseFields(loc: ResourceLocation, name: String, el: RawThemeState, default: ThemeState?): BaseFields {
        val texture = el.texture?.let { ResourceLocation.parse(it) }
            ?: default?.texture
            ?: throw IllegalStateException("Missing texture for state \"$name\" in: $loc")
        val textureSize = el.textureSize
            ?: default?.textureSize
            ?: throw IllegalStateException("Missing texture_size for state \"$name\" in: $loc")
        return BaseFields(
            texture = texture,
            textureSize = textureSize,
            u = el.u ?: default?.u ?: 0,
            v = el.v ?: default?.v ?: 0,
        )
    }


    private fun parseSimple(loc: ResourceLocation, name: String, el: RawThemeState, default: SimpleThemeState? = null): SimpleThemeState {
        val base = baseFields(loc, name, el, default)
        val width  = el.width  ?: default?.width  ?: throw IllegalStateException("Missing width for state \"$name\" in: $loc")
        val height = el.height ?: default?.height ?: throw IllegalStateException("Missing height for state \"$name\" in: $loc")
        return SimpleThemeState(
            base.texture,
            base.textureSize,
            base.u,
            base.v,
            width, height,
            el.uWidthSnake ?: el.uWidth ?: default?.uWidth ?: width,
            el.vHeightSnake ?: el.vHeight ?: default?.vHeight ?: height,
        )
    }

    private fun ResourceManager.isNineSliceTexture(texture: ResourceLocation): Boolean {
        val candidates = if (texture.path.startsWith("textures/") && texture.path.endsWith(".png")) {
            listOf(ResourceLocation.fromNamespaceAndPath(texture.namespace, "${texture.path}.mcmeta"))
        } else {
            listOf(
                ResourceLocation.fromNamespaceAndPath(texture.namespace, "textures/gui/sprites/${texture.path}.png.mcmeta"),
                ResourceLocation.fromNamespaceAndPath(texture.namespace, "textures/${texture.path}.png.mcmeta"),
            )
        }

        for (candidate in candidates) {
            val resource = getResource(candidate).orElse(null) ?: continue
            try {
                resource.openAsReader().use { reader ->
                    val metadata = SerializationManager.json.decodeFromString<GuiTextureMetadata>(reader.readText())
                    val type = metadata.gui?.scaling?.type
                    if (type == "nine_slice") return true
                }
            } catch (_: Exception) {
                // Ignore malformed metadata and continue trying other candidates.
            }
        }
        return false
    }
}
