package net.kernelpanicsoft.archie.gametest

import net.minecraft.server.MinecraftServer
import net.minecraft.server.dedicated.DedicatedServer
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicReference

/**
 * Bridges [ADedicatedServerPlatform.start] on the bootstrap thread with `ServerMixin`, which calls
 * [captureRunningServer] from the constructed [MinecraftServer] once it exists.
 */
object ADedicatedServerPlatformInternal {
    private val bootstrapFutureRef = AtomicReference<CompletableFuture<DedicatedServer>?>(null)
    private val latestCapturedServerRef = AtomicReference<DedicatedServer?>(null)

    /** Starts a new bootstrap wait, clearing any previously captured server. Throws if a bootstrap is already in progress. */
    fun beginBootstrap(): CompletableFuture<DedicatedServer> {
        val future = CompletableFuture<DedicatedServer>()
        latestCapturedServerRef.set(null)
        check(bootstrapFutureRef.compareAndSet(null, future)) { "Dedicated server bootstrap already in progress" }
        return future
    }

    /** Fails the in-progress bootstrap future with [error]. */
    fun failBootstrap(error: Throwable) {
        bootstrapFutureRef.getAndSet(null)?.completeExceptionally(error)
    }

    /** Clears the in-progress bootstrap future without resolving it, used after a timeout falls back to [latestCapturedServer]. */
    fun clearBootstrap() {
        bootstrapFutureRef.set(null)
    }

    /** The most recently captured [DedicatedServer], if any; used as a fallback when the bootstrap future times out. */
    fun latestCapturedServer(): DedicatedServer? = latestCapturedServerRef.get()

    /** Called by `ServerMixin` when a [MinecraftServer] instance is constructed; completes the bootstrap future if [server] is a [DedicatedServer]. */
    @JvmStatic
    fun captureRunningServer(server: MinecraftServer) {
        if (server is DedicatedServer) {
            latestCapturedServerRef.set(server)
            bootstrapFutureRef.getAndSet(null)?.complete(server)
        }
    }
}

