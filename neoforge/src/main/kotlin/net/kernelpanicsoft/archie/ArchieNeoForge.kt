package net.kernelpanicsoft.archie

import dev.architectury.event.events.client.ClientTickEvent
import dev.nyon.klf.MOD_BUS
import net.kernelpanicsoft.archie.gametest.ThreadingImpl
import net.neoforged.fml.common.Mod
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent

/**
 * Main class for the mod on the NeoForge platform.
 */
@Mod(Archie.MOD_ID)
object ArchieNeoForge {
    init {
        MOD_BUS.addListener<FMLConstructModEvent> {
            Archie.init()
        }
        MOD_BUS.addListener<FMLClientSetupEvent> {
            Archie.initClient()

            ClientTickEvent.CLIENT_POST.register {
                ThreadingImpl.onClientTick()
            }
        }
        MOD_BUS.addListener<FMLCommonSetupEvent> {
            Archie.initCommon()
        }
    }
}
