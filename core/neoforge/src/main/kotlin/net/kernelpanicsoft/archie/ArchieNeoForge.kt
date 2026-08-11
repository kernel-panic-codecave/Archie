package net.kernelpanicsoft.archie

import dev.architectury.event.events.client.ClientTickEvent
import dev.nyon.klf.MOD_BUS
import net.kernelpanicsoft.archie.gametest.platform.ThreadingImpl
import net.neoforged.fml.common.Mod
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent

/**
 * NeoForge entrypoint for the mod, registered via the `@Mod` annotation.
 *
 * Delegates all real initialization to [Archie], wiring its lifecycle calls into the NeoForge
 * mod-bus events ([FMLConstructModEvent], [FMLClientSetupEvent], [FMLCommonSetupEvent]) and
 * registering the client tick pump used by [ThreadingImpl].
 */
@Mod(Archie.MOD_ID)
object ArchieNeoForge {
    init {
        MOD_BUS.addListener<FMLConstructModEvent> {
            Archie.init()
        }
        MOD_BUS.addListener<FMLClientSetupEvent> {
            Archie.initClient()
        }
        MOD_BUS.addListener<FMLCommonSetupEvent> {
            Archie.initCommon()
        }
    }
}
