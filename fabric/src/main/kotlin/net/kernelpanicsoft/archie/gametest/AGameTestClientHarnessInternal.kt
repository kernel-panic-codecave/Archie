package net.kernelpanicsoft.archie.gametest

import dev.architectury.platform.Mod
import net.kernelpanicsoft.archie.Archie
import net.kernelpanicsoft.archie.events.AEvents
import java.util.concurrent.atomic.AtomicBoolean

internal object AGameTestClientHarnessInternal {
    private val hasRun = AtomicBoolean(false)

    @JvmStatic
    fun runIfNeeded() {
        if (!AGameTestPlatform.isGameTest || AGameTestPlatform.side != AGameTestSide.CLIENT) return
        if (!hasRun.compareAndSet(false, true)) return

        val mods = if (AEvents.MODS.isEmpty()) listOf(Archie.MOD) else AEvents.MODS
        mods.forEach { mod -> AEvents.REGISTER_GAME_TEST.invoker()(mod) }

        val collected: Map<Mod, List<Class<*>>> = AGameTestPlatform.testClasses.mapValues { it.value.toList() }
        val summary = AClientGameTestHarness.run(collected, AGameTestPlatform.side)
        if (summary.failed > 0) {
            error("Client GameTests failed: ${summary.failed} failing test(s)")
        }
    }
}

