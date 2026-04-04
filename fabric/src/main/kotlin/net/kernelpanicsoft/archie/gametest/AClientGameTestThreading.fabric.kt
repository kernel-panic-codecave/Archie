package net.kernelpanicsoft.archie.gametest

actual object AClientGameTestThreading {
    actual fun runTestThread(testRunner: () -> Unit) {
        ThreadingImpl.runTestThread(testRunner)
    }

    actual fun onClientTick() {
        ThreadingImpl.onClientTick()
    }

    actual fun awaitTicks(ticks: Int, timeoutMillis: Long): Boolean {
        return ThreadingImpl.awaitTicks(ticks, timeoutMillis)
    }
}

