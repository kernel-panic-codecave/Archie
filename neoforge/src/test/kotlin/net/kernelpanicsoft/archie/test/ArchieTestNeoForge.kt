package net.kernelpanicsoft.archie.test

import dev.architectury.event.events.client.ClientLifecycleEvent
import dev.architectury.event.events.common.LifecycleEvent
import dev.nyon.klf.MOD_BUS
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent

@Mod(ArchieTest.MOD_ID)
object ArchieTestNeoForge
{
	init {
		ArchieTest.init()
		ClientLifecycleEvent.CLIENT_SETUP.register {

			ArchieTest.initClient()
		}
		LifecycleEvent.SETUP.register {
			ArchieTest.initCommon()
		}
		MOD_BUS.addListener<RegisterMenuScreensEvent> {
			it.register(GuiRegistry.TestMenu, ::TestScreen)
		}
	}
}