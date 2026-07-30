package net.kernelpanicsoft.archie.gametest.internal.tests

import net.minecraft.gametest.framework.GameTestHelper

/**
 * Fails this GameTest via [GameTestHelper.fail] with [message] if [condition] is `false`.
 */
internal fun GameTestHelper.assertTrue(condition: Boolean, message: () -> String)
{
	if (!condition) {
		fail(message())
	}
}

/**
 * Fails this GameTest via [GameTestHelper.fail] if [expected] and [actual] are not equal.
 *
 * @param message Failure message builder; defaults to reporting both values.
 */
internal fun <T> GameTestHelper.assertEquals(
	expected: T,
	actual: T,
	message: () -> String = { "Expected <$expected>, got <$actual>" },
)
{
	if (expected != actual) {
		fail(message())
	}
}

/**
 * Runs [block] and asserts it throws a [T], failing this GameTest via [GameTestHelper.fail]
 * if [block] completes without throwing or throws a different exception type.
 *
 * @return The caught exception of type [T].
 */
internal inline fun <reified T : Throwable> GameTestHelper.expectThrows(noinline block: () -> Unit): T
{
	return try {
		block()
		fail("Expected exception ${T::class.simpleName} to be thrown")
		throw IllegalStateException("Unreachable")
	} catch (t: Throwable) {
		if (t is T) t
		else {
			fail("Expected ${T::class.simpleName}, got ${t::class.simpleName}")
			throw IllegalStateException("Unreachable", t)
		}
	}
}


