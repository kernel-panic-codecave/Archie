package net.kernelpanicsoft.archie.serialization

/**
 * Marks an [NBTHolder]-delegated property as one that should be synced from server to client.
 *
 * Checked by [NBTHolderImpl] (via reflection) when a delegate is created: an annotated property
 * is registered with the owning block entity's state container
 * ([net.kernelpanicsoft.archie.gui.blockentity.BlockEntityStateContainer], obtained through
 * [net.kernelpanicsoft.archie.gui.blockentity.BlockEntityStateManager]) so later writes push
 * updates through that container instead of only being picked up via [NBTHolder.getSyncTag].
 */
@Target(AnnotationTarget.PROPERTY)
annotation class Sync
