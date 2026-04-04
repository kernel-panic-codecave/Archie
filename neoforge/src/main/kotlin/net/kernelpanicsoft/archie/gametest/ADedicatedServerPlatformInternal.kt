package net.kernelpanicsoft.archie.gametest

import net.minecraft.server.MinecraftServer
import net.minecraft.server.dedicated.DedicatedServer
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicReference

object ADedicatedServerPlatformInternal {
    private val bootstrapFutureRef = AtomicReference<CompletableFuture<DedicatedServer>?>(null)

    fun beginBootstrap(): CompletableFuture<DedicatedServer> {
        val future = CompletableFuture<DedicatedServer>()
        check(bootstrapFutureRef.compareAndSet(null, future)) { "Dedicated server bootstrap already in progress" }
        return future
    }

    fun failBootstrap(error: Throwable) {
        bootstrapFutureRef.getAndSet(null)?.completeExceptionally(error)
    }

    fun clearBootstrap() {
        bootstrapFutureRef.set(null)
    }

    @JvmStatic
    fun captureRunningServer(server: MinecraftServer) {
        if (server is DedicatedServer) {
            bootstrapFutureRef.getAndSet(null)?.complete(server)
        }
    }
}

