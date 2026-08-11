package net.kernelpanicsoft.archie.test.gametest

import net.kernelpanicsoft.archie.test.TileRegistry
import net.kernelpanicsoft.archie.transfer.exposeItemStorage
import net.minecraft.core.Direction

/**
 * Exposes [TileRegistry.TestTile]'s existing `items` [net.kernelpanicsoft.archie.transfer.ArchieItemStorage]
 * for [CapabilityLookupTests] to assert against.
 *
 * This can't live inside the `@GameTest` methods themselves: Common Storage Lib's
 * `BlockLookup.onRegister` (what `exposeItemStorage` calls) is a one-shot listener CSL invokes
 * during its own platform registration event (Fabric's `ItemApiLookup`/NeoForge's
 * `RegisterCapabilitiesEvent`) - which has already fired long before a GameTest server finishes
 * booting and starts ticking tests. [init] is called from [net.kernelpanicsoft.archie.test.ArchieTest.init],
 * after `TileRegistry.init()` (so [TileRegistry.TestTile] actually exists) but still during normal
 * mod initialization - the same point real mod code would call `exposeItemStorage` from.
 */
internal object CapabilityLookupTestFixtures
{
	fun init()
	{
		TileRegistry.TestTile.exposeItemStorage { tile, direction ->
			if (direction == Direction.DOWN) null else tile.items
		}
	}
}
