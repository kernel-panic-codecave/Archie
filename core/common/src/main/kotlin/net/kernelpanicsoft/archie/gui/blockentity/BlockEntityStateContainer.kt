package net.kernelpanicsoft.archie.gui.blockentity

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializerOrNull
import net.kernelpanicsoft.archie.config.toSnakeCase
import net.kernelpanicsoft.archie.serialization.SerializationManager
import net.kernelpanicsoft.archie.serialization.Sync
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.entity.BlockEntity
import kotlin.reflect.KClass
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.safeCast
import kotlin.reflect.jvm.isAccessible

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

    /** Serializers used to encode dirty properties into a [BlockEntityStatePacket], keyed by property name. */
    internal val propertySerializers = mutableMapOf<String, KSerializer<out Any>>()

    @Suppress("UNCHECKED_CAST")
    private fun <T> anySerializer(serializer: KSerializer<T>): KSerializer<out Any> = serializer as KSerializer<out Any>

    /**
     * The serializer registered for [propertyName], or `null` when it has none.
     *
     * Not every `@Sync` property has one: the `init` scan resolves what it can through
     * `serializerOrNull`, and a property whose type it cannot describe - a nested holder map, say -
     * simply gets no entry. Such a property can be tracked and read locally but never sent, so this
     * is nullable rather than a cast that would blow up the sync tick for every block entity at once.
     */
    @Suppress("UNCHECKED_CAST")
    private fun packetSerializer(propertyName: String): KSerializer<Any>? =
        propertySerializers[propertyName] as? KSerializer<Any>

    init {
        blockEntity::class.memberProperties.forEach { property ->
            if (property.hasAnnotation<Sync>())
            {
                property.isAccessible = true
                SerializationManager.module.serializerOrNull(property.returnType)?.let { serializer ->
                    propertySerializers[property.name.toSnakeCase()] = anySerializer(serializer)
                }
            }
        }
    }

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
    fun <T> updateProperty(propertyName: String, value: T?): Boolean {

        val oldValue = propertyValues[propertyName]
        // Identity counts as a change. A storage-backed property hands back the *same* mutable
        // object every time it is touched - `fluidField` passes its own live storage - so comparing
        // it against what is already recorded compares an object with itself and can never report a
        // change, leaving the property permanently clean and its viewers permanently stale.
        // Equality cannot distinguish "mutated in place" from "re-set to an equal value", and
        // callers only reach here on an actual update, so a redundant packet is the right trade
        // against a silent freeze.
        val changed = if (oldValue === value) oldValue != null else oldValue != value
        if (changed) {
            propertyValues[propertyName] = value
            dirtyProperties.add(propertyName)
        }
        return changed
    }

    /**
     * Registers a serializer for [propertyName] if one isn't already known.
     *
     * Only needed for properties whose type can't be resolved automatically via
     * [kotlinx.serialization.serializerOrNull] (see the `init` block).
     *
     * @param propertyName The name of the property.
     * @param serializer The serializer to use when encoding this property.
     */
    fun <T> setPropertySerializer(propertyName: String, serializer: KSerializer<T>) {
        propertySerializers.putIfAbsent(propertyName, anySerializer(serializer))
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
    fun <T : Any> getProperty(propertyName: String, type: KClass<T>): T? =
        type.safeCast(propertyValues[propertyName])

    /**
     * Generates a state packet containing all dirty properties.
     *
     * @param serverTick The current server tick.
     * @return A packet with all dirty property updates, or null if no changes.
     */
    fun generatePacket(serverTick: Long): BlockEntityStatePacket? {
        if (dirtyProperties.isEmpty()) return null

        // A property with no serializer is skipped rather than fatal - see packetSerializer.
        val updates = dirtyProperties.mapNotNull { propertyName ->
            val serializer = packetSerializer(propertyName) ?: return@mapNotNull null
            propertyName to propertyValues[propertyName].toSerializedValue(serializer)
        }.toMap()
        if (updates.isEmpty()) return null

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
     * Re-reads every `@Sync` property straight off the block entity into [propertyValues].
     *
     * A container only ever learned a value when something *changed* it, so anything that was
     * already true before the container existed - a tank filled last session, a machine loaded from
     * disk - was invisible to it. That is fine while the only consumer is a delta stream, and wrong
     * the moment someone opens a screen expecting to see current state.
     *
     * Reads through the property getter rather than any backing map, so a delegated property (an
     * `itemField`/`fluidField` storage) reports the live object exactly as the block entity's own
     * code would. A getter that throws is skipped rather than propagated: seeding is best-effort,
     * and one awkward property must not stop a screen opening.
     */
    fun captureCurrentValues() {
        blockEntity::class.memberProperties.forEach { property ->
            if (!property.hasAnnotation<Sync>()) return@forEach
            property.isAccessible = true
            runCatching { property.getter.call(blockEntity) }
                .onSuccess { value -> propertyValues[property.name.toSnakeCase()] = value }
        }
    }

    /**
     * Marks every known property dirty, so the next packet carries a full snapshot rather than a delta.
     *
     * For a viewer who has just started tracking: they have missed every change so far, and a delta
     * stream tells them nothing until the next one happens.
     */
    fun markAllDirty() {
        // Only what can actually be sent: marking a property with no serializer would leave it dirty
        // forever, re-examined on every sync tick and never cleared by a successful send.
        dirtyProperties.addAll(propertyValues.keys.filter { propertySerializers.containsKey(it) })
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
