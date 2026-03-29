package net.kernelpanicsoft.archie.gui.blockentity

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import net.minecraft.core.BlockPos

/**
 * Client-side state holder for a block entity's synchronized properties.
 *
 * Each property is wrapped in a Compose [MutableState], allowing composables to react
 * to changes automatically through recomposition.
 *
 * @param pos The block position of the block entity.
 */
class ComposeBlockEntityState(
    val pos: BlockPos,
) {
    /** Map of property names to their Compose state values */
    val propertyStates = mutableMapOf<String, MutableState<Any?>>()

    /**
     * Gets or creates a Compose state for a property.
     *
     * @param propertyName The name of the property.
     * @param initialValue The initial value (optional, defaults to null).
     * @return A [MutableState] that can be observed in composables.
     */
    fun observeProperty(
        propertyName: String,
        initialValue: Any? = null,
    ): MutableState<Any?> {
        return propertyStates.computeIfAbsent(propertyName) {
            mutableStateOf(initialValue)
        }
    }

    /**
     * Gets or creates a Compose state for a property with a specific type.
     *
     * @param propertyName The name of the property.
     * @param initialValue The initial value (optional, defaults to null).
     * @param T The expected type of the property.
     * @return A [MutableState] of type T that can be observed in composables.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any?> observePropertyTyped(
        propertyName: String,
        initialValue: T = null as T,
    ): MutableState<T> {
        return propertyStates.computeIfAbsent(propertyName) {
            mutableStateOf(initialValue as Any?)
        } as MutableState<T>
    }

    /**
     * Updates a property value from a network packet.
     *
     * If the property doesn't exist yet, it will be created.
     *
     * @param propertyName The name of the property.
     * @param value The new serialized value from the network packet.
     */
    fun updateProperty(propertyName: String, value: BlockEntityStatePacket.SerializedValue) {
        val deserializedValue = value.deserialize()
        val state = propertyStates.computeIfAbsent(propertyName) {
            mutableStateOf(deserializedValue)
        }
        state.value = deserializedValue
    }

    /**
     * Gets the current value of a property.
     *
     * @param propertyName The name of the property.
     * @return The property value, or null if not tracked.
     */
    fun getProperty(propertyName: String): Any? {
        return propertyStates[propertyName]?.value
    }

    /**
     * Gets the current value of a property with type casting.
     *
     * @param propertyName The name of the property.
     * @param T The expected type.
     * @return The property value cast to T, or null if not found/wrong type.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getPropertyTyped(propertyName: String): T? {
        return propertyStates[propertyName]?.value as? T
    }

    /**
     * Clears all tracked properties.
     *
     * Useful when the block entity is unloaded or the state is no longer needed.
     */
    fun clear() {
        propertyStates.clear()
    }

    /**
     * Gets all currently tracked properties.
     *
     * @return A map of property names to their current values.
     */
    fun getAllProperties(): Map<String, Any?> {
        return propertyStates.mapValues { (_, state) -> state.value }
    }
}

/**
 * Extension function to deserialize network values back to Kotlin objects.
 */
internal fun BlockEntityStatePacket.SerializedValue.deserialize(): Any? = when (this) {
    is BlockEntityStatePacket.SerializedValue.IntValue -> this.value
    is BlockEntityStatePacket.SerializedValue.StringValue -> this.value
    is BlockEntityStatePacket.SerializedValue.BooleanValue -> this.value
    is BlockEntityStatePacket.SerializedValue.FloatValue -> this.value
    is BlockEntityStatePacket.SerializedValue.DoubleValue -> this.value
    is BlockEntityStatePacket.SerializedValue.LongValue -> this.value
    is BlockEntityStatePacket.SerializedValue.ByteValue -> this.value
    is BlockEntityStatePacket.SerializedValue.ListValue -> this.values.map { it.deserialize() }
    is BlockEntityStatePacket.SerializedValue.MapValue -> this.values.mapValues { it.value.deserialize() }
    is BlockEntityStatePacket.SerializedValue.NullValue -> null
}
