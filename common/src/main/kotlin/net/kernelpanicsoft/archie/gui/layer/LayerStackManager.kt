package net.kernelpanicsoft.archie.gui.layer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateListOf
import net.kernelpanicsoft.archie.gui.layout.Box
import net.kernelpanicsoft.archie.gui.layout.Alignment
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.modifiers.fillMaxSize
import net.kernelpanicsoft.archie.gui.modifiers.input.PointerEventType
import net.kernelpanicsoft.archie.gui.modifiers.input.onPointerEvent
import net.kernelpanicsoft.archie.gui.nodes.AUINode
import java.util.*

/**
 * Provides the nearest [LayerStackManager] to composables inside a [net.kernelpanicsoft.archie.gui.ComposeScreen]
 * or [net.kernelpanicsoft.archie.gui.ComposeContainerScreen].
 *
 * Access via `LocalLayerManager.current` to push new overlay layers.
 */
val LocalLayerManager = compositionLocalOf<LayerStackManager> {
    error("No LayerManager provided. Are you inside a ComposeScreen?")
}

/**
 * Receiver scope for modal layer content, exposing a way to close the modal.
 */
interface ModalScope {
    /**
     * Dismisses (removes) the modal layer that owns this scope.
     */
    fun dismiss()
}

/**
 * Manages an ordered stack of [Layer]s for a single screen.
 *
 * The stack determines the rendering order (bottom to top) and input-dispatch priority
 * (top layer receives events first). Overlays such as dialogs, dropdowns, and tooltips
 * are each their own layer on top of the base screen content.
 *
 * Obtain an instance via the [LocalLayerManager] composition local.
 *
 * @param parentComposition The [CompositionContext] from the host screen, required
 *   when creating child [Composition]s for each layer.
 */
class LayerStackManager(private val parentComposition: CompositionContext) {

    /** The ordered list of active layers. Layers are rendered bottom-to-top. */
    val layers = mutableStateListOf<Layer>()

    /**
     * Pushes a new generic layer onto the stack.
     *
     * The content lambda receives a `dismiss` function it can call to remove itself
     * from the stack. This overload is suitable for persistent overlays and custom
     * layer types.
     *
     * @param layerContent The composable content for the new layer.
     * @return A dismiss handle; call it to imperatively remove the layer.
     */
    fun push(layerContent: @Composable (dismiss: () -> Unit) -> Unit): () -> Unit {
        val layerId = UUID.randomUUID()
        val layer = Layer(id = layerId, parentComposition = parentComposition) {
            layerContent { popById(layerId) }
        }
        layers.add(layer)
        return { popById(layerId) }
    }

    /**
     * Pushes a new modal layer onto the stack.
     *
     * Modals are opinionated, input-blocking overlays ideal for dialogs and confirmation
     * prompts. A click outside the modal content area triggers [onDismissRequest] and,
     * when [dismissOnClickOutside] is `true`, automatically removes the layer.
     *
     * @param alignment            Alignment of the modal within the full screen. Default [Alignment.Center].
     * @param dismissOnClickOutside Whether clicking outside the modal content closes it.
     * @param onDismissRequest     Optional callback invoked when the modal is dismissed.
     * @param content              The modal UI, a composable lambda with [ModalScope] receiver.
     */
    fun modal(
        alignment: Alignment = Alignment.Center,
        dismissOnClickOutside: Boolean = true,
        onDismissRequest: () -> Unit = {},
        content: @Composable ModalScope.() -> Unit,
    ) {
        push { dismiss ->
            val scope = object : ModalScope {
                override fun dismiss() { onDismissRequest(); dismiss() }
            }
            ModalLayout(
                alignment = alignment,
                dismissOnClickOutside = dismissOnClickOutside,
                onDismissRequest = { scope.dismiss() },
                content = { scope.content() },
            )
        }
    }

    /**
     * Removes and disposes the topmost layer.
     */
    fun pop() = layers.removeLastOrNull()?.dispose()

    /**
     * Removes and disposes the layer identified by [id].
     *
     * Does nothing if no layer with that id exists.
     *
     * @param id The [UUID] of the layer to remove.
     */
    fun popById(id: UUID) {
        val layer = layers.find { it.id == id } ?: return
        layer.dispose()
        layers.remove(layer)
    }

    /**
     * The topmost (most recently pushed) layer, which receives input events first.
     * `null` if the stack is empty.
     */
    val top: Layer? get() = layers.lastOrNull()

    @Composable
    private fun ModalLayout(
        alignment: Alignment,
        onDismissRequest: () -> Unit,
        dismissOnClickOutside: Boolean,
        content: @Composable () -> Unit,
    ) {
        var rootModifier = Modifier.fillMaxSize()
        if (dismissOnClickOutside) {
            rootModifier = rootModifier.onPointerEvent<AUINode>(PointerEventType.PRESS) { _, event ->
                onDismissRequest()
                event.consume()
            }
        }
        Box(modifier = rootModifier, contentAlignment = alignment) {
            Box(modifier = Modifier.onPointerEvent<AUINode>(PointerEventType.PRESS) { _, event -> event.consume() }) {
                content()
            }
        }
    }
}
