package net.kernelpanicsoft.archie.gametest

import dev.architectury.platform.Mod
import net.kernelpanicsoft.archie.Archie
import net.kernelpanicsoft.archie.events.AEvents
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
     * test classes, runs them via [AClientGameTestHarness.run] on the dedicated test thread, and
     * either throws with a failure summary or stops the client on success.
     */
    @JvmStatic
    fun runIfNeeded()
    {
        if (!AGameTestPlatform.isGameTest || AGameTestPlatform.side != AGameTestSide.CLIENT) return
        if (!hasRun.compareAndSet(false, true)) return

        ThreadingImpl.runTestThread {
            val mods = AEvents.MODS.ifEmpty { listOf(Archie.MOD) }
            mods.forEach { mod -> AEvents.REGISTER_GAME_TEST.invoker()(mod) }

            val collected: Map<Mod, List<Class<*>>> = AGameTestPlatformInternal.testClasses.mapValues { it.value.toList() }
            val summary = AClientGameTestHarness.run(collected, AGameTestPlatform.side)
            if (summary.failed > 0) {
                val details = summary.failedDetails.joinToString("\n") { failure ->
                    " - ${failure.testId}: ${failure.rootCause}"
                }
                error("Client GameTests failed: ${summary.failed} failing test(s)\n$details")
            }
            Minecraft.getInstance().stop()
        }
    }
}

