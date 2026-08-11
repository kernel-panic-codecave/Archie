package net.kernelpanicsoft.archie.gui.layout

import androidx.compose.runtime.Stable
import net.kernelpanicsoft.archie.gui.nodes.UINode
import net.kernelpanicsoft.archie.gui.modifiers.Constraints
import net.minecraft.client.gui.GuiGraphics

/**
 * Marker interface implemented by [LayoutNode] and passed as the first argument to
 * [MeasurePolicy.measure]. Measure policies may cast this to [LayoutNode] to access
 * node-level properties such as padding or margin modifiers during layout.
 */
interface MeasureScope

/**
 * The result of a [MeasurePolicy.measure] call, containing the intrinsic dimensions of the
 * node and a [Placer] that positions child nodes within those bounds.
 *
 * @property width  The measured width in pixels.
 * @property height The measured height in pixels.
 * @property placer The [Placer] that executes child placement when called.
 */
data class MeasureResult(
    val width: Int,
    val height: Int,
    val placer: Placer,
)

/**
 * Defines how a [LayoutNode] measures itself and its children.
 *
 * The `scope` parameter is the [LayoutNode] currently being measured, allowing
 * measure policies to read node properties (e.g. padding) during layout.
 */
@Stable
fun interface MeasurePolicy {
    /**
     * Measures [measurables] within [constraints] and returns a [MeasureResult].
     *
     * @param scope       The [LayoutNode] currently being measured (implements [MeasureScope]).
     * @param measurables The child nodes to measure.
     * @param constraints The size constraints imposed by the parent.
     */
    fun measure(scope: MeasureScope, measurables: List<Measurable>, constraints: Constraints): MeasureResult
}

/**
 * A deferred child-placement action returned inside a [MeasureResult].
 *
 * The [placeChildren] function is invoked by [LayoutNode] after measurement is complete to
 * call [Placeable.placeAt] on each child.
 */
@Stable
fun interface Placer {
    /** Executes all [Placeable.placeAt] calls for this layout pass. */
    fun placeChildren()
}

/**
 * Defines the rendering behaviour of a [LayoutNode].
 *
 * Both [render] and [renderAfterChildren] have no-op defaults so implementors only
 * override what they need.
 */
@Stable
interface Renderer {
    /**
     * Called before the node's children are rendered.
     *
     * Use this for backgrounds, borders, or content that should appear *below* children.
     */
    fun render(
	    node: UINode, x: Int, y: Int,
	    guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float,
    ) {}

    /**
     * Called after all children have been rendered.
     *
     * Use this for overlays or post-process effects that should appear *above* children.
     */
    fun renderAfterChildren(
	    node: UINode, x: Int, y: Int,
	    guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float,
    ) {}
}

/**
 * A node that can participate in a layout pass by returning a [Placeable].
 */
interface Measurable {
    /**
     * Measures this node within [constraints] and returns a [Placeable] for placement.
     *
     * @param constraints The size constraints imposed by the parent.
     */
    fun measure(constraints: Constraints): Placeable
}

/**
 * The result of measuring a node, which can subsequently be positioned via [placeAt].
 */
interface Placeable {
    /** The measured width in pixels. */
    var width: Int

    /** The measured height in pixels. */
    var height: Int

    /**
     * Places this node at the given screen coordinates.
     *
     * @param x Absolute x position in screen pixels.
     * @param y Absolute y position in screen pixels.
     */
    fun placeAt(x: Int, y: Int)

    /** Places this node using an [IntOffset] convenience type. */
    fun placeAt(offset: IntOffset) = placeAt(offset.x, offset.y)

    /** The measured size as an [IntSize] value. */
    val size: IntSize get() = IntSize(width, height)
}
