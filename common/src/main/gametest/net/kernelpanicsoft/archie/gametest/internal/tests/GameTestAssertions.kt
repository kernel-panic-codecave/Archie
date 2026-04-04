package net.kernelpanicsoft.archie.gametest.internal.tests

internal fun assertTrue(condition: Boolean, message: () -> String)
{
	if (!condition) {
		throw AssertionError(message())
	}
}

internal fun <T> assertEquals(expected: T, actual: T, message: () -> String = { "Expected <$expected>, got <$actual>" })
{
	if (expected != actual) {
		throw AssertionError(message())
	}
}

internal inline fun <reified T : Throwable> expectThrows(noinline block: () -> Unit): T
{
	return try {
		block()
		throw AssertionError("Expected exception ${T::class.simpleName} to be thrown")
	} catch (t: Throwable) {
		if (t is T) t else throw AssertionError("Expected ${T::class.simpleName}, got ${t::class.simpleName}", t)
	}
}

