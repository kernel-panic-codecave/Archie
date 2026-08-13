package net.kernelpanicsoft.archie.gui.item

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import kotlinx.serialization.serializer
import net.kernelpanicsoft.archie.serialization.SerializationManager

/**
 * Provides the current [ComposeItemContainerMenu]'s state to composables in the composition
 * tree - the [ComposeItemState] equivalent of
 * [net.kernelpanicsoft.archie.gui.blockentity.LocalBlockEntityState]. `null` when the current
 * screen's menu isn't item-backed.
 *
 * Use with `LocalItemState.current` to access the state, or use the [observeItemProperty] helper
 * for convenience.
 */
val LocalItemState = compositionLocalOf<ComposeItemState?> { null }

/**
 * Observes a property on the current [ComposeItemContainerMenu] in the current composition
 * context. The [net.kernelpanicsoft.archie.gui.blockentity.observeProperty] equivalent for
 * item-backed menus.
 *
 * Returns a [MutableState] that automatically triggers recomposition when the property changes.
 * Must be called where [LocalItemState] has been provided with a non-null value (i.e. inside an
 * item-backed menu's screen composition) - otherwise it throws.
 *
 * ### Example
 * ```kotlin
 * @Composable
 * fun MyComponent() {
 *     val progressState = observeItemProperty<Int>("progress")
 *     Text("Progress: ${progressState.value}")
 * }
 * ```
 *
 * @param propertyName The name of the property to observe.
 * @param T The expected type of the property.
 * @return A [MutableState] of type T reflecting the property's current value.
 * @throws RuntimeException if no [ComposeItemState] is available in the current composition.
 */
@Composable
inline fun <reified T> observeItemProperty(
	propertyName: String,
	initialValue: T? = null,
): MutableState<T?> {
	val state = LocalItemState.current ?: throw RuntimeException("No item container state available in composition")
	return state.observeProperty<T>(propertyName, SerializationManager.module.serializer(), initialValue)
}
