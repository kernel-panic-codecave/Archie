package net.kernelpanicsoft.archie.gametest.platform

import net.minecraft.Util
import net.minecraft.server.Main
import net.minecraft.server.MinecraftServer
import net.minecraft.server.dedicated.DedicatedServer
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Fabric implementation of [ADedicatedServerPlatform].
 *
 * Boots a real dedicated server ([Main.main]) on a daemon thread and hands the resulting
 * [DedicatedServer] back through [ADedicatedServerPlatformInternal], which `ServerMixin` feeds
 * via [ADedicatedServerPlatformInternal.captureRunningServer] once the server instance exists.
 */
actual object ADedicatedServerPlatform {
    /** Baseline `server.properties` for a headless, single-player-only GameTest server; overridden by caller-supplied properties. */
    private val defaultProperties: Properties = Util.make(Properties()) { props ->
        props.setProperty("online-mode", "false")
        props.setProperty("sync-chunk-writes", (Util.getPlatform() == Util.OS.WINDOWS).toString())
        props.setProperty("spawn-protection", "0")
        props.setProperty("max-players", "1")
    }

    /**
     * Writes `server.properties`/`eula.txt` into [serverDirectory], launches vanilla's dedicated
     * server entrypoint on a background thread, and blocks up to [timeoutSeconds] for it to report
     * back via [ADedicatedServerPlatformInternal]. Falls back to the last captured server instance
     * if the wait times out but a server is already up and listening.
     */
    actual fun start(serverDirectory: Path, serverProperties: Properties, timeoutSeconds: Long): Any {
        Files.createDirectories(serverDirectory)
        writeServerFiles(serverDirectory, serverProperties)

        val future = ADedicatedServerPlatformInternal.beginBootstrap()

        Thread({
            try {
                Main.main(arrayOf("--nogui", "--universe", serverDirectory.toAbsolutePath().toString(), "--world", "world"))
            } catch (t: Throwable) {
                ADedicatedServerPlatformInternal.failBootstrap(t)
            }
        }, "Archie Dedicated GameTest Server Bootstrap").apply {
            isDaemon = true
            start()
        }

        val server = try {
            future.get(timeoutSeconds, TimeUnit.SECONDS)
        } catch (e: TimeoutException) {
            ADedicatedServerPlatformInternal.clearBootstrap()
            val fallbackServer = ADedicatedServerPlatformInternal.latestCapturedServer()
            if (fallbackServer != null && fallbackServer.isRunning && fallbackServer.serverPort > 0) {
                fallbackServer
            } else {
                throw IllegalStateException("Timed out waiting for dedicated server bootstrap", e)
            }
        }

        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds)
        while (System.nanoTime() < deadline) {
            if (server.isRunning && server.serverPort > 0) break
            Thread.sleep(50L)
        }

        return server
    }

    actual fun stop(serverInstance: Any) {
        (serverInstance as? DedicatedServer)?.stopServer()
    }

    actual fun port(serverInstance: Any): Int {
        return (serverInstance as? DedicatedServer)?.serverPort ?: 25565
    }

    actual fun isAlive(serverInstance: Any): Boolean {
        val threadMethod = serverInstance.javaClass.methods.firstOrNull {
            (it.name == "getRunningThread" || it.name == "getThread") && it.parameterCount == 0
        } ?: return true

        val thread = runCatching { threadMethod.invoke(serverInstance) as? Thread }.getOrNull()
        return thread?.isAlive ?: true
    }

    private fun writeServerFiles(serverDirectory: Path, customProperties: Properties) {
        val merged = Properties()
        merged.putAll(defaultProperties)
        merged.putAll(customProperties)

        Files.newBufferedWriter(serverDirectory.resolve("server.properties")).use { writer ->
            merged.store(writer, "Archie GameTest dedicated server properties")
        }

        Files.newBufferedWriter(serverDirectory.resolve("eula.txt")).use { writer ->
            writer.write("eula=true")
            writer.newLine()
        }
    }
}

