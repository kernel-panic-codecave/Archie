package net.kernelpanicsoft.archie.gametest.internal.tests

import net.minecraft.gametest.framework.GameTestHelper

internal fun assertTrue(helper: GameTestHelper, condition: Boolean, message: () -> String)
{
	if (!condition) {
		helper.fail(message())
	}
}

internal fun <T> assertEquals(
	helper: GameTestHelper,
	expected: T,
	actual: T,
	message: () -> String = { "Expected <$expected>, got <$actual>" },
)
{
	if (expected != actual) {
		helper.fail(message())
	}
}

internal inline fun <reified T : Throwable> expectThrows(helper: GameTestHelper, noinline block: () -> Unit): T
{
	return try {
		block()
		helper.fail("Expected exception ${T::class.simpleName} to be thrown")
		throw IllegalStateException("Unreachable")
	} catch (t: Throwable) {
		if (t is T) t
		else {
			helper.fail("Expected ${T::class.simpleName}, got ${t::class.simpleName}")
			throw IllegalStateException("Unreachable", t)
		}
	}
}


