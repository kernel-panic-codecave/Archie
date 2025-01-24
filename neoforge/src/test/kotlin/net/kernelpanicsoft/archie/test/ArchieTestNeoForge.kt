package net.kernelpanicsoft.archie.test

import androidx.compose.runtime.BroadcastFrameClock
import net.neoforged.fml.common.Mod
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS

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
		MOD_BUS.addListener<RegisterMenuScreensEvent> {
			it.register(GuiRegistry.TestMenu, ::TestScreen)
		}
	}
}