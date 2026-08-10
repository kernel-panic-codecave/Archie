package net.kernelpanicsoft.archie

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.ModInitializer
import net.kernelpanicsoft.archie.gametest.ThreadingImpl

/**
 * Fabric entrypoint for the mod (`fabric.mod.json` `main`/`client` entrypoints).
 *
 * Delegates all real initialization to [Archie]; this object only wires that shared logic into
 * Fabric's initializer callbacks and registers the Fabric-specific client tick pump used by
 * [ThreadingImpl].
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
    }
}
