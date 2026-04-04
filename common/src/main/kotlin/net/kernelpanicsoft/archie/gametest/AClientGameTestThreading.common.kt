package net.kernelpanicsoft.archie.gametest

expect object AClientGameTestThreading {
    fun awaitTicks(ticks: Int, timeoutMillis: Long): Boolean
}

