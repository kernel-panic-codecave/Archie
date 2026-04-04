package net.kernelpanicsoft.archie.gametest

import net.minecraft.client.Minecraft
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

actual object AClientGameTestThreading {
    private val tickSemaphore = Semaphore(0)

    @Volatile
    private var testThread: Thread? = null

    actual fun runTestThread(testRunner: () -> Unit) {
        check(testThread == null) { "There is already a test thread running" }

        val thread = Thread {
            var failure: Throwable? = null
            try {
                testRunner()
            } catch (t: Throwable) {
                failure = t
            } finally {
                testThread = null
                if (failure != null) {
                    Minecraft.getInstance().execute {
                        throw failure as Throwable
                    }
                }
            }
        }

        thread.name = "Archie Client GameTest Thread"
        thread.isDaemon = true
        testThread = thread
        thread.start()
    }

    actual fun onClientTick() {
        tickSemaphore.release()
    }

    actual fun awaitTicks(ticks: Int, timeoutMillis: Long): Boolean {
        if (ticks <= 0) return true

        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis.coerceAtLeast(1L))
        repeat(ticks) {
            val remainingNanos = deadline - System.nanoTime()
            if (remainingNanos <= 0L) {
                return false
            }

            try {
                if (!tickSemaphore.tryAcquire(remainingNanos, TimeUnit.NANOSECONDS)) {
                    return false
                }
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                return false
            }
        }

        return true
    }
}

