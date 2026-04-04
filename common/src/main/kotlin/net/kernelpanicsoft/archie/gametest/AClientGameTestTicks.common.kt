package net.kernelpanicsoft.archie.gametest

expect object AClientGameTestTicks {
    fun onClientTick()

    suspend fun awaitTicks(ticks: Int, timeoutMillis: Long): Boolean
}




