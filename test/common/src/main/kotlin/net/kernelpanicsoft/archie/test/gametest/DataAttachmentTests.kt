package net.kernelpanicsoft.archie.test.gametest

import net.kernelpanicsoft.archie.gametest.internal.EMPTY
import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.ChestBlockEntity

/**
 * GameTest coverage for [net.kernelpanicsoft.archie.serialization.ArchieDataAttachment]/
 * [net.kernelpanicsoft.archie.serialization.AttachmentRegistry], via [DataAttachmentTestFixtures].
 * Uses vanilla entity/block-entity/item types throughout - attachments are generic across any
 * supported holder kind, so there's no need for a custom registered fixture type the way
 * [CapabilityLookupTests] needed [net.kernelpanicsoft.archie.test.TileRegistry.TestTile].
 */
@Suppress("unused")
class DataAttachmentTests
{
	@GameTest(template = EMPTY)
	fun GameTestHelper.testGetReturnsDefaultBeforeSet()
	{
		val pig = spawnWithNoFreeWill(EntityType.PIG, BlockPos(1, 2, 1))

		// has() must be checked *before* any get() call on an Entity/BlockEntity holder: Fabric's
		// AttachmentTarget.getAttachedOrCreate (what get() calls under the hood there) silently
		// creates *and persists* the default on first read, so has() can no longer distinguish
		// "never touched" from "read once" afterward. See ArchieDataAttachment's KDoc.
		if (DataAttachmentTestFixtures.counter.has(pig)) fail("Expected a freshly-spawned entity to have no explicitly-set value")
		if (DataAttachmentTestFixtures.counter.get(pig) != 0) fail("Expected a freshly-spawned entity to read back the default value")
		succeed()
	}

	@GameTest(template = EMPTY)
	fun GameTestHelper.testSetGetHasRemoveRoundTrip()
	{
		val pig = spawnWithNoFreeWill(EntityType.PIG, BlockPos(2, 2, 1))
		val attachment = DataAttachmentTestFixtures.counter

		attachment.set(pig, 5)
		if (attachment.get(pig) != 5) fail("Expected get() to return the value just set()")
		if (!attachment.has(pig)) fail("Expected has() to be true after set()")

		attachment.remove(pig)
		// Check has() immediately after remove(), before the get() below re-triggers the same
		// auto-persist-on-read behavior described above.
		if (attachment.has(pig)) fail("Expected has() to be false immediately after remove()")
		if (attachment.get(pig) != 0) fail("Expected get() to revert to the default after remove()")
		succeed()
	}

	@GameTest(template = EMPTY)
	fun GameTestHelper.testModifyAppliesFunctionAndPersists()
	{
		val pig = spawnWithNoFreeWill(EntityType.PIG, BlockPos(3, 2, 1))
		val attachment = DataAttachmentTestFixtures.counter

		attachment.set(pig, 5)
		val result = attachment.modify(pig) { it + 1 }

		if (result != 6) fail("Expected modify() to return the new value")
		if (attachment.get(pig) != 6) fail("Expected modify() to persist the new value")
		succeed()
	}

	@GameTest(template = EMPTY)
	fun GameTestHelper.testExtensionPropertyDelegateRoundTrip()
	{
		val pig = spawnWithNoFreeWill(EntityType.PIG, BlockPos(4, 2, 1))

		pig.testCounter = 42
		if (pig.testCounter != 42) fail("Expected the by-delegated extension property to read back what it just wrote")
		if (DataAttachmentTestFixtures.counter.get(pig) != 42) fail("Expected the extension property and the direct get() call to observe the same value")
		succeed()
	}

	@GameTest(template = EMPTY)
	fun GameTestHelper.testBlockEntityAttachmentRoundTrip()
	{
		val pos = BlockPos(5, 2, 1)
		setBlock(pos, Blocks.CHEST)
		val chest = getBlockEntity<ChestBlockEntity>(pos)
		val attachment = DataAttachmentTestFixtures.counter

		if (attachment.has(chest)) fail("Expected a freshly-placed block entity to have no explicitly-set value")
		attachment.set(chest, 7)
		if (attachment.get(chest) != 7) fail("Expected get() to return the value just set() on a block entity")
		if (!attachment.has(chest)) fail("Expected has() to be true after set() on a block entity")
		succeed()
	}

	@GameTest(template = EMPTY)
	fun GameTestHelper.testItemComponentAttachmentRoundTripsOnItemStack()
	{
		val stack = ItemStack(Items.STONE)
		val attachment = DataAttachmentTestFixtures.label

		if (attachment.get(stack) != "") fail("Expected a fresh ItemStack to read back the default value")
		if (attachment.has(stack)) fail("Expected a fresh ItemStack to have no explicitly-set value")

		attachment.set(stack, "hello")
		if (attachment.get(stack) != "hello") fail("Expected get() to return the value just set() on an ItemStack")
		if (!attachment.has(stack)) fail("Expected has() to be true after set() on an ItemStack")

		attachment.remove(stack)
		if (attachment.get(stack) != "") fail("Expected get() to revert to the default after remove()")
		succeed()
	}

	@GameTest(template = EMPTY)
	fun GameTestHelper.testUnexposedItemComponentAttachmentThrowsOnItemStack()
	{
		// `counter` was declared without itemComponent = true, so it has no DataComponentType to
		// read/write - ItemStack still passes CSL's DataComponentHolder check either way, so this
		// fails with NullPointerException (a null componentType()), not IllegalArgumentException.
		val stack = ItemStack(Items.STONE)
		try {
			DataAttachmentTestFixtures.counter.get(stack)
			fail("Expected NullPointerException reading an ItemStack through an attachment with no itemComponent")
		} catch (_: NullPointerException) {
			// expected
		}
		succeed()
	}

	@GameTest(template = EMPTY)
	fun GameTestHelper.testUnsupportedHolderThrowsIllegalArgumentException()
	{
		// A plain BlockPos is neither an attachment holder nor a DataComponentHolder.
		try {
			DataAttachmentTestFixtures.counter.get(BlockPos(0, 0, 0))
			fail("Expected IllegalArgumentException reading an unsupported holder type")
		} catch (_: IllegalArgumentException) {
			// expected
		}
		succeed()
	}
}
