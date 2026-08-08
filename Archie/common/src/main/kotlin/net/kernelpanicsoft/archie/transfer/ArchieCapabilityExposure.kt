package net.kernelpanicsoft.archie.transfer

import dev.architectury.registry.registries.RegistrySupplier
import earth.terrarium.common_storage_lib.context.ItemContext
import earth.terrarium.common_storage_lib.energy.EnergyApi
import earth.terrarium.common_storage_lib.fluid.FluidApi
import earth.terrarium.common_storage_lib.item.ItemApi
import earth.terrarium.common_storage_lib.lookup.BlockLookup
import earth.terrarium.common_storage_lib.lookup.ItemLookup
import net.minecraft.core.Direction
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType

/**
 * Exposes [ArchieItemStorage]/[ArchieFluidStorage]/[ArchieEnergyStorage] to third-party mods'
 * pipes/hoppers/etc, by registering against Common Storage Lib's [ItemApi]/[FluidApi]/[EnergyApi]
 * `BLOCK` lookups - which are, unlike a lookup you'd build yourself via [BlockLookup.create], the
 * real, already-canonical singletons Common Storage Lib itself wires straight through to each
 * platform's native capability system (Fabric Transfer API's `ItemStorage.SIDED`/`FluidStorage.SIDED`,
 * NeoForge's `Capabilities.ItemHandler.BLOCK`/`Capabilities.FluidHandler.BLOCK`). Registering here
 * makes a block entity's storage visible to *any* mod querying those native systems directly - no
 * dependency on Common Storage Lib (or Archie) required on the consuming side.
 *
 * Deliberately explicit opt-in, not wired into [net.kernelpanicsoft.archie.serialization.NBTHolder.itemField]/
 * `fluidField`/`energyField`: registration must happen exactly once per [BlockEntityType], while
 * those field delegates run once per block entity *instance* (inside its constructor) - auto-registering
 * from there would either re-register redundantly per instance or need awkward static bookkeeping.
 * Call these once, at registration time, next to your `DeferredRegister`/`RegistrySupplier` declarations:
 *
 * ```kotlin
 * object BlockEntities : ADeferredRegistryHolder<BlockEntityType<*>>(MyMod.MOD, Registries.BLOCK_ENTITY_TYPE) {
 *     val TANK by register("tank") { BlockEntityType.Builder.of(::TankBlockEntity, MyBlocks.TANK).build(null) }
 * }
 *
 * // In mod init, after BlockEntities.init():
 * BlockEntities.TANK.exposeFluidStorage { tank -> tank.fluid }
 * ```
 */
@Suppress("unused")
object ArchieCapabilityExposure

/** See [ArchieCapabilityExposure]. Exposes this block entity type's [ArchieItemStorage] to [ItemApi.BLOCK]. */
fun <T : BlockEntity> BlockEntityType<T>.exposeItemStorage(selector: (T, Direction?) -> ArchieItemStorage?) {
    exposeToBlockLookup(ItemApi.BLOCK, selector)
}

/** [exposeItemStorage] overload for a selector that doesn't need the query direction. */
fun <T : BlockEntity> BlockEntityType<T>.exposeItemStorage(selector: (T) -> ArchieItemStorage?) {
    exposeItemStorage { be, _ -> selector(be) }
}

/** See [ArchieCapabilityExposure]. Exposes this block entity type's [ArchieFluidStorage] to [FluidApi.BLOCK]. */
fun <T : BlockEntity> BlockEntityType<T>.exposeFluidStorage(selector: (T, Direction?) -> ArchieFluidStorage?) {
    exposeToBlockLookup(FluidApi.BLOCK, selector)
}

/** [exposeFluidStorage] overload for a selector that doesn't need the query direction. */
fun <T : BlockEntity> BlockEntityType<T>.exposeFluidStorage(selector: (T) -> ArchieFluidStorage?) {
    exposeFluidStorage { be, _ -> selector(be) }
}

