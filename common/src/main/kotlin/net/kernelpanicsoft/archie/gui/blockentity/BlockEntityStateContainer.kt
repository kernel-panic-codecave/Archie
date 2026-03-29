package net.kernelpanicsoft.archie.gui.blockentity

import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.entity.BlockEntity

/**
 * Wraps a block entity and tracks which properties have changed since the last sync.
 *
 * This class provides dirty tracking for efficient server-to-client synchronization.
 * Only modified properties are included in generated state packets.
 *
 * @param blockEntity The block entity to monitor for changes.
 */
class BlockEntityStateContainer(
    val blockEntity: BlockEntity,
) {
    /** The block position of the wrapped block entity. */
    val pos: BlockPos get() = blockEntity.blockPos

    /** Map of property names to their current values. */
    private val propertyValues = mutableMapOf<String, Any?>()

    /** Set of property names that have changed since the last sync. */
    private val dirtyProperties = mutableSetOf<String>()

    /** Server tick when this container was last synced. */
    var lastSyncTick: Long = 0

    /** Whether any properties have changed. */
    val isDirty: Boolean get() = dirtyProperties.isNotEmpty()

    /**
     * Records a property value and marks it dirty if it changed.
     *
     * @param propertyName The name of the property.
     * @param value The new value.
     * @return True if the value changed, false if it's the same as before.
     */
    fun updateProperty(propertyName: String, value: Any?): Boolean {
        val oldValue = propertyValues[propertyName]
        val changed = oldValue != value
        if (changed) {
            propertyValues[propertyName] = value
            dirtyProperties.add(propertyName)
        }
        return changed
    }

    /**
     * Gets the current value of a property.
     *
     * @param propertyName The name of the property.
     * @return The property value, or null if not tracked.
     */
    fun getProperty(propertyName: String): Any? = propertyValues[propertyName]

    /**
     * Gets the current value of a property with a type cast.
     *
     * @param propertyName The name of the property.
     * @param T The expected type of the property.
     * @return The property value cast to type T, or null if not found/wrong type.
     */
    fun <T : Any> getProperty(propertyName: String, type: Class<T>): T? =
        propertyValues[propertyName] as? T

    /**
     * Generates a state packet containing all dirty properties.
     *
     * @param serverTick The current server tick.
     * @return A packet with all dirty property updates, or null if no changes.
     */
    fun generatePacket(serverTick: Long): BlockEntityStatePacket? {
        if (dirtyProperties.isEmpty()) return null

        val updates = dirtyProperties.associate { propertyName ->
            propertyName to propertyValues[propertyName].toSerializedValue()
        }

        return BlockEntityStatePacket(
            pos = pos,
            updates = updates,
            timestamp = serverTick,
        )
    }

    /**
     * Clears the dirty flag, marking all properties as synced.
     *
     * Should be called after successfully sending a state packet to clients.
     *
     * @param serverTick The server tick when the sync completed.
     */
    fun clearDirty(serverTick: Long) {
        dirtyProperties.clear()
        lastSyncTick = serverTick
    }

    /**
     * Gets all dirty property names.
     *
     * Useful for debugging or logging.
     *
     * @return An immutable set of property names that have changed.
     */
    fun getDirtyProperties(): Set<String> = dirtyProperties.toSet()

    /**
     * Gets a snapshot of all tracked properties.
     *
     * @return An immutable map of all property names and values.
     */
    fun getAllProperties(): Map<String, Any?> = propertyValues.toMap()

    /**
     * Resets all tracking, clearing dirty flags and property values.
     *
     * Useful when the block entity is unloaded or the container is no longer needed.
     */
    fun reset() {
        propertyValues.clear()
        dirtyProperties.clear()
        lastSyncTick = 0
    }
}
