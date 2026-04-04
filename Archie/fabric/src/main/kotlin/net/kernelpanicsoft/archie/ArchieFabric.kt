package net.kernelpanicsoft.archie

import dev.architectury.event.events.client.ClientTickEvent
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.ModInitializer
import net.kernelpanicsoft.archie.gametest.ThreadingImpl

/**
 * This class is the entrypoint for the mod on the Fabric platform.
 */
object ArchieFabric : ModInitializer, ClientModInitializer {
    override fun onInitialize()
    {
        Archie.init()
        Archie.initCommon()
    }

    override fun onInitializeClient()
    {
        Archie.initClient()
        ClientTickEvent.CLIENT_POST.register {
            ThreadingImpl.onClientTick()
        }
    }
}
