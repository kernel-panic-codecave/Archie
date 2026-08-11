package net.kernelpanicsoft.archie.gametest

import dev.architectury.platform.Mod
import net.kernelpanicsoft.archie.Archie
import net.kernelpanicsoft.archie.events.gametest.AGametestEvents
import net.kernelpanicsoft.archie.gametest.platform.AGameTestPlatform
import net.kernelpanicsoft.archie.gametest.platform.AGameTestSide
import net.kernelpanicsoft.archie.gametest.platform.ThreadingImpl
import net.minecraft.client.Minecraft
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Kicks off the client GameTest run once the client has finished loading past its title-screen
 * overlay, called from `MinecraftClientMixin.onTick` on every client tick.
 */
internal object AGameTestClientHarnessInternal {
    private val hasRun = AtomicBoolean(false)

    /**
     * No-ops unless this is a client-side GameTest run ([AGameTestPlatform.isGameTest] and
     * [AGameTestPlatform.side] `== CLIENT`) that hasn't started yet. Otherwise registers each mod's
     * test classes and runs them via [AClientGameTestHarness.run] on the dedicated test thread.
     *
     * The client process is always terminated afterward, on both success and failure - it must
     * exit with a non-zero code on failure, since [ThreadingImpl.runTestThread] catches and
     * stores any thrown exception rather than propagating it, so a plain `error(...)` throw here
     * would leave the client sitting at the title screen forever instead of failing the run
     * (which is what CI observed: the game never closed, so the Gradle task - and the whole
     * CI job - just hung until the outer timeout killed it).
     */
    @JvmStatic
    fun runIfNeeded()
    {
        if (!AGameTestPlatform.isGameTest || AGameTestPlatform.side != AGameTestSide.CLIENT) return
        if (!hasRun.compareAndSet(false, true)) return

        ThreadingImpl.runTestThread {
            val mods = AGametestEvents.MODS.ifEmpty { listOf(Archie.MOD) }
            mods.forEach { mod -> AGametestEvents.REGISTER_GAME_TEST.invoker()(mod) }

            val collected: Map<Mod, List<Class<*>>> = AGameTestPlatform.testClasses.mapValues { it.value.toList() }
            val summary = AClientGameTestHarness.run(collected, AGameTestPlatform.side)
            if (summary.failed > 0) {
                val details = summary.failedDetails.joinToString("\n") { failure ->
                    " - ${failure.testId}: ${failure.rootCause}"
                }
                Archie.LOGGER.error("Client GameTests failed: {} failing test(s)\n{}", summary.failed, details)
                kotlin.system.exitProcess(1)
            }
            Minecraft.getInstance().stop()
        }
    }
}
