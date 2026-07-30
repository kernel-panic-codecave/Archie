package net.kernelpanicsoft.archie.gametest.internal.tests

import net.kernelpanicsoft.archie.gametest.internal.EMPTY
import net.kernelpanicsoft.archie.serialization.NBTHolder
import net.kernelpanicsoft.archie.serialization.Sync
import net.kernelpanicsoft.archie.serialization.listField
import net.kernelpanicsoft.archie.serialization.mapField
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.nbt.CompoundTag

/**
 * GameTest coverage for [NBTHolder]: default values, save/load round-tripping for scalar,
 * list, and map delegated fields, and that only [Sync]-annotated fields appear in the sync tag.
 */
@Suppress("unused")
class BlockEntityNBTHolderTests
{
	/** Minimal [NBTHolder] with one of each supported field kind, used as a fixture across tests. */
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
	fun GameTestHelper.testFieldDefaultsAndPersistenceRoundTrip()
	{
		val holder = HolderFixture()
		assertEquals(1, holder.counter)
		assertEquals("default", holder.label)

		holder.counter = 12
		holder.label = "changed"

		val tag = CompoundTag()
		holder.saveToTag(tag)

		val loaded = HolderFixture()
		loaded.loadFromTag(tag)

		assertEquals(12, loaded.counter)
		assertEquals("changed", loaded.label)
		succeed()
	}

	@GameTest(template = EMPTY)
	fun GameTestHelper.testListAndMapDelegatesPersistMutations()
	{
		val holder = HolderFixture()
		holder.values.add(3)
		holder.weights["b"] = 2

		val tag = CompoundTag()
		holder.saveToTag(tag)

		val loaded = HolderFixture()
		loaded.loadFromTag(tag)

		assertEquals(listOf(1, 2, 3), loaded.values.toList())
		assertEquals(2, loaded.weights["b"])
		succeed()
	}

	@GameTest(template = EMPTY)
	fun GameTestHelper.testSyncTagContainsOnlySyncAnnotatedFields()
	{
		val holder = HolderFixture()
		holder.counter = 42
		holder.syncedCounter = 9

		val syncTag = holder.getSyncTag()
		assertTrue("synced_counter" in syncTag) {
			"Expected sync tag to include synced field"
		}
		assertTrue("counter" !in syncTag) {
			"Expected sync tag to exclude non-synced field"
		}
		succeed()
	}

}
