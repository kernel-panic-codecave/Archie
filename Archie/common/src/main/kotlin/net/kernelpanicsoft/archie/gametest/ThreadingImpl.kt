package net.kernelpanicsoft.archie.gametest

import net.minecraft.client.Minecraft
import java.util.concurrent.Phaser
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicReference

/**
 * Shared client gametest threading bridge inspired by Fabric's ThreadingImpl.
 *
 * Uses a Phaser for tick-phase barriers and semaphores for task handoff from
 * the gametest thread to client/server threads.
 */
object ThreadingImpl {
    private const val THREAD_IMPL_CLASS_NAME = "net.kernelpanicsoft.archie.gametest.ThreadingImpl"
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
    private var serverRegistered: Boolean = false

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
        check(Thread.currentThread() === testThread) {
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
            if (serverRegistered) {
                serverRegistered = false
                phaser.arriveAndDeregister()
            }
        }
    }

    @JvmStatic
    fun onServerRunStart() {
        if (!serverRegistered) {
            synchronized(this) {
                if (!serverRegistered) {
                    phaser.register()
                    serverRegistered = true
                }
            }
        }
    }

    @JvmStatic
    fun onServerRunStop() {
        serverCanAcceptTasks = false

        synchronized(this) {
            if (serverRegistered) {
                serverRegistered = false
                phaser.arriveAndDeregister()
            }
        }
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

        if (clientRegistered) {
            phaser.arrive()
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

        if (!serverRegistered) {
            synchronized(this) {
                if (!serverRegistered) {
                    phaser.register()
                    serverRegistered = true
                }
            }
        }

        serverCanAcceptTasks = true

        if (serverSemaphore.tryAcquire()) {
            taskToRun?.run()
        }

        if (serverRegistered) {
            phaser.arrive()
        }
    }

    @Suppress("unused")
    @JvmStatic
    fun runOnClient(action: () -> Unit) {
        checkOnGametestThread("runOnClient")
        ensureDispatchPhase()
        check(clientCanAcceptTasks) { "runOnClient called when no client is running" }
        runTaskOnOtherThread(action)
    }

    @Suppress("unused")
    @JvmStatic
    fun runOnServer(action: () -> Unit) {
        checkOnGametestThread("runOnServer")
        ensureDispatchPhase()
        check(serverCanAcceptTasks) { "runOnServer called when no server is running" }
        runTaskOnOtherThread(action, serverSemaphore)
    }

    private fun ensureDispatchPhase() {
        // Intentionally no-op for the current client harness bridge.
        // Dispatch relies on non-blocking client/server loop integration.
    }

    private fun runTaskOnOtherThread(action: () -> Unit) {
        runTaskOnOtherThread(action, clientSemaphore)
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

