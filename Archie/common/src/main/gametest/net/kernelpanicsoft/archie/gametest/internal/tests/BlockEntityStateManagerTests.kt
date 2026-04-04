package net.kernelpanicsoft.archie.gametest.internal.tests

import kotlinx.serialization.builtins.serializer
import net.kernelpanicsoft.archie.gametest.internal.EMPTY
import net.kernelpanicsoft.archie.gui.blockentity.BlockEntityStateManager
import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.ChestBlockEntity

@Suppress("unused")
class BlockEntityStateManagerTests
{
	@GameTest(template = EMPTY)
	fun testRegisterReturnsStableContainer(helper: GameTestHelper)
	{
		BlockEntityStateManager.clear()
		val blockEntity = ChestBlockEntity(BlockPos(1, 2, 3), Blocks.CHEST.defaultBlockState())

		val first = BlockEntityStateManager.registerBlockEntity(blockEntity)
		val second = BlockEntityStateManager.registerBlockEntity(blockEntity)

		assertTrue(helper, first === second) { "Expected the same container instance for repeated registration" }
		assertTrue(helper, BlockEntityStateManager.getContainer(blockEntity) != null) { "Expected container to be retrievable" }
		BlockEntityStateManager.clear()
		helper.succeed()
	}

	@GameTest(template = EMPTY)
	fun testSyncSkipsWithoutTrackedPlayers(helper: GameTestHelper)
	{
		BlockEntityStateManager.clear()
		val blockEntity = ChestBlockEntity(BlockPos(2, 2, 3), Blocks.CHEST.defaultBlockState())
		val container = BlockEntityStateManager.registerBlockEntity(blockEntity)
		container.setPropertySerializer("energy", Int.serializer())
		container.updateProperty("energy", 99)

		var packetsSent = 0
		BlockEntityStateManager.syncDirtyEntities(40L) { _, _ -> packetsSent++ }

		assertEquals(helper, 0, packetsSent)
		assertTrue(helper, container.isDirty) { "Container should remain dirty until a packet is sent" }
		BlockEntityStateManager.clear()
		helper.succeed()
	}

	@GameTest(template = EMPTY)
	fun testUnregisterRemovesContainer(helper: GameTestHelper)
	{
		BlockEntityStateManager.clear()
		val blockEntity = ChestBlockEntity(BlockPos(3, 2, 3), Blocks.CHEST.defaultBlockState())

		val container = BlockEntityStateManager.registerBlockEntity(blockEntity)
		container.setPropertySerializer("progress", Int.serializer())
		container.updateProperty("progress", 3)

		BlockEntityStateManager.unregisterBlockEntity(blockEntity)
		assertEquals(helper, null, BlockEntityStateManager.getContainer(blockEntity))
		BlockEntityStateManager.clear()
		helper.succeed()
	}

	@GameTest(template = EMPTY)
	fun testClearRemovesAllTrackedEntities(helper: GameTestHelper)
	{
		BlockEntityStateManager.clear()
		val first = ChestBlockEntity(BlockPos(4, 2, 3), Blocks.CHEST.defaultBlockState())
		val second = ChestBlockEntity(BlockPos(5, 2, 3), Blocks.CHEST.defaultBlockState())

		BlockEntityStateManager.registerBlockEntity(first)
		BlockEntityStateManager.registerBlockEntity(second)
		BlockEntityStateManager.clear()

		assertEquals(helper, null, BlockEntityStateManager.getContainer(first))
		assertEquals(helper, null, BlockEntityStateManager.getContainer(second))
		helper.succeed()
	}
}

