package net.kernelpanicsoft.archie.gametest.internal.tests

import earth.terrarium.common_storage_lib.resources.item.ItemResource
import net.kernelpanicsoft.archie.gametest.internal.EMPTY
import net.kernelpanicsoft.archie.transfer.ArchieItemStorage
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

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
	fun testReadSnapshotMergesByMinSlotCount(helper: GameTestHelper)
	{
		val source = ArchieItemStorage(2)
		source.get(0).set(ItemStack(Items.STONE, 4))
		source.get(1).set(ItemStack(Items.DIAMOND, 2))

		val target = ArchieItemStorage(1)
		target.readSnapshot(source.createSnapshot())

		assertEquals(helper, Items.STONE, target.get(0).getItem().item)
		assertEquals(helper, 4, target.get(0).getItem().count)
		helper.succeed()
	}

	@GameTest(template = EMPTY)
	fun testCreateSnapshotReadSnapshotRoundTrip(helper: GameTestHelper)
	{
		val source = ArchieItemStorage(2)
		source.get(0).set(ItemStack(Items.GOLD_INGOT, 3))
		source.get(1).set(ItemStack(Items.IRON_INGOT, 5))

		val target = ArchieItemStorage(2)
		target.readSnapshot(source.createSnapshot())

		assertEquals(helper, Items.GOLD_INGOT, target.get(0).getItem().item)
		assertEquals(helper, 3, target.get(0).getItem().count)
		assertEquals(helper, Items.IRON_INGOT, target.get(1).getItem().item)
		assertEquals(helper, 5, target.get(1).getItem().count)
		helper.succeed()
	}
}

