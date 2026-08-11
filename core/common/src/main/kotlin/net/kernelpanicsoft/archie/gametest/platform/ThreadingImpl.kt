package net.kernelpanicsoft.archie.gametest.platform

import net.minecraft.client.Minecraft
import java.util.concurrent.Phaser
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicReference

/**
 * Tracks which server instance's tick thread currently owns the shared "server" phaser slot.
 *
 * The `ServerMixin` (per-loader, in `src/main/mixin`) this bridge backs is applied to every
 * [net.minecraft.server.MinecraftServer] instance - both the integrated singleplayer server and
 * an in-process dedicated GameTest server can exist back-to-back (or briefly overlap during
 * teardown/startup). Since only one
 * "server" participant can safely register with [ThreadingImpl.phaser] at a time, this
 * identifies the owning thread so a late call from an already-superseded instance can't
 * deregister or arrive on behalf of a different, currently-active instance.
 */
private val serverRegisteredThread = AtomicReference<Thread?>(null)

/**
 * Shared client gametest threading bridge inspired by Fabric's ThreadingImpl.
 *
 * Uses a Phaser for tick-phase barriers and semaphores for task handoff from
 * the gametest thread to client/server threads.
 */
object ThreadingImpl {
    private const val THREAD_IMPL_CLASS_NAME = "net.kernelpanicsoft.archie.gametest.platform.ThreadingImpl"
    private const val TASK_ON_THIS_THREAD_METHOD_NAME = "runTaskOnThisThread"
    private const val TASK_ON_OTHER_THREAD_METHOD_NAME = "runTaskOnOtherThread"
    private const val PHASE_MASK = 3
    private const val PHASE_TICK = 0
    private const val PHASE_CLIENT_TASKS = 1
    private const val PHASE_SERVER_TASKS = 2
    private const val PHASE_TEST = 3

    private val clientSemaphore = Semaphore(0)
    private val serverSemaphore = Semaphore(0)
    private val testSemaphore = Semaphore(0)
    private val phaser = Phaser(0)

    @Volatile
    private var clientCanAcceptTasks: Boolean = false

    @Volatile
    private var serverCanAcceptTasks: Boolean = false

    @Volatile
    private var clientRegistered: Boolean = false

    @Volatile
    private var testRegistered: Boolean = false

    @Volatile
    private var taskToRun: Runnable? = null

    @Volatile
    private var testThread: Thread? = null

    @Volatile
    var testFailureException: Throwable? = null
        private set

    @Volatile
    private var gameCrashed: Boolean = false

    // The phase each tick source last called phaser.arrive() for. onClientTick()/onServerTick()
    // fire every real tick regardless of whether the test thread has caught up and arrived for
    // the current phase yet - Phaser requires each registered party to arrive at most once per
    // phase, so without this guard, two ticks landing before the test thread's next arrival (more
    // likely under CI's slower/more contended scheduling - never reproduced on a fast local
    // machine) throws "Attempted arrival of unregistered party" on the second one. Reading the
    // phase this call actually arrived for straight off arrive()'s return value (rather than a
    // separate phaser.phase read beforehand) avoids a TOCTOU gap between checking and arriving.
    // Reset to -1 on (re-)registration in onClientRunStart()/onServerRunStart() - the phase
    // counter doesn't reset when a party deregisters, so a stale value surviving into a new
    // registration could wrongly skip that new party's first required arrival and stall forever.
    @Volatile
    private var clientLastArrivedPhase: Int = -1

    @Volatile
    private var serverLastArrivedPhase: Int = -1

