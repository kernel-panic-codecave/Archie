package net.kernelpanicsoft.archie.gui.nodes

import net.kernelpanicsoft.archie.gui.layout.LayoutNode
import net.kernelpanicsoft.archie.gui.layout.MeasurePolicy
import net.kernelpanicsoft.archie.gui.layout.Renderer
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.minecraft.client.gui.GuiGraphics

/**
 * A node in Archie's Compose-based UI tree, exposing the layout/render state a node needs
 * regardless of its concrete representation. Implemented by [LayoutNode], the tree node type
 * produced by [LayoutNodeApplier].
 */
interface UINode {
	/** Determines how this node measures and places its children. */
	var measurePolicy: MeasurePolicy

	/** Draws this node's own content (not its children) each frame. */
	var renderer: Renderer

	/** The chained [Modifier] applied to this node. */
	var modifier: Modifier

	/** This node's measured width, in pixels. */
	var width: Int

	/** This node's measured height, in pixels. */
	var height: Int

	/** This node's placed x position, in pixels, relative to its parent. */
	var x: Int

	/** This node's placed y position, in pixels, relative to its parent. */
	var y: Int

	/**
	 * The [net.kernelpanicsoft.archie.gui.composables.theme.TextureStates] key a stateful
	 * [Renderer] most recently selected to draw (e.g. `"focused"`, `"clicked_and_focused"`),
	 * or `null` for nodes that don't render theme-state-driven visuals.
	 *
	 * Set by the [Renderer] itself, purely as a test hook - lets a client GameTest assert which
	 * visual state a component resolved to without pixel comparison. Not read by the framework.
	 */
	var renderState: String?

	/**
	 * Renders this node and its subtree at the given absolute screen position.
	 *
	 * @param x Absolute screen x position to render at.
	 * @param y Absolute screen y position to render at.
	 * @param guiGraphics The graphics context to draw with.
	 * @param mouseX Current mouse x position, in screen space.
	 * @param mouseY Current mouse y position, in screen space.
	 * @param partialTick Fractional tick time for this frame, for smooth animation.
	 */
	fun render(x: Int, y: Int, guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float)
}