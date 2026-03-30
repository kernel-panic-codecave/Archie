package net.kernelpanicsoft.archie.gui.blockentity

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import net.kernelpanicsoft.archie.networking.ArchieNetworkChannel
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
    val propertySerializers = mutableMapOf<String, KSerializer<out Any>>()

    /**
     * Gets or creates a Compose state for a property with a specific type.
     *
     * @param propertyName The name of the property.
     * @param initialValue The initial value (optional, defaults to null).
     * @param T The expected type of the property.
     * @return A [MutableState] of type T that can be observed in composables.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> observeProperty(
        propertyName: String,
        serializer: KSerializer<T>,
        initialValue: T? = null,
    ): MutableState<T?> {
        propertySerializers[propertyName] = serializer as KSerializer<out Any>
        return propertyStates.computeIfAbsent(propertyName) {
            PropertyState(this,propertyName, mutableStateOf(initialValue as Any?))
        } as MutableState<T?>
    }

    class PropertyState<T>(private val state: ComposeBlockEntityState, private val propertyName: String, internal val mutableState: MutableState<T>) : MutableState<T> by mutableState
    {
        override var value: T
            get() = mutableState.value
            set(value)
            {
                mutableState.value = value
                state.sendUpdatedProperty(propertyName, value)
            }
    }

    /**
     * Updates a property value from a network packet.
     *
     * If the property doesn't exist yet, it will be created.
     *
     * @param propertyName The name of the property.
     * @param value The new serialized value from the network packet.
     */
    @OptIn(ExperimentalSerializationApi::class)
    fun updateProperty(propertyName: String, value: BlockEntityStatePacket.SerializedValue) {
        val deserializedValue = value.deserialize(propertySerializers[propertyName])
        val state = propertyStates.computeIfAbsent(propertyName) {
            mutableStateOf(deserializedValue)
        }
        state.value = deserializedValue
    }

    /**
     * Updates a property value and sends the change to the server.
     *
     * This method should be called when a client-side interaction changes a property.
     *
     * @param propertyName The name of the property.
     * @param value The new value.
     */
    @OptIn(ExperimentalSerializationApi::class)
    fun <T> sendUpdatedProperty(propertyName: String, value: T) {
        val serializer = propertySerializers[propertyName] ?: run {
            println("No serializer found for property $propertyName. Cannot send update to server.")
            return
        }

        val serializedValue = value.toSerializedValue(serializer as KSerializer<T>)
        val packet = BlockEntityUpdatePacket.singleUpdate(pos, propertyName, serializedValue)
        ArchieNetworkChannel.toServer(packet)
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
