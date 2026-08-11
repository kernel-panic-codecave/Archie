package net.kernelpanicsoft.archie.gui.layer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import net.kernelpanicsoft.archie.gui.animation.AnimationSpec
import net.kernelpanicsoft.archie.gui.animation.Easing
import net.kernelpanicsoft.archie.gui.animation.Easings
import net.kernelpanicsoft.archie.gui.animation.animateFloat
import net.kernelpanicsoft.archie.gui.animation.animateInt
import net.kernelpanicsoft.archie.gui.composables.containers.RootContainer
import net.kernelpanicsoft.archie.gui.composables.modal.AlertDialog
import net.kernelpanicsoft.archie.gui.composables.modal.ChoiceDialog
import net.kernelpanicsoft.archie.gui.composables.modal.ConfirmDialog
import net.kernelpanicsoft.archie.gui.composables.modal.ModalChoice
import net.kernelpanicsoft.archie.gui.composables.modal.PromptDialog
import net.kernelpanicsoft.archie.gui.layout.Box
import net.kernelpanicsoft.archie.gui.layout.Alignment
import net.kernelpanicsoft.archie.gui.layout.IntCoordinates
import net.kernelpanicsoft.archie.gui.layout.Size
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.modifiers.appearance.background
import net.kernelpanicsoft.archie.gui.modifiers.fillMaxSize
import net.kernelpanicsoft.archie.gui.modifiers.input.PointerEventType
import net.kernelpanicsoft.archie.gui.modifiers.input.onPointerEvent
import net.kernelpanicsoft.archie.gui.modifiers.position.offset
import net.kernelpanicsoft.archie.gui.nodes.UINode
import net.minecraft.network.chat.Component
import java.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.kernelpanicsoft.archie.gui.modifiers.position.zIndex
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Provides the nearest [LayerStackManager] to composables inside a [net.kernelpanicsoft.archie.gui.ComposeScreen]
 * or [net.kernelpanicsoft.archie.gui.ComposeContainerScreen].
 *
 * Access via `LocalLayerManager.current` to push new overlay layers.
 */
val LocalLayerManager = compositionLocalOf<LayerStackManager> {
    error("No LayerManager provided. Are you inside a ComposeScreen?")
}

/** The depth index of the currently composed layer (base layer is `0`). */
val LocalLayerDepth = compositionLocalOf { 0 }

/**
 * Receiver scope for modal layer content, exposing a way to close the modal.
 */
interface ModalScope {
    /**
     * Dismisses (removes) the modal layer that owns this scope.
     */
    fun dismiss()
}

