package net.kernelpanicsoft.archie.test

import net.neoforged.fml.common.Mod
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent
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
	}
}