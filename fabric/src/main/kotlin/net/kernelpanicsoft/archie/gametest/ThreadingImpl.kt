package net.kernelpanicsoft.archie.gametest

import net.minecraft.client.Minecraft
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Fabric client gametest threading backport inspired by Fabric API's ThreadingImpl.
 *
 * This is intentionally minimal for 1.21.1: it provides a dedicated gametest thread,
 * client-thread task handoff, and tick stepping used by the Archie harness.
 */
internal object ThreadingImpl {
    private const val CLIENT_ACTION_TIMEOUT_SECONDS = 10L

    @Volatile
    private var testThread: Thread? = null

    @Volatile
    var testFailureException: Throwable? = null
        private set

    fun runTestThread(testRunner: () -> Unit) {
        check(testThread == null) { "There is already a test thread running" }
        testFailureException = null

        val thread = Thread {
            try {
                testRunner()
            } catch (failure: Throwable) {
                testFailureException = failure
            } finally {
                val capturedFailure = testFailureException
                testThread = null
                if (capturedFailure != null) {
                    Minecraft.getInstance().execute {
                        throw capturedFailure
                    }
                }
            }
        }
        thread.name = "Archie Client GameTest Thread"
        thread.isDaemon = true
        testThread = thread
        thread.start()
    }

    fun checkOnGametestThread(methodName: String) {
        check(Thread.currentThread() === testThread) {
            "$methodName can only be called from the client gametest thread"
        }
    }

    fun runTick() {
        checkOnGametestThread("runTick")
        check(AClientGameTestTicks.awaitTicks(1, 1000L)) {
            "Timed out waiting for next client tick"
        }
    }

    fun <T> runOnClient(action: (Minecraft) -> T): T {
        checkOnGametestThread("runOnClient")
        val client = Minecraft.getInstance()

        if (client.isSameThread) {
            return action(client)
        }

        var value: T? = null
        var throwable: Throwable? = null
        val latch = CountDownLatch(1)
        client.execute {
            runCatching { action(client) }
                .onSuccess { value = it }
                .onFailure { throwable = it }
            latch.countDown()
        }

        check(latch.await(CLIENT_ACTION_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            "Timed out waiting for client thread action"
        }

        throwable?.let { throw it }
        @Suppress("UNCHECKED_CAST")
        return value as T
    }
}