/** Transition defaults applied to every modal pushed through [LayerStackManager.modal]. */
data class ModalTransitionSpec(
    val durationMillis: Duration = 180.milliseconds,
    val easing: Easing = Easings.OutCubic,
    val enterOffsetY: Int = 8,
    val maxBackdropAlpha: Int = 132,
)

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
     * Represents the total size of the screen, calculated based on the dimensions of all active layers.
     *
     * This property computes the maximum width and height among all the root container nodes
     * from the layers managed by the containing class. It aggregates these dimensions by traversing
     * the active layers and comparing their widths and heights.
     *
     * If a layer does not have a root container node, it is skipped in the calculation.
     *
     * @return A [Size] object representing the combined width and height required to encapsulate
     * all visible layers.
     */
    val screenSize: Size
        get() = layers.fold(Size(0, 0)) { acc, layer ->
            val node = layer.rootContainerNode ?: return@fold acc
            Size(max(node.width, acc.width), max(node.height, acc.height))
        }

    /**
     * Represents the top-left position of the screen, calculated based on the root container
     * nodes of all active layers within the layer stack.
     *
     * The result aggregates the minimum x and y coordinates across all layers. If no root
     * container nodes are found, the default position is (0, 0).
     *
     * The position is determined by folding over all layers and comparing the x and y positions
     * of their root container nodes, if present. The computation ensures that the resulting
     * coordinates account for the smallest bounds of the visible layers in the stack.
     */
    val screenPos: IntCoordinates
        get() = layers.fold(null) { acc, layer ->
            val node = layer.rootContainerNode ?: return@fold acc
            IntCoordinates(min(acc?.x ?: node.x,  node.x), min(acc?.y ?: node.y, node.y))
        } ?: IntCoordinates(0, 0)

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
        val layerDepth = layers.size
        val layer = Layer(id = layerId, parentComposition = parentComposition, depth = layerDepth) {
            CompositionLocalProvider(LocalLayerDepth provides layerDepth) {
                layerContent { popById(layerId) }
            }
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
        transitionSpec: ModalTransitionSpec = ModalTransitionSpec(),
        onDismissRequest: () -> Unit = {},
        content: @Composable ModalScope.() -> Unit,
    ) {
        push { popLayer ->
            var entered by remember { mutableStateOf(false) }
            var closing by remember { mutableStateOf(false) }
            val closeScope = rememberCoroutineScope()
            val progress = animateFloat(
                targetValue = if (entered) 1f else 0f,
                spec = AnimationSpec(durationMillis = transitionSpec.durationMillis, easing = transitionSpec.easing),
            )

            fun requestDismiss() {
                if (closing) return
                closing = true
                entered = false
                onDismissRequest()
                closeScope.launch {
                    delay(transitionSpec.durationMillis)
                    popLayer()
                }
            }

            val scope = object : ModalScope {
                override fun dismiss() = requestDismiss()
            }

            LaunchedEffect(Unit) {
                entered = true
            }

            ModalLayout(
                alignment = alignment,
                dismissOnClickOutside = dismissOnClickOutside,
                onDismissRequest = ::requestDismiss,
                transitionSpec = transitionSpec,
                transitionProgress = progress,
                content = { scope.content() },
            )
        }
    }

    /**
     * Pushes a modal presenting a [ConfirmDialog] with confirm/cancel actions. The modal
     * animates out and dismisses itself after either action runs.
     *
     * @param onConfirm Invoked when the user confirms.
     * @param onCancel Invoked when the user cancels.
     * @param content Additional body content shown above the actions.
     */
    fun confirmDialog(
        title: Component = Component.literal("Confirm Dialog"),
        confirmText: Component = Component.literal("Confirm"),
        cancelText: Component = Component.literal("Cancel"),
        onConfirm: () -> Unit = {},
        onCancel: () -> Unit = {},
        content: @Composable () -> Unit
    ) {
        modal(
            dismissOnClickOutside = false
        ) {

            ConfirmDialog(
                title = title,
                confirmText = confirmText,
                cancelText = cancelText,
                onConfirm = onConfirm,
                onCancel = onCancel,
                content = content
            )
        }
    }

    /**
     * Pushes a modal presenting an [AlertDialog] with a single acknowledgement action.
     *
     * @param onConfirm Invoked when the user acknowledges the alert.
     */
    fun alertDialog(
        title: Component = Component.literal("Alert"),
        message: Component,
        confirmText: Component = Component.literal("OK"),
        onConfirm: () -> Unit = {},
    ) {
        modal(dismissOnClickOutside = false) {
            AlertDialog(
                title = title,
                message = message,
                confirmText = confirmText,
                onConfirm = onConfirm,
            )
        }
    }

    /**
     * Pushes a modal presenting a [PromptDialog] for single-line text input.
     *
     * @param initialValue Text prefilled in the input field.
     * @param validator Predicate controlling whether the confirm action is enabled.
     * @param onConfirm Invoked with the entered text when the user confirms.
     * @param onCancel Invoked when the user cancels.
     */
    fun promptDialog(
        title: Component = Component.literal("Enter Value"),
        initialValue: String = "",
        prompt: Component = Component.literal("Enter a value:"),
        confirmText: Component = Component.literal("Confirm"),
        cancelText: Component = Component.literal("Cancel"),
        validator: (String) -> Boolean = { true },
        onConfirm: (String) -> Unit,
        onCancel: () -> Unit = {},
    ) {
        modal(dismissOnClickOutside = false) {
            PromptDialog(
                title = title,
                initialValue = initialValue,
                prompt = prompt,
                confirmText = confirmText,
                cancelText = cancelText,
                validator = validator,
                onConfirm = onConfirm,
                onCancel = onCancel,
            )
        }
    }

    /**
     * Pushes a modal presenting a [ChoiceDialog] listing [choices] for the user to pick from.
     *
     * @param choices The selectable options.
     * @param onSelected Invoked with the chosen value's [ModalChoice.value] when a choice is picked.
     * @param onCancel Invoked when the user cancels without choosing.
     */
    fun <T> choiceDialog(
        title: Component = Component.literal("Choose an Option"),
        message: Component? = null,
        choices: List<ModalChoice<T>>,
        cancelText: Component = Component.literal("Cancel"),
        onSelected: (T) -> Unit,
        onCancel: () -> Unit = {},
    ) {
        modal(dismissOnClickOutside = false) {
            ChoiceDialog(
                title = title,
                message = message,
                choices = choices,
                cancelText = cancelText,
                onSelected = onSelected,
                onCancel = onCancel,
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
        transitionSpec: ModalTransitionSpec,
        transitionProgress: Float,
        content: @Composable () -> Unit,
    ) {
        val alpha = animateInt(
            targetValue = (transitionSpec.maxBackdropAlpha * transitionProgress.coerceIn(0f, 1f)).roundToInt(),
            spec = AnimationSpec(durationMillis = transitionSpec.durationMillis, easing = transitionSpec.easing),
        )
        val offsetY = animateInt(
            targetValue = ((1f - transitionProgress.coerceIn(0f, 1f)) * transitionSpec.enterOffsetY).roundToInt(),
            spec = AnimationSpec(durationMillis = transitionSpec.durationMillis, easing = transitionSpec.easing),
        )
        var rootModifier = Modifier.fillMaxSize()
            .background((alpha.coerceIn(0, 255) shl 24))
        if (dismissOnClickOutside) {
            rootModifier = rootModifier.onPointerEvent<UINode>(PointerEventType.PRESS) { _, event ->
                onDismissRequest()
                event.consume()
            }
        }
        Box(modifier = rootModifier, contentAlignment = alignment) {
            RootContainer(
                modifier = Modifier
                    .offset(x = 0, y = offsetY)
                    .onPointerEvent<UINode>(PointerEventType.PRESS) { _, event -> event.consume() }
                    .zIndex(1f)
            ) {
                content()
            }
        }
    }
}
