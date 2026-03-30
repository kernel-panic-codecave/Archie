package net.kernelpanicsoft.archie.gui.layout

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.kernelpanicsoft.archie.gui.modifiers.*
import net.kernelpanicsoft.archie.gui.modifiers.position.MarginModifier
import net.kernelpanicsoft.archie.gui.modifiers.position.ZIndexModifier
import net.kernelpanicsoft.archie.gui.modifiers.DebugModifier
import net.kernelpanicsoft.archie.gui.modifiers.position.PaddingModifier
import net.kernelpanicsoft.archie.gui.nodes.AUINode
import net.kernelpanicsoft.archie.gui.util.extension.drawRectOutline
import kotlin.reflect.KClass

// ARGB debug overlay colours
private const val COMPONENT_OUTLINE = 0xFF00FFFF.toInt()
private const val DEBUG_OUTLINE     = 0xFF000000.toInt()
private const val DEBUG_FILL        = 0xA7000000.toInt()
private const val DEBUG_TEXT        = 0xFFFFFFFF.toInt()
private const val OUTSET_FILL       = 0x80800080.toInt()
private const val INSET_FILL        = 0x80FF0000.toInt()
private const val LINE_SPACING      = 2
private const val COLUMN_SPACING    = 6

/**
 * The concrete node type that forms Archie's UI scene graph.
 *
 * Every composable in the Archie GUI framework ultimately creates one [LayoutNode].
 * It handles measurement, draw-chain rendering (including [DrawModifier] wrapping),
 * z-index sorting, input hit-testing, and the Ctrl+Shift debug overlay.
 *
 * Do not instantiate directly — use [Layout] and higher-level composables instead.
 *
 * @param nodeName A human-readable label shown in the debug overlay for this node.
 */
