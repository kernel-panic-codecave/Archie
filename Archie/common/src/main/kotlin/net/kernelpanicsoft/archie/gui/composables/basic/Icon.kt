package net.kernelpanicsoft.archie.gui.composables.basic

import androidx.compose.runtime.Composable
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.modifiers.size
import net.minecraft.resources.ResourceLocation

/**
 * Convenience wrapper around [Texture] for fixed-size icon sprites.
 */
@Composable
fun Icon(
    texture: ResourceLocation,
    size: Int = 16,
    uOffset: Float = 0f,
    vOffset: Float = 0f,
    u: Int = size,
    v: Int = size,
    textureWidth: Int = 256,
    textureHeight: Int = 256,
    modifier: Modifier = Modifier,
) {
    Texture(
        loc = texture,
        uOffset = uOffset,
        vOffset = vOffset,
        u = u,
        v = v,
        textureWidth = textureWidth,
        textureHeight = textureHeight,
        modifier = Modifier.size(size, size).then(modifier),
    )
}

