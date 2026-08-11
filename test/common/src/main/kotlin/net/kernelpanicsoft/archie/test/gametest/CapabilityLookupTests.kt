package net.kernelpanicsoft.archie.test.gametest

import earth.terrarium.common_storage_lib.item.ItemApi
import net.kernelpanicsoft.archie.gametest.assertEquals
import net.kernelpanicsoft.archie.gametest.assertTrue
import net.kernelpanicsoft.archie.gametest.internal.EMPTY
import net.kernelpanicsoft.archie.test.BlockRegistry
import net.kernelpanicsoft.archie.test.TestTile
import net.kernelpanicsoft.archie.test.TileRegistry
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.ChestBlockEntity

/**
 * GameTest coverage for [net.kernelpanicsoft.archie.transfer.ArchieCapabilityExposure]: confirms
 * `exposeItemStorage` (registered against [TileRegistry.TestTile] by [CapabilityLookupTestFixtures])
 * actually reaches [ItemApi.BLOCK] (Common Storage Lib's real, platform-native lookup), not just
 * some Archie-internal bookkeeping.
 */
@Suppress("unused")
class CapabilityLookupTests
{
	@GameTest(template = EMPTY)
	fun GameTestHelper.testExposeItemStorageReachesItemApiBlock()
	{
		val pos = BlockPos(10, 2, 3)
		val state = BlockRegistry.TestBlock.defaultBlockState()
		val tile = TestTile(pos, state)

		val found = ItemApi.BLOCK.find(level, pos, state, tile, null)
		assertTrue(found === tile.items) { "Expected TestTile.items to be reachable via ItemApi.BLOCK.find" }
		succeed()
	}

	@GameTest(template = EMPTY)
	fun GameTestHelper.testExposeItemStorageRespectsDirection()
	{
		val pos = BlockPos(11, 2, 3)
		val state = BlockRegistry.TestBlock.defaultBlockState()
		val tile = TestTile(pos, state)

		assertTrue(ItemApi.BLOCK.find(level, pos, state, tile, Direction.UP) === tile.items) { "Expected UP to resolve TestTile.items" }
		assertEquals(null, ItemApi.BLOCK.find(level, pos, state, tile, Direction.DOWN)) { "Expected DOWN to be excluded by the fixture's selector" }
		succeed()
	}

	@GameTest(template = EMPTY)
	fun GameTestHelper.testUnexposedTypeIsNotFound()
	{
		// Vanilla's own chest is never exposed anywhere in this suite.
		val pos = BlockPos(12, 2, 3)
		val state = Blocks.CHEST.defaultBlockState()
		val blockEntity = ChestBlockEntity(pos, state)

		assertEquals(null, ItemApi.BLOCK.find(level, pos, state, blockEntity, null)) { "Expected an unexposed type to resolve to null" }
		succeed()
	}
}
