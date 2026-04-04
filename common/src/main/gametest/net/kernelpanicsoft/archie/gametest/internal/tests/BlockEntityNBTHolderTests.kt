package net.kernelpanicsoft.archie.gametest.internal.tests

import net.kernelpanicsoft.archie.gametest.internal.EMPTY
import net.kernelpanicsoft.archie.serialization.NBTHolder
import net.kernelpanicsoft.archie.serialization.Sync
import net.kernelpanicsoft.archie.serialization.listField
import net.kernelpanicsoft.archie.serialization.mapField
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.nbt.CompoundTag

@Suppress("unused")
class BlockEntityNBTHolderTests
{
	private class HolderFixture : NBTHolder by NBTHolder.create()
	{
		var counter by intField { 1 }
		var label by stringField { "default" }
		val values by listField<Int> { listOf(1, 2) }
		val weights by mapField<Int> { mapOf("a" to 1) }

		@Sync
		var syncedCounter by intField { 7 }
	}

	@GameTest(template = EMPTY)
	fun testFieldDefaultsAndPersistenceRoundTrip(helper: GameTestHelper)
	{
		val holder = HolderFixture()
		assertEquals(helper, 1, holder.counter)
		assertEquals(helper, "default", holder.label)

		holder.counter = 12
		holder.label = "changed"

		val tag = CompoundTag()
		holder.saveToTag(tag)

		val loaded = HolderFixture()
		loaded.loadFromTag(tag)

		assertEquals(helper, 12, loaded.counter)
		assertEquals(helper, "changed", loaded.label)
		helper.succeed()
	}

	@GameTest(template = EMPTY)
	fun testListAndMapDelegatesPersistMutations(helper: GameTestHelper)
	{
		val holder = HolderFixture()
		holder.values.add(3)
		holder.weights["b"] = 2

		val tag = CompoundTag()
		holder.saveToTag(tag)

		val loaded = HolderFixture()
		loaded.loadFromTag(tag)

		assertEquals(helper, listOf(1, 2, 3), loaded.values.toList())
		assertEquals(helper, 2, loaded.weights["b"])
		helper.succeed()
	}

	@GameTest(template = EMPTY)
	fun testSyncTagContainsOnlySyncAnnotatedFields(helper: GameTestHelper)
	{
		val holder = HolderFixture()
		holder.counter = 42
		holder.syncedCounter = 9

		val syncTag = holder.getSyncTag()
		assertTrue(helper, syncTag.contains("synced_counter")) {
			"Expected sync tag to include synced field"
		}
		assertTrue(helper, !syncTag.contains("counter")) {
			"Expected sync tag to exclude non-synced field"
		}
		helper.succeed()
	}

}

