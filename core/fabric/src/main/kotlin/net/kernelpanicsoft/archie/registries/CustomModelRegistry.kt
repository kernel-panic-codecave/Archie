package net.kernelpanicsoft.archie.registries

import dev.architectury.platform.Mod
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin
import net.minecraft.resources.ResourceLocation

actual object CustomModelRegistry
{
	actual fun register(mod: Mod, id: ResourceLocation)
	{
		ModelLoadingPlugin.register { plugin ->
			plugin.addModels(id)
		}
	}
}