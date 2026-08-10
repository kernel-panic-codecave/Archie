package net.kernelpanicsoft.archie.gametest

import java.nio.file.Path
import java.util.Properties

/** Cross-loader bridge for dedicated server bootstrap used by client GameTests. */
expect object ADedicatedServerPlatform {
    /**
     * Boots a loader-specific dedicated server rooted at [serverDirectory] using
     * [serverProperties], waiting up to [timeoutSeconds] for it to finish starting.
     *
     * @return An opaque, loader-specific handle to pass to [stop]/[port]/[isAlive].
     */
    fun start(serverDirectory: Path, serverProperties: Properties, timeoutSeconds: Long): Any

    /** Shuts down the dedicated server identified by [serverInstance] (as returned by [start]). */
    fun stop(serverInstance: Any)

    /** The port the dedicated server identified by [serverInstance] is listening on. */
    fun port(serverInstance: Any): Int

    /** Whether the dedicated server identified by [serverInstance] is still running. */
    fun isAlive(serverInstance: Any): Boolean
}

