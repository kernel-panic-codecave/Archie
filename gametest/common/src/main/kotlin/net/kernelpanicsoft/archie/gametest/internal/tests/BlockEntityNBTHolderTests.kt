package net.kernelpanicsoft.archie.gametest.internal.tests

import dev.architectury.fluid.FluidStack
import net.kernelpanicsoft.archie.gametest.assertEquals
import net.kernelpanicsoft.archie.gametest.assertTrue
import net.kernelpanicsoft.archie.gametest.internal.EMPTY
import net.kernelpanicsoft.archie.serialization.NBTHolder
import net.kernelpanicsoft.archie.serialization.Sync
import net.kernelpanicsoft.archie.serialization.listField
import net.kernelpanicsoft.archie.serialization.mapField
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.material.Fluids

/**
 * GameTest coverage for [NBTHolder]: default values, save/load round-tripping for scalar,
 * list, map, item, fluid, and energy delegated fields, and that only [Sync]-annotated fields
 * appear in the sync tag.
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

	/** [NBTHolder] with one of each resource-storage field kind, used only by the test below. */
	private class ResourceFixture : NBTHolder by NBTHolder.create()
	{
		val items by itemField(1)
		val tank by fluidField(FluidStack.bucketAmount() * 2)
		val energy by energyField(1_000)
	}

	/** [NBTHolder] with one of each resource-storage *collection* field kind, used only by the test below. */
	private class ResourceCollectionFixture : NBTHolder by NBTHolder.create()
	{
		val itemMap by itemMapField(1)
		val itemList by itemListField(1)
	}

	/** A nested [NBTHolder] type, used only by [OtherNestedFixture] and the nesting test below. */
	private class NestedFixture : NBTHolder by NBTHolder.create()
	{
		var value by intField { 0 }
	}

	/** A second, differently-shaped nested [NBTHolder] type, to exercise heterogeneous nested collections. */
	private class OtherNestedFixture : NBTHolder by NBTHolder.create()
	{
		var label by stringField { "" }
	}

	/** [NBTHolder] with one of each nested-holder field kind, used only by the test below. */
	private class NestingFixture : NBTHolder by NBTHolder.create()
	{
		val single by nestedField { NestedFixture() }
		val list by nestedListField { tag -> if ("label" in tag) OtherNestedFixture() else NestedFixture() }
		val map by nestedMapField { tag -> if ("label" in tag) OtherNestedFixture() else NestedFixture() }
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

	@GameTest(template = EMPTY)
	fun GameTestHelper.testItemFluidAndEnergyFieldsPersistMutations()
	{
		val holder = ResourceFixture()
		holder.items[0].set(ItemStack(Items.DIAMOND, 5))
		holder.tank[0].set(FluidStack.create(Fluids.WATER, FluidStack.bucketAmount()))
		holder.energy.insert(400, false)

		val tag = CompoundTag()
		holder.saveToTag(tag)

		val loaded = ResourceFixture()
		loaded.loadFromTag(tag)

		assertEquals(ItemStack(Items.DIAMOND, 5).item, loaded.items[0].getItem().item)
		assertEquals(5, loaded.items[0].getItem().count)
		assertEquals(FluidStack.bucketAmount(), loaded.tank[0].getFluid().amount)
		assertTrue(loaded.tank[0].getFluid().fluid == Fluids.WATER) {
			"Expected loaded tank to still hold water"
		}
		assertEquals(400L, loaded.energy.getStoredAmount())
		assertEquals(1_000L, loaded.energy.getCapacity())
		succeed()
	}

	@GameTest(template = EMPTY)
	fun GameTestHelper.testItemMapAndListFieldsPersistMutations()
	{
		val holder = ResourceCollectionFixture()
		holder.itemMap.getOrPut("a")[0].set(ItemStack(Items.DIAMOND, 3))
		holder.itemList.add()[0].set(ItemStack(Items.EMERALD, 2))

		val tag = CompoundTag()
		holder.saveToTag(tag)

		val loaded = ResourceCollectionFixture()
		loaded.loadFromTag(tag)

		assertEquals(Items.DIAMOND, loaded.itemMap["a"]!![0].getItem().item)
		assertEquals(3, loaded.itemMap["a"]!![0].getItem().count)
		assertEquals(1, loaded.itemList.size)
		assertEquals(Items.EMERALD, loaded.itemList[0][0].getItem().item)
		assertEquals(2, loaded.itemList[0][0].getItem().count)
		succeed()
	}

	@GameTest(template = EMPTY)
	fun GameTestHelper.testNestedHolderFieldsPersistHeterogeneousMutations()
	{
		val holder = NestingFixture()
		// getOrSet, not `value?.value =` - a nested single field starts empty (the factory is for
		// *loading*, and picks the concrete type out of the saved tag), so the safe-call form this
		// used to take silently did nothing and the assertion below read the default back.
		holder.single.getOrSet { NestedFixture() }.value = 5
		holder.list.add { NestedFixture() }.value = 6
		holder.list.add { OtherNestedFixture() }.label = "seven"
		holder.map.getOrPut("a") { NestedFixture() }.value = 8
		holder.map.getOrPut("b") { OtherNestedFixture() }.label = "nine"

		val tag = CompoundTag()
		holder.saveToTag(tag)

		val loaded = NestingFixture()
		loaded.loadFromTag(tag)

		assertEquals(5, loaded.single.value?.value)
		assertEquals(6, (loaded.list[0] as NestedFixture).value)
		assertEquals("seven", (loaded.list[1] as OtherNestedFixture).label)
		assertEquals(8, (loaded.map["a"] as NestedFixture).value)
		assertEquals("nine", (loaded.map["b"] as OtherNestedFixture).label)
		succeed()
	}

	/**
	 * A nested single field that was never set comes back unset, rather than as a
	 * default-constructed instance.
	 *
	 * It used to save as an empty compound, which [NestedNBTHolder.loadFrom] cannot tell from a
	 * holder that genuinely serialized to nothing - so it called the factory with an empty tag. This
	 * fixture's factory ignores that tag and always constructs, which is exactly the case that
	 * resurrects something from nothing; a real one reads a discriminator out of it and either
	 * returns null or, with `parse` rather than `tryParse`, throws.
	 */
	@GameTest(template = EMPTY)
	fun GameTestHelper.testAnUnsetNestedHolderStaysUnsetAcrossARoundTrip()
	{
		val holder = NestingFixture()
		assertTrue(holder.single.value == null) { "Expected a nested single field to start empty" }

		val tag = CompoundTag()
		holder.saveToTag(tag)

		val loaded = NestingFixture()
		loaded.loadFromTag(tag)

		assertTrue(loaded.single.value == null) {
			"Expected an unset nested holder to stay unset, got ${loaded.single.value}"
		}
		succeed()
	}
}