class LayoutNode(
    private val nodeName: String = "LayoutNode",
) : Measurable, Placeable, AUINode, MeasureScope {

    override var measurePolicy: MeasurePolicy = ChildMeasurePolicy
    override var renderer: Renderer = EmptyRenderer

    /** Mutable list of child [LayoutNode]s, managed by [AUINodeApplier]. */
    val children = mutableListOf<LayoutNode>()

    fun findNode(name: String): LayoutNode? = children.find { it.nodeName == name } ?: children.map { it.findNode(name) }.firstOrNull()

    override var modifier: Modifier = Modifier
        set(value) {
            field = value
            // Rebuild processed-modifier map (merged by type)
            processedModifier = modifier.foldIn(mutableMapOf()) { acc, element ->
                val existing = acc[element::class]
                acc[element::class] = if (existing != null) existing.unsafeMergeWith(element) else element
                acc
            }
            drawModifiers = modifier.foldIn(mutableListOf()) { acc, element ->
                if (element is DrawModifier) acc.add(element)
                acc
            }
            layoutChangingModifiers = modifier.foldIn(mutableListOf()) { acc, element ->
                if (element is LayoutChangingModifier) acc.add(element)
                acc
            }
        }

    /** Processed modifier map keyed by element type for O(1) lookup. */
    var processedModifier = mapOf<KClass<out Modifier.Element<*>>, Modifier.Element<*>>()
        private set

    /** Ordered list of [DrawModifier]s extracted from [modifier]. */
    var drawModifiers: List<DrawModifier> = emptyList()
        private set

    /** Ordered list of [LayoutChangingModifier]s extracted from [modifier]. */
    var layoutChangingModifiers: List<LayoutChangingModifier> = emptyList()
        private set

    /** Retrieves the merged [Modifier.Element] of type [T] from [processedModifier], or `null`. */
    inline fun <reified T : Modifier.Element<T>> get(): T? = processedModifier[T::class] as? T

    /** The parent [LayoutNode] in the scene graph, or `null` for root nodes. */
    var parent: LayoutNode? = null

    override var width: Int = 0
    override var height: Int = 0
    override var x: Int = 0
    override var y: Int = 0

    /** The effective z-index for this node, used for draw and input ordering. */
    val zIndex: Float get() = get<ZIndexModifier>()?.zIndex ?: 0f

    /** Computes the maximum effective z-depth in this subtree, adding [layerOffset]. */
    fun getMaxZ(layerOffset: Float): Float {
        val myZ = effectiveZ(layerOffset)
        return maxOf(myZ, children.maxOfOrNull { it.getMaxZ(layerOffset) } ?: myZ)
    }

    private fun effectiveZ(layerOffset: Float): Float =
        (parent?.effectiveZ(layerOffset) ?: layerOffset) + zIndex

    /**
     * Absolute on-screen coordinates, accumulating parent offsets up the scene graph.
     */
    val absoluteCoords: IntCoordinates
        get() {
            var coords = IntCoordinates(x, y)
            var p = parent
            while (p != null) { coords += IntCoordinates(p.x, p.y); p = p.parent }
            return coords
        }

    /** The topmost ancestor [LayoutNode] (the root of this subtree). */
    val rootNode: LayoutNode get() = parent?.rootNode ?: this

    /**
     * Whether the debug overlay is active. Setting this on a child propagates to the root.
     */
    var debug: Boolean = false
        get() = parent?.debug ?: field
        set(value) = parent?.let { it.debug = value } ?: run { field = value }

    /**
     * Whether the extended modifier info is shown in the debug overlay. Setting this propagates to the root.
     */
    var extraDebug: Boolean = false
        get() = parent?.extraDebug ?: field
        set(value) = parent?.let { it.extraDebug = value } ?: run { field = value }

    // ── Measurement ───────────────────────────────────────────────────────

    override fun measure(constraints: Constraints): Placeable {
        // Collect outset (margin) from children
        val outset = children.fold(listOf<MarginModifier>()) { acc, child ->
            acc + child.modifier.getAll<MarginModifier>()
        }
        val horizontal = outset.sumOf { it.horizontal }
        val vertical   = outset.sumOf { it.vertical }

        val innerConstraints = layoutChangingModifiers.fold(constraints) { c, m -> m.modifyInnerConstraints(c) }
        val result = measurePolicy.measure(this, children, innerConstraints)

        // Account for padding inset
        val inset = get<PaddingModifier>()
        val insetH = inset?.horizontal ?: 0
        val insetV = inset?.vertical   ?: 0

        val newWidth  = result.width  + horizontal + insetH
        val newHeight = result.height + vertical   + insetV

        if (width != newWidth || height != newHeight) {
            get<OnSizeChangedModifier>()?.onSizeChanged?.invoke(Size(newWidth, newHeight))
        }
        width  = newWidth
        height = newHeight

        val layoutConstraints = layoutChangingModifiers.fold(constraints) { c, m ->
            m.modifyLayoutConstraints(IntSize(newWidth, newHeight), c)
        }
        width  = width.coerceIn(layoutConstraints.minWidth..layoutConstraints.maxWidth)
        height = height.coerceIn(layoutConstraints.minHeight..layoutConstraints.maxHeight)

        result.placer.placeChildren()

        return object : Placeable by this {
            override var width:  Int = this@LayoutNode.width
            override var height: Int = this@LayoutNode.height
        }
    }

    override fun placeAt(x: Int, y: Int) {
        val offset = layoutChangingModifiers.fold(IntOffset(x, y)) { acc, m -> m.modifyPosition(acc) }
        this.x = offset.x
        this.y = offset.y
        get<OnGloballyPositionedModifier>()?.onGloballyPositioned?.invoke(absoluteCoords)
    }

    // ── Rendering ─────────────────────────────────────────────────────────

    override fun render(x: Int, y: Int, guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        render(x, y, guiGraphics, mouseX, mouseY, partialTick, 0f)
    }

    /**
     * Renders this node and its entire subtree, with z-index translation, draw-modifier
     * wrapping, and the optional debug overlay.
     */
    fun render(x: Int, y: Int, guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float, zOffset: Float) {
        if (parent == null) {
            guiGraphics.pose().pushPose()
            renderRecursive(x, y, guiGraphics, mouseX, mouseY, partialTick, zOffset)

            if (rootNode.debug) {
                guiGraphics.pose().pushPose()
                guiGraphics.pose().translate(0.0, 0.0, 1000.0 + zOffset)
                renderDebug(x, y, guiGraphics, mouseX, mouseY, partialTick)
                guiGraphics.pose().popPose()
            }

            guiGraphics.pose().popPose()
        }
    }

    private fun renderRecursive(x: Int, y: Int, guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float, zOffset: Float) {
        val dx = this.x + x
        val dy = this.y + y

        guiGraphics.pose().pushPose()
        guiGraphics.pose().translate(0.0, 0.0, zIndex.toDouble())

        // Build the draw chain from innermost (content) outward through DrawModifiers
        val contentDrawer: () -> Unit = {
            renderer.render(this, dx, dy, guiGraphics, mouseX, mouseY, partialTick)
            children.sortedBy { it.zIndex }.forEach { it.renderRecursive(dx, dy, guiGraphics, mouseX, mouseY, partialTick, zOffset) }
            renderer.renderAfterChildren(this, dx, dy, guiGraphics, mouseX, mouseY, partialTick)
        }

        val drawChain = drawModifiers.reversed().fold(contentDrawer) { acc, mod ->
            {
                val scope = object : ContentDrawScope {
                    override val guiGraphics = guiGraphics
                    override val width  = this@LayoutNode.width
                    override val height = this@LayoutNode.height
                    override val x      = dx
                    override val y      = dy
                    override fun drawContent() = acc()
                }
                with(mod) { scope.draw() }
            }
        }
        drawChain()

        guiGraphics.pose().popPose()
    }

    // ── Debug overlay ─────────────────────────────────────────────────────

    private fun renderDebug(x: Int, y: Int, guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        val dx = this.x + x
        val dy = this.y + y

        val hoveredChildren = children.filter { it.isBounded(mouseX, mouseY) }
        if (hoveredChildren.isNotEmpty()) {
            hoveredChildren.forEach { it.renderDebug(dx, dy, guiGraphics, mouseX, mouseY, partialTick) }
            return
        }
        if (!isBounded(mouseX, mouseY)) return

        guiGraphics.drawRectOutline(dx, dy, width, height, COMPONENT_OUTLINE)

        // Margin (outset) visualisation
        (processedModifier[MarginModifier::class] as? MarginModifier)?.let { mod ->
            with(mod.margin) {
                if (top    != 0) guiGraphics.fill(dx, dy - top, dx + width, dy, OUTSET_FILL)
                if (bottom != 0) guiGraphics.fill(dx, dy + height, dx + width, dy + height + bottom, OUTSET_FILL)
                if (left   != 0) guiGraphics.fill(dx - left, dy, dx, dy + height, OUTSET_FILL)
                if (right  != 0) guiGraphics.fill(dx + width, dy, dx + width + right, dy + height, OUTSET_FILL)
            }
        }

        // Padding (inset) visualisation
        (processedModifier[PaddingModifier::class] as? PaddingModifier)?.let { mod ->
            with(mod.padding) {
                if (top    != 0) guiGraphics.fill(dx + left, dy, dx + width - right, dy + top, INSET_FILL)
                if (bottom != 0) guiGraphics.fill(dx + left, dy + height - bottom, dx + width - right, dy + height, INSET_FILL)
                if (left   != 0) guiGraphics.fill(dx, dy + top, dx + left, dy + height - bottom, INSET_FILL)
                if (right  != 0) guiGraphics.fill(dx + width - right, dy + top, dx + width, dy + height - bottom, INSET_FILL)
            }
        }

        // Tooltip panel
        val font = Minecraft.getInstance().font
        var tooltipY = dy + height + 1

        val debugLines: List<List<Component>> = buildList {
            add(listOf(Component.literal(nodeName)))
            add(listOf(
                Component.literal("X:").apply { append(Component.literal("$dx").withColor(0x00FFFF)); append(", Y:"); append(Component.literal("$dy").withColor(0x32CD32)) },
                Component.literal("W:").apply { append(Component.literal("$width").withColor(0xFFA500)); append(", H:"); append(Component.literal("$height").withColor(0x87CEEB)) },
            ))
            if (extraDebug) {
                val mods = mutableListOf<Component>()
                modifier.all { mod ->
                    if (mod is DebugModifier) mods.addAll(0, mod.toComponents())
                    else mods.add(mod.toComponent())
                    true
                }
                if (mods.isNotEmpty()) {
                    add(listOf(Component.literal("Modifiers:")))
                    mods.forEach { add(listOf(it)) }
                }
            }
        }

        val lineWidths  = debugLines.map { line -> line.sumOf { font.width(it) } + (line.size - 1) * COLUMN_SPACING }
        val maxLineWidth = (lineWidths.maxOrNull() ?: 0) + 4
        val panelHeight  = debugLines.size * (font.lineHeight + LINE_SPACING) - LINE_SPACING + 2

        if (tooltipY + panelHeight > guiGraphics.guiHeight()) tooltipY -= height + panelHeight + 2

        guiGraphics.drawRectOutline(dx + 1, tooltipY, maxLineWidth, panelHeight, DEBUG_OUTLINE)
        guiGraphics.fill(dx + 1, tooltipY, dx + 1 + maxLineWidth, tooltipY + panelHeight, DEBUG_FILL)

        debugLines.forEachIndexed { row, line ->
            var colX = dx + 3
            val textY = tooltipY + row * (font.lineHeight + LINE_SPACING) + 1
            line.forEachIndexed { col, text ->
                guiGraphics.drawString(font, text, colX, textY, DEBUG_TEXT)
                if (col < line.size - 1) colX += font.width(text) + COLUMN_SPACING
            }
        }
    }

    // ── Hit testing ───────────────────────────────────────────────────────

    /**
     * Returns `true` if ([mouseX], [mouseY]) falls within this node's absolute screen bounds.
     */
    fun isBounded(mouseX: Int, mouseY: Int): Boolean {
        val (ax, ay) = absoluteCoords
        return mouseX in ax until (ax + width) && mouseY in ay until (ay + height)
    }

    override fun toString() = children.joinToString(prefix = "$nodeName(", postfix = ")")

    internal companion object {
        val ChildMeasurePolicy = MeasurePolicy { scope, measurables, constraints ->
            val placeables = measurables.map { it.measure(constraints) }
            MeasureResult(
                placeables.maxOfOrNull { it.width }  ?: 0,
                placeables.maxOfOrNull { it.height } ?: 0,
            ) { placeables.forEach { it.placeAt(0, 0) } }
        }
    }
}

/** A [Renderer] that performs no drawing — the default for layout-only nodes. */
val EmptyRenderer = object : Renderer {}
