package net.kernelpanicsoft.archie.gui.blockentity

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import kotlinx.serialization.serializer

/**
 * Provides the current block entity state to composables in the composition tree.
 *
 * Use with [LocalBlockEntityState.current] to access the state, or use the
 * [observeProperty] helper for convenience.
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
inline fun <reified T> observeProperty(
    propertyName: String,
    initialValue: T? = null,
): MutableState<T?> {
    val state = LocalBlockEntityState.current ?: throw RuntimeException("No block entity state available in composition")
    return state.observeProperty<T>(propertyName, serializer(), initialValue)
}
