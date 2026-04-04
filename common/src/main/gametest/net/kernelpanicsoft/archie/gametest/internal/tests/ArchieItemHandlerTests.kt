package net.kernelpanicsoft.archie.gametest.internal.tests

import earth.terrarium.common_storage_lib.resources.item.ItemResource
import net.kernelpanicsoft.archie.gametest.internal.EMPTY
import net.kernelpanicsoft.archie.transfer.ArchieItemStorage
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

@Suppress("unused")
class ArchieItemHandlerTests
{
	@GameTest(template = EMPTY)
	fun testInsertRespectsMaxStackSize(helper: GameTestHelper)
	{
		val storage = ArchieItemStorage(1)
		val stone = ItemResource.of(ItemStack(Items.STONE, 1))

		val inserted = storage.insert(stone, 80, false)

		assertEquals(helper, 64L, inserted)
		assertEquals(helper, 64, storage.get(0).getItem().count)
		helper.succeed()
	}

	@GameTest(template = EMPTY)
	fun testExtractToZeroClearsResource(helper: GameTestHelper)
	{
		val storage = ArchieItemStorage(1)
		val stone = ItemResource.of(ItemStack(Items.STONE, 1))

		storage.insert(stone, 10, false)
		val extracted = storage.extract(stone, 10, false)

		assertEquals(helper, 10L, extracted)
		assertTrue(helper, storage.get(0).getItem().isEmpty) {
			"Expected slot to be empty after full extraction"
		}
		helper.succeed()
	}

	@GameTest(template = EMPTY)
	fun testSimulatedInsertDoesNotMutateStorage(helper: GameTestHelper)
	{
		val storage = ArchieItemStorage(1)
		val diamond = ItemResource.of(ItemStack(Items.DIAMOND, 1))

		val inserted = storage.insert(diamond, 16, true)

		assertEquals(helper, 16L, inserted)
		assertTrue(helper, storage.get(0).getItem().isEmpty) {
			"Simulated insert should not mutate slot contents"
		}
		helper.succeed()
	}

	@GameTest(template = EMPTY)
	fun testSimulatedExtractDoesNotMutateStorage(helper: GameTestHelper)
	{
		val storage = ArchieItemStorage(1)
		val iron = ItemResource.of(ItemStack(Items.IRON_INGOT, 1))
		storage.insert(iron, 7, false)

		val extracted = storage.extract(iron, 4, true)

		assertEquals(helper, 4L, extracted)
		assertEquals(helper, 7, storage.get(0).getItem().count)
		helper.succeed()
	}
}

