package net.kernelpanicsoft.archie.gui.layer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionContext
import net.kernelpanicsoft.archie.gui.layout.LayoutNode
import net.kernelpanicsoft.archie.gui.nodes.AUINodeApplier
import java.util.*

/**
 * A self-contained UI layer with its own independent [LayoutNode] tree and [Composition].
 *
 * Layers are used to implement overlapping UI surfaces such as modals, dropdowns, and
 * tooltips. Each layer has its own root node that is measured and rendered separately
 * from the base screen content.
 *
 * Layers are managed by [LayerStackManager]. Do not create or dispose [Layer] instances
 * directly; use [LayerStackManager.push] or [LayerStackManager.modal] instead.
 *
 * @property id                Unique identifier for this layer, used for removal.
 * @property rootNode          The root [LayoutNode] of this layer's composition tree.
 * @property composition       The Compose [Composition] backing this layer.
 */
class Layer(
    val id: UUID = UUID.randomUUID(),
    parentComposition: CompositionContext,
    content: @Composable () -> Unit,
) {
    val rootNode = LayoutNode("Root")
    val composition = Composition(AUINodeApplier(rootNode), parentComposition)

    init {
        composition.setContent(content)
    }

    /**
     * Disposes the Compose [Composition] associated with this layer, releasing all
     * remembered state and coroutines.
     *
     * Called automatically by [LayerStackManager.pop] and [LayerStackManager.popById].
     */
    fun dispose() = composition.dispose()
}
