package net.kernelpanicsoft.archie.gui.render

import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.TextureAtlas
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.world.level.material.Fluid
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions

/** NeoForge implementation of [AFluidRenderPlatform], backed by [IClientFluidTypeExtensions]. */
actual object AFluidRenderPlatform
{
	actual fun getStillSprite(fluid: Fluid): TextureAtlasSprite?
	{
		val loc = IClientFluidTypeExtensions.of(fluid).stillTexture ?: return null
		return Minecraft.getInstance().modelManager.getAtlas(TextureAtlas.LOCATION_BLOCKS).getSprite(loc)
	}

	actual fun getTintColor(fluid: Fluid): Int = IClientFluidTypeExtensions.of(fluid).tintColor
}
