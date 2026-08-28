package net.kernelpanicsoft.archie.registries

import dev.architectury.platform.Mod
import dev.architectury.platform.hooks.EventBusesHooks
import dev.nyon.klf.MOD_BUS
import net.minecraft.client.resources.model.ModelResourceLocation
import net.minecraft.resources.ResourceLocation
import net.neoforged.bus.api.EventPriority
import net.neoforged.neoforge.client.event.ModelEvent
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs.registryKey
import net.neoforged.neoforge.registries.RegisterEvent

actual object CustomModelRegistry
{
	actual fun register(mod: Mod, id: ResourceLocation)
	{
		EventBusesHooks.whenAvailable(mod.modId) { bus ->
			bus.addListener(EventPriority.LOWEST, ModelEvent.RegisterAdditional::class.java) { event ->
				val modelLoc = ModelResourceLocation(
					id,
					"standalone"
				)
				event.register(modelLoc)
			}
		}
	}
}