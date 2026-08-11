package net.kernelpanicsoft.archie.gui.modifiers

import net.minecraft.client.gui.GuiGraphics

/**
 * A [Modifier] element that can participate in the draw chain for a composable node.
 *
 * [DrawModifier]s wrap the node's normal render call, allowing effects to be drawn
 * **before** (e.g. a background fill) or **after** (e.g. an overlay) the node's own
 * content. The modifier calls [ContentDrawScope.drawContent] to trigger the wrapped render.
 *
 * Implement [draw] inside the modifier to define the drawing logic.
 */
interface DrawModifier {
    /**
     * Performs custom drawing for this modifier.
     *
     * Call [ContentDrawScope.drawContent] at the desired point to render the wrapped content.
     * Omitting the call suppresses the node's normal rendering entirely.
     */
    fun ContentDrawScope.draw()
}

/**
 * Receiver scope provided to [DrawModifier.draw] containing everything needed to render
 * and position content.
 */
interface ContentDrawScope {
    /** The current [GuiGraphics] context. */
    val guiGraphics: GuiGraphics

    /** The width of the node being drawn, in pixels. */
    val width: Int

    /** The height of the node being drawn, in pixels. */
    val height: Int

    /** The absolute x coordinate of the node's top-left corner on screen. */
    val x: Int

    /** The absolute y coordinate of the node's top-left corner on screen. */
    val y: Int

    /**
     * Renders the wrapped content (the node's own renderer and all child nodes).
     *
     * Call this at any point inside [DrawModifier.draw] to position the content
     * relative to any surrounding draw operations.
     */
    fun drawContent()
}
