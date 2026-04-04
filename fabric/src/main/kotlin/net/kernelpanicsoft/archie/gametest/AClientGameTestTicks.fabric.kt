package net.kernelpanicsoft.archie.gametest

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeout

actual object AClientGameTestTicks {
    private val tickCount = MutableStateFlow(0L)

    actual fun onClientTick() {
        tickCount.update { it + 1L }
    }

    actual suspend fun awaitTicks(ticks: Int, timeoutMillis: Long): Boolean {
        if (ticks <= 0) return true

        val targetTick = tickCount.value + ticks
        return try {
            withTimeout(timeoutMillis) {
                tickCount.first { it >= targetTick }
                true
            }
        } catch (_: TimeoutCancellationException) {
            false
        }
    }
}

