package net.kernelpanicsoft.archie.gametest.internal.tests

import earth.terrarium.common_storage_lib.resources.fluid.FluidResource
import net.kernelpanicsoft.archie.gametest.assertEquals
import net.kernelpanicsoft.archie.gametest.assertTrue
import net.kernelpanicsoft.archie.gametest.internal.EMPTY
import net.kernelpanicsoft.archie.transfer.ArchieFluidStorage
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.level.material.Fluids

/**
 * GameTest coverage for [ArchieFluidStorage]/[net.kernelpanicsoft.archie.transfer.ArchieFluidSlot],
 * centred on one thing the item side never has to worry about: **matching a fluid by value**.
 *
 * Common Storage Lib's `FluidResource` overrides neither `equals` nor `hashCode`, so any `==` on it
 * is reference identity. A slot that compared that way accepted the first insert into a blank slot
 * and then silently rejected every later one, because a resource that has been round-tripped
 * through NBT or rebuilt from a packet is a different object even when it is plainly the same
 * fluid. Every test here therefore builds a **fresh** `FluidResource` per call rather than reusing
 * one - reusing a single instance is exactly what let the bug hide.
 */
@Suppress("unused")
class ArchieFluidHandlerTests
{
	/** A new instance every time, deliberately - see this class's own KDoc. */
	private fun water() = FluidResource.of(Fluids.WATER)

	private fun lava() = FluidResource.of(Fluids.LAVA)

	@GameTest(template = EMPTY)
	fun GameTestHelper.testTopUpAcceptsAnEqualButDistinctResource()
	{
		val storage = ArchieFluidStorage(BUCKET * 4, 1)

		assertEquals(BUCKET, storage.insert(water(), BUCKET, false))
		// The second insert is the one that used to return 0.
		assertEquals(BUCKET, storage.insert(water(), BUCKET, false))
		assertEquals(BUCKET * 2, storage.getAmount(0))
		succeed()
	}

	@GameTest(template = EMPTY)
	fun GameTestHelper.testExtractAcceptsAnEqualButDistinctResource()
	{
		val storage = ArchieFluidStorage(BUCKET * 4, 1)
		storage.insert(water(), BUCKET * 2, false)

		assertEquals(BUCKET * 2, storage.extract(water(), BUCKET * 2, false))
		assertTrue(storage.getResource(0).isBlank) {
			"Expected the slot to clear once fully drained, got ${storage.getResource(0)}"
		}
		succeed()
	}

	@GameTest(template = EMPTY)
	fun GameTestHelper.testInsertRespectsCapacity()
	{
		val storage = ArchieFluidStorage(BUCKET, 1)

		assertEquals(BUCKET, storage.insert(water(), BUCKET * 3, false))
		assertEquals(BUCKET, storage.getAmount(0))
		succeed()
	}

	/** Matching by value must not go so far as matching a *different* fluid. */
	@GameTest(template = EMPTY)
	fun GameTestHelper.testADifferentFluidIsStillRejected()
	{
		val storage = ArchieFluidStorage(BUCKET * 4, 1)
		storage.insert(water(), BUCKET, false)

		assertEquals(0L, storage.insert(lava(), BUCKET, false))
		assertEquals(0L, storage.extract(lava(), BUCKET, false))
		assertEquals(BUCKET, storage.getAmount(0))
		succeed()
	}

	@GameTest(template = EMPTY)
	fun GameTestHelper.testSimulatedCallsDoNotMutateStorage()
	{
		val storage = ArchieFluidStorage(BUCKET * 4, 1)
		storage.insert(water(), BUCKET, false)

		storage.insert(water(), BUCKET, true)
		storage.extract(water(), BUCKET, true)

		assertEquals(BUCKET, storage.getAmount(0))
		succeed()
	}

	private companion object
	{
		/**
		 * A bucket in platform units, spelled out rather than read from `FluidAmounts.BUCKET` -
		 * Common Storage Lib 0.0.5 leaves that constant (and every other `FluidAmounts` field) at
		 * `0` on both loaders, which would make every assertion here trivially true.
		 */
		const val BUCKET = 81_000L
	}
}