/** See [ArchieCapabilityExposure]. Exposes this block entity type's [ArchieEnergyStorage] to [EnergyApi.BLOCK]. */
fun <T : BlockEntity> BlockEntityType<T>.exposeEnergyStorage(selector: (T, Direction?) -> ArchieEnergyStorage?) {
    exposeToBlockLookup(EnergyApi.BLOCK, selector)
}

/** [exposeEnergyStorage] overload for a selector that doesn't need the query direction. */
fun <T : BlockEntity> BlockEntityType<T>.exposeEnergyStorage(selector: (T) -> ArchieEnergyStorage?) {
    exposeEnergyStorage { be, _ -> selector(be) }
}

/**
 * Shared implementation: [BlockLookup] only supports registering by [BlockEntityType] via the
 * [BlockLookup.BlockRegistrar] callback handed to [BlockLookup.onRegister] - `registerSelf` (the
 * more direct-looking method) only accepts a `Block`-keyed getter, not a block-entity-keyed one.
 */
@Suppress("UNCHECKED_CAST")
private fun <T : BlockEntity, S> BlockEntityType<T>.exposeToBlockLookup(
    lookup: BlockLookup<S, Direction?>,
    selector: (T, Direction?) -> S?,
) {
    lookup.onRegister { registrar ->
        registrar.registerBlockEntities(
            BlockLookup.BlockEntityGetter { blockEntity, direction -> selector(blockEntity as T, direction) },
            this,
        )
    }
}

// ── RegistrySupplier convenience overloads ──────────────────────────────────────────────────
// So these can be chained right where the type is declared, without waiting for a separate
// registration-time call site. Architectury's RegistrySupplier.listen(...) already guarantees the
// callback runs once the entry is actually registered.

fun <T : BlockEntity> RegistrySupplier<BlockEntityType<T>>.exposeItemStorage(selector: (T, Direction?) -> ArchieItemStorage?) =
    listen { it.exposeItemStorage(selector) }

fun <T : BlockEntity> RegistrySupplier<BlockEntityType<T>>.exposeItemStorage(selector: (T) -> ArchieItemStorage?) =
    listen { it.exposeItemStorage(selector) }

fun <T : BlockEntity> RegistrySupplier<BlockEntityType<T>>.exposeFluidStorage(selector: (T, Direction?) -> ArchieFluidStorage?) =
    listen { it.exposeFluidStorage(selector) }

fun <T : BlockEntity> RegistrySupplier<BlockEntityType<T>>.exposeFluidStorage(selector: (T) -> ArchieFluidStorage?) =
    listen { it.exposeFluidStorage(selector) }

fun <T : BlockEntity> RegistrySupplier<BlockEntityType<T>>.exposeEnergyStorage(selector: (T, Direction?) -> ArchieEnergyStorage?) =
    listen { it.exposeEnergyStorage(selector) }

fun <T : BlockEntity> RegistrySupplier<BlockEntityType<T>>.exposeEnergyStorage(selector: (T) -> ArchieEnergyStorage?) =
    listen { it.exposeEnergyStorage(selector) }

// ── Item-in-item exposure (stretch) ─────────────────────────────────────────────────────────
// Unlike the BLOCK lookups above, ItemApi.ITEM has no equivalent native-platform bridge - it's
// registered under Common Storage Lib's own mod id, so this is only visible to other mods that
// also depend on Common Storage Lib and query this exact same field. Still useful: it makes a
// backpack/bag's own storage pipe-accessible (by other CSL-aware mods) even while its GUI is closed.

/** Exposes this item's [ArchieItemStorage] (e.g. a bag/backpack's contents) to [ItemApi.ITEM]. */
fun Item.exposeItemStorage(selector: (ItemStack, ItemContext) -> ArchieItemStorage?) {
    ItemApi.ITEM.registerSelf(ItemLookup.ItemGetter { stack, context -> selector(stack, context) }, this)
}
