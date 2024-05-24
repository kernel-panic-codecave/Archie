package com.withertech.archie.neoforge

import com.withertech.archie.Archie
import net.neoforged.fml.common.Mod
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS

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
        }
        MOD_BUS.addListener<FMLCommonSetupEvent> {
            Archie.initCommon()
        }
    }
}
