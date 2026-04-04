package net.kernelpanicsoft.archie.gametest

import net.minecraft.Util
import net.minecraft.server.Main
import net.minecraft.server.MinecraftServer
import net.minecraft.server.dedicated.DedicatedServer
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicReference

actual object ADedicatedServerPlatform {
    private val bootstrapFutureRef = AtomicReference<CompletableFuture<DedicatedServer>?>(null)

    private val defaultProperties: Properties = Util.make(Properties()) { props ->
        props.setProperty("online-mode", "false")
        props.setProperty("sync-chunk-writes", (Util.getPlatform() == Util.OS.WINDOWS).toString())
        props.setProperty("spawn-protection", "0")
        props.setProperty("max-players", "1")
    }

    actual fun start(serverDirectory: Path, serverProperties: Properties, timeoutSeconds: Long): Any {
        Files.createDirectories(serverDirectory)
        writeServerFiles(serverDirectory, serverProperties)

        val future = CompletableFuture<DedicatedServer>()
        check(bootstrapFutureRef.compareAndSet(null, future)) { "Dedicated server bootstrap already in progress" }

        Thread({
            try {
                Main.main(arrayOf("--nogui", "--universe", serverDirectory.toAbsolutePath().toString(), "--world", "world"))
            } catch (t: Throwable) {
                bootstrapFutureRef.getAndSet(null)?.completeExceptionally(t)
            }
        }, "Archie Dedicated GameTest Server Bootstrap").apply {
            isDaemon = true
            start()
        }

        val server = try {
            future.get(timeoutSeconds, TimeUnit.SECONDS)
        } catch (e: TimeoutException) {
            bootstrapFutureRef.set(null)
            throw IllegalStateException("Timed out waiting for dedicated server bootstrap", e)
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

    @JvmStatic
    fun captureRunningServer(server: MinecraftServer) {
        if (server is DedicatedServer) {
            bootstrapFutureRef.getAndSet(null)?.complete(server)
        }
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

