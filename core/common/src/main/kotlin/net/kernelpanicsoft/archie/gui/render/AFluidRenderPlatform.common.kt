package net.kernelpanicsoft.archie.gui.render

import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.world.level.material.Fluid

/**
 * Cross-loader lookup of a [Fluid]'s client-rendering appearance, backed by an `actual` per mod
 * loader - Fabric's `FluidRenderHandlerRegistry` and NeoForge's `IClientFluidTypeExtensions`
 * expose the same information through unrelated APIs, so [net.kernelpanicsoft.archie.gui.composables.basic.FluidTank]
 * goes through this instead of touching either directly.
 *
 * Client-only; only ever called from GUI rendering code.
 */
expect object AFluidRenderPlatform
{
	/** The fluid's still-texture sprite from the blocks atlas, or `null` if it can't be resolved. */
	fun getStillSprite(fluid: Fluid): TextureAtlasSprite?

	/** The ARGB tint color applied over [getStillSprite]'s sprite (`0xFFFFFFFF` = no tint). */
	fun getTintColor(fluid: Fluid): Int
}
