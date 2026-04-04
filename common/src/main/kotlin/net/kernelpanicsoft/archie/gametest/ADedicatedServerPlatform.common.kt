package net.kernelpanicsoft.archie.gametest

import java.nio.file.Path
import java.util.Properties

/** Cross-loader bridge for dedicated server bootstrap used by client GameTests. */
expect object ADedicatedServerPlatform {
    fun start(serverDirectory: Path, serverProperties: Properties, timeoutSeconds: Long): Any

    fun stop(serverInstance: Any)

    fun port(serverInstance: Any): Int

    fun isAlive(serverInstance: Any): Boolean
}

