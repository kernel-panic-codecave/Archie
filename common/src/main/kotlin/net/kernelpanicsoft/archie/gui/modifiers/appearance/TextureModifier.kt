package net.kernelpanicsoft.archie.gui.modifiers.appearance

import androidx.compose.runtime.Stable
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.minecraft.resources.ResourceLocation

/**
 * A [Modifier.Element] that overrides the texture used by certain theme-aware composables
 * (such as [net.kernelpanicsoft.archie.gui.composables.data.Slot]).
 *
 * Only the last applied [TextureModifier] on a node takes effect.
 *
 * @property texture The [ResourceLocation] of the replacement texture.
 */
data class TextureModifier(val texture: ResourceLocation) : Modifier.Element<TextureModifier> {
    override fun mergeWith(other: TextureModifier): TextureModifier =
        throw UnsupportedOperationException("TextureModifier cannot be merged; only one texture can be active at a time.")
}

/**
 * Overrides the texture of theme-aware composables with the given [ResourceLocation].
 *
 * @param texture The replacement texture resource location.
 */
@Stable
fun Modifier.texture(texture: ResourceLocation): Modifier = this then TextureModifier(texture)