    @JvmStatic
    fun runTestThread(testRunner: () -> Unit) {
        check(testThread == null) { "There is already a test thread running" }
        testFailureException = null
        clientCanAcceptTasks = false
        serverCanAcceptTasks = false

        val thread = Thread {
            if (!testRegistered) {
                synchronized(this) {
                    if (!testRegistered) {
                        phaser.register()
                        testRegistered = true
                    }
                }
            }

            try {
                testRunner()
            } catch (failure: Throwable) {
                testFailureException = failure
            } finally {
                synchronized(this) {
                    if (testRegistered) {
                        testRegistered = false
                        phaser.arriveAndDeregister()
                    }
                }
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

    @JvmStatic
    fun checkOnGametestThread(methodName: String) {
        check(isOnGametestThread()) {
            "$methodName can only be called from the client gametest thread"
        }
    }

    @JvmStatic
    fun isOnGametestThread(): Boolean = Thread.currentThread() === testThread

    @JvmStatic
    fun onClientRunStart() {
        gameCrashed = false
        if (!clientRegistered) {
            synchronized(this) {
                if (!clientRegistered) {
                    phaser.register()
                    clientRegistered = true
                    // A fresh registration must not inherit a previous instance's arrival
                    // history - the phaser's phase counter doesn't reset just because the
                    // prior party deregistered, so a stale value here could make this new
                    // party's first tick wrongly believe it already arrived for the current
                    // phase, permanently stalling that phase (see clientLastArrivedPhase kdoc).
                    clientLastArrivedPhase = -1
                }
            }
        }
    }

    @JvmStatic
    fun onClientRunStop() {
        clientCanAcceptTasks = false
        serverCanAcceptTasks = false

        synchronized(this) {
            if (clientRegistered) {
                clientRegistered = false
                phaser.arriveAndDeregister()
            }
        }

        // Force-release the server slot regardless of which instance holds it - the client is
        // shutting down entirely, so nothing should be left registered afterward.
        if (serverRegisteredThread.getAndSet(null) != null) {
            synchronized(this) {
                phaser.arriveAndDeregister()
            }
        }
    }

    @JvmStatic
    fun onServerRunStart() {
        val current = Thread.currentThread()
        if (serverRegisteredThread.compareAndSet(null, current)) {
            synchronized(this) {
                phaser.register()
                // See the matching comment in onClientRunStart() - a new server instance
                // (e.g. an integrated singleplayer server starting after an earlier dedicated
                // GameTest server already registered, arrived, and deregistered) must not
                // inherit the previous instance's last-arrived phase.
                serverLastArrivedPhase = -1
            }
        }
        // If another server instance's thread already holds the slot (e.g. an integrated
        // server that hasn't finished tearing down yet), this instance simply won't
        // participate in tick-phase sync until that one releases it - see onServerTick().
    }

    @JvmStatic
    fun onServerRunStop() {
        serverCanAcceptTasks = false

        val current = Thread.currentThread()
        if (serverRegisteredThread.compareAndSet(current, null)) {
            synchronized(this) {
                phaser.arriveAndDeregister()
            }
        }
        // If this thread never held the slot, it never registered either - nothing to release.
    }

    @JvmStatic
    fun setGameCrashed() {
        gameCrashed = true
        onClientRunStop()
    }

    @JvmStatic
    fun onClientTick() {
        if (testThread == null && !testRegistered) return

        if (!clientRegistered) {
            synchronized(this) {
                if (!clientRegistered) {
                    phaser.register()
                    clientRegistered = true
                }
            }
        }

        clientCanAcceptTasks = true

        if (clientSemaphore.tryAcquire()) {
            taskToRun?.run()
        }

        if (clientRegistered && phaser.phase != clientLastArrivedPhase) {
            clientLastArrivedPhase = phaser.arrive()
        }
    }

    @JvmStatic
    fun preRunTasks() {
        if (!isThreadingActive()) return
    }

    @JvmStatic
    fun postRunTasks() {
        if (!isThreadingActive()) return

        clientCanAcceptTasks = true

        while (clientSemaphore.tryAcquire()) {
            val task = taskToRun ?: break
            task.run()
        }
    }

    @JvmStatic
    fun onServerTick() {
        if (testThread == null && !testRegistered) return

        val current = Thread.currentThread()
        if (serverRegisteredThread.compareAndSet(null, current)) {
            synchronized(this) {
                phaser.register()
            }
        }

        if (serverRegisteredThread.get() !== current) {
            // Another server instance already owns the shared slot (e.g. this is a dedicated
            // GameTest server ticking while the integrated server hasn't finished tearing
            // down yet). Don't touch the semaphore/phaser on its behalf.
            return
        }

        serverCanAcceptTasks = true

        if (serverSemaphore.tryAcquire()) {
            taskToRun?.run()
        }

        if (phaser.phase != serverLastArrivedPhase) {
            serverLastArrivedPhase = phaser.arrive()
        }
    }

    @Suppress("unused")
    @JvmStatic
    fun runOnClient(action: () -> Unit) {
        checkOnGametestThread("runOnClient")
        ensureDispatchPhase()
        check(clientCanAcceptTasks) { "runOnClient called when no client is running" }
        runTaskOnOtherThread(action, clientSemaphore)
    }

    @Suppress("unused")
    @JvmStatic
    fun runOnServer(action: () -> Unit) {
        checkOnGametestThread("runOnServer")
        ensureDispatchPhase()
        check(serverCanAcceptTasks) {
            "runOnServer called when no server is running " +
                "(serverRegisteredThread=${serverRegisteredThread.get()?.name}, " +
                "testRegistered=$testRegistered, testThread=${testThread?.name}, phase=${getCurrentPhase()})"
        }
        runTaskOnOtherThread(action, serverSemaphore)
    }

    private fun ensureDispatchPhase() {
        // Intentionally no-op for the current client harness bridge.
        // Dispatch relies on non-blocking client/server loop integration.
    }

    private fun runTaskOnOtherThread(action: () -> Unit, targetSemaphore: Semaphore) {
        val thrown = AtomicReference<Throwable?>(null)
        taskToRun = Runnable { runTaskOnThisThread(action, thrown) }

        targetSemaphore.release()

        try {
            val acquired = testSemaphore.tryAcquire(10, TimeUnit.SECONDS)
            check(acquired) {
                "Timed out waiting for cross-thread task completion " +
                    "(phase=${getCurrentPhase()}, nextPhase=${getNextPhase()}, " +
                    "clientCanAcceptTasks=$clientCanAcceptTasks, serverCanAcceptTasks=$serverCanAcceptTasks, " +
                    "target=${if (targetSemaphore === clientSemaphore) "client" else "server"}, " +
                    "taskPending=${taskToRun != null}, testThreadAlive=${testThread?.isAlive == true})"
            }
        } catch (e: InterruptedException) {
            throw RuntimeException(e)
        }

        val error = thrown.get()
        if (error != null) {
            joinAsyncStackTrace(error)
            throw error
        }
    }

    private fun runTaskOnThisThread(action: () -> Unit, thrown: AtomicReference<Throwable?>) {
        try {
            action()
        } catch (e: Throwable) {
            thrown.set(e)
        } finally {
            taskToRun = null
            testSemaphore.release()
        }
    }

    private fun joinAsyncStackTrace(error: Throwable) {
        if (System.getProperty("fabric.client.gametest.disableJoinAsyncStackTraces") != null) {
            return
        }

        val otherThreadStackTrace = error.stackTrace ?: return
        var otherThreadIndex = otherThreadStackTrace.size - 1
        while (otherThreadIndex >= 0) {
            val element = otherThreadStackTrace[otherThreadIndex]
            if (THREAD_IMPL_CLASS_NAME == element.className && TASK_ON_THIS_THREAD_METHOD_NAME == element.methodName) {
                break
            }
            otherThreadIndex--
        }

        if (otherThreadIndex == -1) {
            return
        }

        val thisThreadStackTrace = Thread.currentThread().stackTrace
        var thisThreadIndex = 0
        while (thisThreadIndex < thisThreadStackTrace.size) {
            val element = thisThreadStackTrace[thisThreadIndex]
            if (THREAD_IMPL_CLASS_NAME == element.className && TASK_ON_OTHER_THREAD_METHOD_NAME == element.methodName) {
                break
            }
            thisThreadIndex++
        }

        if (thisThreadIndex == thisThreadStackTrace.size) {
            return
        }

        val joinedStackTrace = arrayOfNulls<StackTraceElement>(
            (otherThreadIndex + 1) + 1 + (thisThreadStackTrace.size - thisThreadIndex),
        )
        System.arraycopy(otherThreadStackTrace, 0, joinedStackTrace, 0, otherThreadIndex + 1)
        joinedStackTrace[otherThreadIndex + 1] = StackTraceElement("Async Stack Trace", ".", null, 1)
        System.arraycopy(
            thisThreadStackTrace,
            thisThreadIndex,
            joinedStackTrace,
            otherThreadIndex + 2,
            thisThreadStackTrace.size - thisThreadIndex,
        )
        @Suppress("UNCHECKED_CAST")
        error.stackTrace = joinedStackTrace as Array<StackTraceElement>
    }

    @JvmStatic
    fun awaitTicks(ticks: Int, timeoutMillis: Long): Boolean {
        if (gameCrashed) return false
        if (ticks <= 0) return true

        if (!testRegistered) {
            synchronized(this) {
                if (!testRegistered) {
                    phaser.register()
                    testRegistered = true
                }
            }
        }

        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis.coerceAtLeast(1L))
        repeat(ticks) {
            val phase = advanceToNextTickPhase()
            val remainingNanos = deadline - System.nanoTime()
            if (remainingNanos <= 0L) return false

            try {
                phaser.awaitAdvanceInterruptibly(phase, remainingNanos, TimeUnit.NANOSECONDS)
            } catch (_: TimeoutException) {
                return false
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                return false
            }
        }

        return true
    }

    @Suppress("unused")
    private fun getCurrentPhase(): Int = (phaser.phase - 1) and PHASE_MASK

    @Suppress("unused")
    private fun getNextPhase(): Int = phaser.phase and PHASE_MASK

    @Suppress("unused")
    private fun enterPhase(phase: Int) {
        while (getNextPhase() != phase) {
            phaser.arriveAndAwaitAdvance()
        }

        // After aligning to the requested next phase, participate in that
        // phase barrier as well. Without this, callers can observe the phase
        // but not synchronize with peer threads at the same boundary.
        phaser.arriveAndAwaitAdvance()
    }

    private fun advanceToNextTickPhase(): Int {
        check(PHASE_TICK == 0 && PHASE_CLIENT_TASKS == 1 && PHASE_SERVER_TASKS == 2 && PHASE_TEST == 3)
        return phaser.arrive()
    }

    private fun isThreadingActive(): Boolean = testThread != null || testRegistered
}

