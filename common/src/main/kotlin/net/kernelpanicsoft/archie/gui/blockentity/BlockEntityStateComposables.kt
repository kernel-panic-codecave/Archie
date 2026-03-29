package net.kernelpanicsoft.archie.gui.blockentity

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import net.minecraft.core.BlockPos

/**
 * Provides the current block entity state to composables in the composition tree.
 *
 * Use with [LocalBlockEntityState.current] to access the state, or use the
 * [observeBlockEntityProperty] helper for convenience.
 */
val LocalBlockEntityState = compositionLocalOf<ComposeBlockEntityState?> { null }

/**
 * Observes a property on the block entity in the current composition context.
 *
 * Returns a [MutableState] that automatically triggers recomposition when the property changes.
 * If no block entity state is available in the composition, returns null.
 *
 * ### Example
 * ```kotlin
 * @Composable
 * fun MyComponent() {
 *     val powerState = observeBlockEntityProperty<Int>("power")
 *     if (powerState != null) {
 *         Text("Power: ${powerState.value}")
 *     }
 * }
 * ```
 *
 * @param propertyName The name of the property to observe.
 * @param T The expected type of the property.
 * @return A [MutableState] of type T, or null if no block entity state is available.
 */
@Composable
inline fun <reified T : Any?> observeBlockEntityProperty(
    propertyName: String,
    initialValue: T = null as T,
): MutableState<T>? {
    val state = LocalBlockEntityState.current ?: return null
    @Suppress("UNCHECKED_CAST")
    return state.propertyStates.computeIfAbsent(propertyName) {
        mutableStateOf(initialValue as Any?)
    } as MutableState<T>
}

/**
 * Observes a property on the block entity as a non-state value.
 *
 * Returns the current value of the property, automatically triggering recomposition
 * when it changes. This is a convenience function that unwraps the state value.
 *
 * ### Example
 * ```kotlin
 * @Composable
 * fun MyComponent() {
 *     val power: Int? = observeBlockEntityPropertyValue("power")
 *     Text("Power: $power")
 * }
 * ```
 *
 * @param propertyName The name of the property to observe.
 * @param T The expected type of the property.
 * @return The current value of the property, or null if not available.
 */
@Composable
inline fun <reified T : Any?> observeBlockEntityPropertyValue(
    propertyName: String,
    initialValue: T = null as T,
): T? {
    return observeBlockEntityProperty<T>(propertyName, initialValue)?.value
}

/**
 * Gets the current block entity state from the composition context.
 *
 * Returns null if no block entity state is available. Useful when you need to access
 * the state directly rather than observing a specific property.
 *
 * @return The current [ComposeBlockEntityState], or null if not available.
 */
@Composable
fun currentBlockEntityState(): ComposeBlockEntityState? = LocalBlockEntityState.current

/**
 * DSL builder for observing multiple block entity properties at once.
 *
 * ### Example
 * ```kotlin
 * @Composable
 * fun MyComponent() {
 *     observeBlockEntityProperties {
 *         val power = observeInt("power")
 *         val name = observeString("name")
 *         val items = observeAny("items")
 *
 *         Text("$name - Power: $power (${(items as? List<*>)?.size} items)")
 *     }
 * }
 * ```
 */
@Composable
fun observeBlockEntityProperties(
    block: @Composable BlockEntityPropertyObserverScope.() -> Unit,
) {
    val state = LocalBlockEntityState.current
    if (state != null) {
        BlockEntityPropertyObserverScope(state).block()
    }
}

/**
 * Receiver scope for [observeBlockEntityProperties] DSL.
 */
class BlockEntityPropertyObserverScope(private val state: ComposeBlockEntityState) {
    /**
     * Observes a property within the DSL block.
     *
     * @param propertyName The name of the property.
     * @param initialValue The initial value.
     * @return The current value of the property.
     */
    @Composable
    fun observeInt(propertyName: String, initialValue: Int = 0): Int {
        return (state.observeProperty(propertyName, initialValue).value as? Int) ?: initialValue
    }

    @Composable
    fun observeString(propertyName: String, initialValue: String = ""): String {
        return (state.observeProperty(propertyName, initialValue).value as? String) ?: initialValue
    }

    @Composable
    fun observeBoolean(propertyName: String, initialValue: Boolean = false): Boolean {
        return (state.observeProperty(propertyName, initialValue).value as? Boolean) ?: initialValue
    }

    @Composable
    fun observeDouble(propertyName: String, initialValue: Double = 0.0): Double {
        return (state.observeProperty(propertyName, initialValue).value as? Double) ?: initialValue
    }

    @Composable
    fun observeLong(propertyName: String, initialValue: Long = 0L): Long {
        return (state.observeProperty(propertyName, initialValue).value as? Long) ?: initialValue
    }

    @Composable
    fun observeFloat(propertyName: String, initialValue: Float = 0f): Float {
        return (state.observeProperty(propertyName, initialValue).value as? Float) ?: initialValue
    }

    @Composable
    fun observeAny(propertyName: String, initialValue: Any? = null): Any? {
        return state.observeProperty(propertyName, initialValue).value
    }
}
