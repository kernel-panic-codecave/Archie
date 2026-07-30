package net.kernelpanicsoft.archie.gametest.internal.tests

import earth.terrarium.common_storage_lib.resources.item.ItemResource
import net.kernelpanicsoft.archie.gametest.internal.EMPTY
import net.kernelpanicsoft.archie.transfer.ArchieItemStorage
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

/**
 * GameTest coverage for [ArchieItemStorage]: max-stack-size clamping on insert, resource
 * clearing on full extraction, and that simulated insert/extract calls never mutate storage.
 */
@Suppress("unused")
class ArchieItemHandlerTests
{
	@GameTest(template = EMPTY)
	fun GameTestHelper.testInsertRespectsMaxStackSize()
	{
		val storage = ArchieItemStorage(1)
		val stone = ItemResource.of(ItemStack(Items.STONE, 1))

		val inserted = storage.insert(stone, 80, false)

		assertEquals(64L, inserted)
		assertEquals(64, storage.get(0).getItem().count)
		succeed()
	}

	@GameTest(template = EMPTY)
	fun GameTestHelper.testExtractToZeroClearsResource()
	{
		val storage = ArchieItemStorage(1)
		val stone = ItemResource.of(ItemStack(Items.STONE, 1))

		storage.insert(stone, 10, false)
		val extracted = storage.extract(stone, 10, false)

		assertEquals(10L, extracted)
		assertTrue(storage.get(0).getItem().isEmpty) {
			"Expected slot to be empty after full extraction"
		}
		succeed()
	}

	@GameTest(template = EMPTY)
	fun GameTestHelper.testSimulatedInsertDoesNotMutateStorage()
	{
		val storage = ArchieItemStorage(1)
		val diamond = ItemResource.of(ItemStack(Items.DIAMOND, 1))

		val inserted = storage.insert(diamond, 16, true)

		assertEquals(16L, inserted)
		assertTrue(storage.get(0).getItem().isEmpty) {
			"Simulated insert should not mutate slot contents"
		}
		succeed()
	}

	@GameTest(template = EMPTY)
	fun GameTestHelper.testSimulatedExtractDoesNotMutateStorage()
	{
		val storage = ArchieItemStorage(1)
		val iron = ItemResource.of(ItemStack(Items.IRON_INGOT, 1))
		storage.insert(iron, 7, false)

		val extracted = storage.extract(iron, 4, true)

		assertEquals(4L, extracted)
		assertEquals(7, storage.get(0).getItem().count)
		succeed()
	}
}
