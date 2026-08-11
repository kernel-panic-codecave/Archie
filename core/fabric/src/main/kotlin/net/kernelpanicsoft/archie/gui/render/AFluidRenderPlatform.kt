package net.kernelpanicsoft.archie.gui.render

import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.world.level.material.Fluid

/** Fabric implementation of [AFluidRenderPlatform], backed by `fabric-rendering-fluids-v1`. */
actual object AFluidRenderPlatform
{
	actual fun getStillSprite(fluid: Fluid): TextureAtlasSprite?
	{
		val handler = FluidRenderHandlerRegistry.INSTANCE.get(fluid) ?: return null
		return handler.getFluidSprites(null, null, fluid.defaultFluidState()).getOrNull(0)
	}

	actual fun getTintColor(fluid: Fluid): Int
	{
		val handler = FluidRenderHandlerRegistry.INSTANCE.get(fluid) ?: return -1
		return handler.getFluidColor(null, null, fluid.defaultFluidState())
	}
}
