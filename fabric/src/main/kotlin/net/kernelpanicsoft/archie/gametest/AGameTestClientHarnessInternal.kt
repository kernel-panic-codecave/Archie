package net.kernelpanicsoft.archie.gametest

import dev.architectury.platform.Mod
import net.kernelpanicsoft.archie.Archie
import net.kernelpanicsoft.archie.events.AEvents
import net.minecraft.client.Minecraft
import java.util.concurrent.atomic.AtomicBoolean

internal object AGameTestClientHarnessInternal {
    private val hasRun = AtomicBoolean(false)

    @JvmStatic
    fun runIfNeeded()
    {
        if (!AGameTestPlatform.isGameTest || AGameTestPlatform.side != AGameTestSide.CLIENT) return
        if (!hasRun.compareAndSet(false, true)) return

        ThreadingImpl.runTestThread {
            val mods = AEvents.MODS.ifEmpty { listOf(Archie.MOD) }
            mods.forEach { mod -> AEvents.REGISTER_GAME_TEST.invoker()(mod) }

            val collected: Map<Mod, List<Class<*>>> = AGameTestPlatform.testClasses.mapValues { it.value.toList() }
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

