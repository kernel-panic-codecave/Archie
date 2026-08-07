package net.kernelpanicsoft.archie.gui.item

import kotlinx.serialization.KSerializer

/**
 * Implemented by whatever owns an [net.kernelpanicsoft.archie.serialization.NBTHolder.item]-backed
 * holder that wants its `@Sync`-annotated fields to actually push updates somewhere, mirroring
 * what [net.minecraft.world.level.block.entity.BlockEntity] gets automatically via
 * [net.kernelpanicsoft.archie.gui.blockentity.BlockEntityStateManager].
 *
 * [net.kernelpanicsoft.archie.serialization.ItemStackNBTHolderImpl] checks for this on its
 * `thisRef` the same way it checks `thisRef is BlockEntity` for the block-entity-backed
 * implementation - so any `NBTHolder.item(stack)`-delegated property declared directly on a type
 * implementing this interface gets [registerSyncedProperty]/[onSyncedPropertyChanged] calls
 * automatically.
 */
interface SyncedItemHolder
{
	/**
	 * Called once, at property-declaration time, for every `@Sync`-annotated `NBTHolder.item`-
	 * delegated property named [name] - independent of whether its value has ever actually been
	 * written. Needed so a serializer is available to decode an incoming edit even for a property
	 * whose value came from an *existing* stack's already-populated data (where the delegate's own
	 * initial-value write, which [onSyncedPropertyChanged] would otherwise piggyback on, never
	 * runs) - mirrors why [net.kernelpanicsoft.archie.gui.blockentity.BlockEntityStateContainer]
	 * has its own separate `setPropertySerializer` call distinct from `updateProperty`. No-op by
	 * default for implementations that don't need it.
	 */
	fun <T> registerSyncedProperty(name: String, serializer: KSerializer<T>) {}

	/** Called on every write to a `@Sync`-annotated `NBTHolder.item`-delegated property named [name]. */
	fun <T> onSyncedPropertyChanged(name: String, serializer: KSerializer<T>, value: T)
}
