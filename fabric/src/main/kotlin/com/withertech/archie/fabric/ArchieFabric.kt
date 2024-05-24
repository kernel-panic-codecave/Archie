package com.withertech.archie.fabric

import com.withertech.archie.Archie
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.ModInitializer

/**
 * This class is the entrypoint for the mod on the Fabric platform.
 */
object ArchieFabric : ModInitializer, ClientModInitializer {
    override fun onInitialize() {
        Archie.init()
        Archie.initCommon()
    }

    override fun onInitializeClient()
    {
        Archie.initClient()
    }
}
