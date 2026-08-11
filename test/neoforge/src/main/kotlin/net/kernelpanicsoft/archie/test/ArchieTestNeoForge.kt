package net.kernelpanicsoft.archie.test

import dev.nyon.klf.MOD_BUS
import net.neoforged.fml.common.Mod
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent

@Mod(ArchieTest.MOD_ID)
object ArchieTestNeoForge
{
	init {
		MOD_BUS.addListener<FMLConstructModEvent> {
			ArchieTest.init()
		}
		MOD_BUS.addListener<FMLClientSetupEvent> {
			ArchieTest.initClient()
		}
		MOD_BUS.addListener<FMLCommonSetupEvent> {
			ArchieTest.initCommon()
		}
		// GuiRegistry.initClient() (an ADeferredRegistryHolder override) now registers its own
		// screen factories at the correct time on its own - see
		// net.kernelpanicsoft.archie.registries.scheduleEarlyClientRegistration. A manual
		// RegisterMenuScreensEvent listener duplicating that here would now throw
		// "Duplicate attempt to register screen" instead of silently doing nothing.
	}
}