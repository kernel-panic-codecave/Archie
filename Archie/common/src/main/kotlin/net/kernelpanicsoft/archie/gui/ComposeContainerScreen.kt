package net.kernelpanicsoft.archie.gui

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.snapshots.Snapshot
import com.mojang.blaze3d.platform.InputConstants
import kotlinx.coroutines.*
import net.kernelpanicsoft.archie.gui.access.SlotHighlightClipProvider
import net.kernelpanicsoft.archie.gui.access.SlotLayerDepthProvider
import net.kernelpanicsoft.archie.gui.blockentity.LocalBlockEntityState
import net.kernelpanicsoft.archie.gui.composables.containers.RootContainer
import net.kernelpanicsoft.archie.gui.layer.LayerStackManager
import net.kernelpanicsoft.archie.gui.layer.LocalLayerManager
import net.kernelpanicsoft.archie.gui.layout.IntCoordinates
import net.kernelpanicsoft.archie.gui.layout.IntRect
import net.kernelpanicsoft.archie.gui.layout.LayoutNode
import net.kernelpanicsoft.archie.gui.modifiers.Constraints
import net.kernelpanicsoft.archie.gui.modifiers.input.PointerEventType
import net.kernelpanicsoft.archie.gui.util.extension.processCharEvent
import net.kernelpanicsoft.archie.gui.util.extension.processDragEvent
import net.kernelpanicsoft.archie.gui.util.extension.processKeyEvent
import net.kernelpanicsoft.archie.gui.util.extension.processPointerEvent
import net.kernelpanicsoft.archie.gui.util.extension.processScrollEvent
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.Slot
import net.minecraft.world.level.block.entity.BlockEntity
import org.lwjgl.glfw.GLFW
import kotlin.coroutines.CoroutineContext

/** Provides the current [ComposeContainerScreen] to any composable in its tree. */
val LocalContainerScreen: ProvidableCompositionLocal<ComposeContainerScreen<*, *>> =
    compositionLocalOf { throw IllegalStateException("Screen has not been provided") }

/** Provides the current [ComposeContainerMenu] to any composable in its tree. */
val LocalContainerMenu: ProvidableCompositionLocal<ComposeContainerMenu<*, *>> =
    compositionLocalOf { throw IllegalStateException("Screen has not been provided") }

/**
 * A Compose-driven [AbstractContainerScreen] with layer support, async recomposition, and
 * vanilla [Slot] rendering kept in sync with the Compose-reported [SlotGroup] layout.
 *
 * Behaves like [ComposeScreen] but additionally bridges vanilla's container/slot machinery:
 * the base layer (layer 0) is rendered from [renderBg] so it draws under vanilla's slots, and
 * any additional layers (modals) render on top from [render] via [renderSlot]/[slotClipRect]
 * clipping so scrolled-out-of-view slots don't paint over unrelated content.
 *
 * Extend this class and call [start] inside your `init()` override, the same way as
 * [ComposeScreen].
 *
 * @param T The concrete [ComposeContainerMenu] subclass driving this screen.
 * @param B The [BlockEntity] type backing [T].
 * @param menu The container menu instance for this screen.
 * @param playerInventory The opening player's inventory.
 * @param title The screen title passed to the vanilla [AbstractContainerScreen] constructor.
 * @param asynchronous When `true` (default), recomposition runs off the main thread and
 *   the result is joined at the start of the next frame for smooth, non-blocking updates.
 *   Set to `false` to force synchronous recomposition (simpler but may stutter).
 */
abstract class ComposeContainerScreen<T : ComposeContainerMenu<B, T>, B : BlockEntity>(
	menu: T, playerInventory: Inventory, title: Component,
    val asynchronous: Boolean = true,
) : AbstractContainerScreen<T>(menu, playerInventory, title),
    CoroutineScope,
    SlotLayerDepthProvider,
    SlotHighlightClipProvider,
    ComposeIdleAware,
    LayerManagerProvider
{
    companion object {
        private const val BASE_LAYER_Z = 100f
        private const val LAYER_Z_STEP = 200f
        private const val SLOT_LAYER_OFFSET = 120f

        /** The base Z offset used when rendering the layer at [layerDepth], deepest layers on top. */
        fun layerBaseZ(layerDepth: Int): Float = BASE_LAYER_Z + layerDepth * LAYER_Z_STEP
    }


    private var hasFrameWaiters = false
    private val clock = BroadcastFrameClock { hasFrameWaiters = true }

    private val composeScope = CoroutineScope(Dispatchers.Default) + clock
    final override val coroutineContext: CoroutineContext = composeScope.coroutineContext

    final override lateinit var layerManager: LayerStackManager
        private set
    private lateinit var recomposer: Recomposer
    private var recomposeJob: Job? = null

    private var applyScheduled = false
    private val snapshotHandle = Snapshot.registerGlobalWriteObserver {
        if (!applyScheduled) {
            applyScheduled = true
            composeScope.launch {
                applyScheduled = false
                Snapshot.sendApplyNotifications()
            }
        }
    }

    private var lastMouseX = 0.0
    private var lastMouseY = 0.0

    override fun isComposeIdle(): Boolean =
        !applyScheduled && !hasFrameWaiters && recomposeJob?.isActive != true

    /** [titleLabelX]/[titleLabelY] expressed as an absolute-screen [IntCoordinates] pair. */
    var titleLabelPos: IntCoordinates
        get() = IntCoordinates(titleLabelX, titleLabelY)
        set(value) {
            titleLabelX = value.x - leftPos
            titleLabelY = value.y - topPos
        }

    /** [inventoryLabelX]/[inventoryLabelY] expressed as an absolute-screen [IntCoordinates] pair. */
    var inventoryLabelPos: IntCoordinates
        get() = IntCoordinates(inventoryLabelX, inventoryLabelY)
        set(value) {
            inventoryLabelX = value.x - leftPos
            inventoryLabelY = value.y - topPos
        }

    /**
     * Initialises the Compose runtime and pushes the base layer with [content].
     *
     * Must be called once from [init]. Subsequent calls replace the content.
     *
     * @param content The root composable content for this screen.
     */
    protected fun start(content: @Composable () -> Unit) {
        recomposer = Recomposer(coroutineContext)
        layerManager = LayerStackManager(recomposer)

        AUIScopeManager.scopes += composeScope
        launch { recomposer.runRecomposeAndApplyChanges() }

        layerManager.push { _ ->
            CompositionLocalProvider(
                LocalContainerScreen provides this,
                LocalContainerMenu provides menu,
                LocalSlotData provides menu.slotData,
                LocalBlockEntityState provides menu.blockEntityState,
                LocalLayerManager provides layerManager,
            ) {
                RootContainer {
                    content()
                }
            }
        }
    }

    // ── Rendering ─────────────────────────────────────────────────────────

    /**
     * Measures and renders all active layers.
     *
     * In async mode the previous recompose job is joined before rendering, then a new
     * job is launched if there are pending frame waiters.
     */
    open fun renderNodes(baseLayer: Boolean, guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        if (asynchronous) {
            recomposeJob?.let { job ->
                runBlocking {
                    job.join()
                }
                recomposeJob = null
            }
        } else if (hasFrameWaiters) {
            hasFrameWaiters = false
            clock.sendFrame(System.nanoTime())
        }

        // layerManager.layers is a SnapshotStateList that can be structurally mutated (modal
        // push/dismiss) from the recomposer coroutine on another thread while this method runs on
        // the render thread. Reading it inside a snapshot gives a frozen, consistent view for the
        // whole size-check-then-index sequence below, instead of racing the live list. A mutable
        // (not read-only) snapshot is required because measure() can itself write state (e.g.
        // ScrollableState.setChildSize), and those writes must be applied back afterward.
        val layersSnapshot = Snapshot.takeMutableSnapshot()
        try {
            layersSnapshot.enter {
                val layerIndices = if (baseLayer) {
                    if (layerManager.layers.isEmpty()) return@enter
                    0..0
                } else {
                    if (layerManager.layers.size <= 1) return@enter
                    1 until layerManager.layers.size
                }

                for (layerIndex in layerIndices) {
                    val layer = layerManager.layers[layerIndex]
                    val rootNode = layer.rootNode
                    rootNode.measure(Constraints(maxWidth = width, maxHeight = height))
                    rootNode.render(0, 0, guiGraphics, mouseX, mouseY, partialTick, layerBaseZ(layerIndex))
                }

                layerManager.screenSize.let { (width, height) ->
                    imageWidth = width
                    imageHeight = height
                }
                layerManager.screenPos.let { (x, y) ->
                    if (x == 0 && y == 0)
                        return@let
                    leftPos = x
                    topPos = y
                    menu.screenLeftPos = leftPos
                    menu.screenTopPos = topPos
                }
                menu.refreshSlotPositions()

                if (asynchronous && hasFrameWaiters) {
                    hasFrameWaiters = false
                    recomposeJob = composeScope.launch {
                        clock.sendFrame(System.nanoTime())
                    }
                }
            }
            layersSnapshot.apply().check()
        } finally {
            layersSnapshot.dispose()
        }
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(guiGraphics, mouseX, mouseY, partialTick)
        renderTooltip(guiGraphics, mouseX, mouseY)
        if (layerManager.layers.size > 1)
        {
            renderNodes(false, guiGraphics, mouseX, mouseY, partialTick)
        }
    }

    override fun isHovering(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        mouseX: Double,
        mouseY: Double
    ): Boolean
    {
        if (layerManager.layers.size != 1) return false

        if (width == 16 && height == 16) {
            val slotIndex = menu.slots.indexOfFirst { it.x == x && it.y == y }
            val clip = if (slotIndex >= 0) menu.slotClipBounds(slotIndex) else null
            if (clip != null) {
                val absX = leftPos + x
                val absY = topPos + y
                val visible = IntRect(absX, absY, absX + width, absY + height).intersect(clip) ?: return false
                return mouseX >= visible.minX - 1 && mouseX < visible.maxX + 1 &&
                    mouseY >= visible.minY - 1 && mouseY < visible.maxY + 1
            }
        }

        return super.isHovering(x, y, width, height, mouseX, mouseY)
    }

    override fun renderBg(guiGraphics: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        renderNodes(true, guiGraphics, mouseX, mouseY, partialTick)
    }

    /**
     * Hook point for slot rendering customisation.
     *
     * By default, slots are clipped against the container bounds so partially visible
     * slots still render correctly when Compose repositions them.
     */
    override fun renderSlot(guiGraphics: GuiGraphics, slot: Slot) {
        val clip = slotClipRect(slot) ?: return

        guiGraphics.enableScissor(clip.minX, clip.minY, clip.maxX, clip.maxY)
        try {
            super.renderSlot(guiGraphics, slot)
        } finally {
            guiGraphics.disableScissor()
        }
    }

    /**
     * Called by [net.kernelpanicsoft.archie.mixin.client.gui.AbstractContainerScreenMixin]
     * (via [net.kernelpanicsoft.archie.gui.access.SlotHighlightClipProvider]) to clip the
     * hover-highlight overlay the same way [renderSlot] clips the item icon - vanilla only
     * exposes a static `renderSlotHighlight(GuiGraphics, x, y, blitOffset)` with no per-slot
     * override point, so this has to be reached from a mixin redirect instead of `override`.
     */
    override fun slotHighlightClipRect(x: Int, y: Int): IntRect? {
        val slot = menu.slots.firstOrNull { it.x == x && it.y == y } ?: return null
        return slotClipRect(slot)
    }

    override fun slotRenderLayerOffset(slot: Slot): Float? = slotRenderLayerZ(slot)

    /**
     * Z layer used when rendering a specific vanilla [slot].
     *
     * Slots render above the compose content of the layer that owns them, while
     * still remaining below content from higher layers.
     */
    protected open fun slotRenderLayerZ(slot: Slot): Float {
        val slotIndex = menu.slots.indexOf(slot).takeIf { it >= 0 } ?: return layerBaseZ(0) + SLOT_LAYER_OFFSET
        val layerDepth = menu.slotLayerDepth(slotIndex)
        return layerBaseZ(layerDepth) + SLOT_LAYER_OFFSET
    }

    protected open fun slotRenderLayerZ(): Float = layerBaseZ(0) + SLOT_LAYER_OFFSET

    /**
     * Computes the clip rectangle for [slot] in absolute screen coordinates.
     *
     * Intersects the overall container bounds with the slot's own group clip (if it sits
     * inside a [net.kernelpanicsoft.archie.gui.composables.containers.Scrollable] viewport),
     * so a slot scrolled out of view is actually clipped instead of rendering on top of
     * whatever else occupies that screen area.
     *
     * Returns `null` when the slot does not intersect the (possibly narrower) clip area.
     */
    protected open fun slotClipRect(slot: Slot): IntRect? {
        val containerClip = IntRect(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight)
        val slotIndex = menu.slots.indexOf(slot).takeIf { it >= 0 }
        val groupClip = slotIndex?.let { menu.slotClipBounds(it) }
        val effectiveClip = groupClip?.let { containerClip.intersect(it) } ?: containerClip

        val slotMinX = leftPos + slot.x
        val slotMinY = topPos + slot.y
        val slotRect = IntRect(slotMinX, slotMinY, slotMinX + 16, slotMinY + 16)

        return effectiveClip.intersect(slotRect)
    }

    override fun onClose() {
        GLFW.glfwSetCursor(minecraft!!.window.window, 0L)
        super.onClose()
        recomposeJob?.cancel("GUI closing")
        recomposer.close()
        snapshotHandle.dispose()
        layerManager.layers.forEach { it.dispose() }
        AUIScopeManager.scopes -= composeScope
        composeScope.cancel()
    }

    private fun getTopNode(): LayoutNode? = layerManager.top?.rootNode

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val topNode = getTopNode() ?: return super.mouseClicked(mouseX, mouseY, button)
        processPointerEvent(topNode, mouseX, mouseY, PointerEventType.GLOBAL_PRESS, true)
        val event = processPointerEvent(topNode, mouseX, mouseY, PointerEventType.PRESS)
        return event.bypassSuper || super.mouseClicked(mouseX, mouseY, button)
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val topNode = getTopNode() ?: return super.mouseReleased(mouseX, mouseY, button)
        processPointerEvent(topNode, mouseX, mouseY, PointerEventType.GLOBAL_RELEASE, true)
        val event = processPointerEvent(topNode, mouseX, mouseY, PointerEventType.RELEASE)
        return event.bypassSuper || super.mouseReleased(mouseX, mouseY, button)
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        val topNode = getTopNode() ?: return super.mouseMoved(mouseX, mouseY)
        processPointerEvent(topNode, mouseX, mouseY, PointerEventType.MOVE)

        processPointerEvent(
            topNode,
            mouseX,
            mouseY,
            PointerEventType.ENTER
        ) {
            it.isBounded(mouseX.toInt(), mouseY.toInt()) && !it.isBounded(
                lastMouseX.toInt(),
                lastMouseY.toInt()
            )
        }

        processPointerEvent(
            topNode,
            mouseX,
            mouseY,
            PointerEventType.EXIT
        ) {
            !it.isBounded(mouseX.toInt(), mouseY.toInt()) && it.isBounded(
                lastMouseX.toInt(),
                lastMouseY.toInt()
            )
        }

        lastMouseX = mouseX
        lastMouseY = mouseY
        super.mouseMoved(mouseX, mouseY)
    }

    override fun mouseScrolled(
        mouseX: Double,
        mouseY: Double,
        scrollX: Double,
        scrollY: Double
    ): Boolean {
        val topNode = getTopNode() ?: return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)
        processScrollEvent(topNode, mouseX, mouseY, scrollX, scrollY, PointerEventType.GLOBAL_SCROLL, true)
        val event =
            processScrollEvent(topNode, mouseX, mouseY, scrollX, scrollY, PointerEventType.SCROLL)
        return event.bypassSuper || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)
    }

    override fun mouseDragged(
        mouseX: Double,
        mouseY: Double,
        button: Int,
        dragX: Double,
        dragY: Double
    ): Boolean {
        val topNode =
            getTopNode() ?: return super.mouseDragged(mouseX, mouseY, button, dragX, dragY)
        val event =
            processDragEvent(topNode, mouseX, mouseY, button, dragX, dragY, PointerEventType.DRAG)
        return event.bypassSuper || super.mouseDragged(mouseX, mouseY, button, dragX, dragY)
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        val topNode = getTopNode() ?: return super.keyPressed(keyCode, scanCode, modifiers)
        // Ctrl+Shift+D toggles the debug overlay. A bitwise check (not `modifiers == 3`) is
        // required here - GLFW also sets bits for Caps Lock/Num Lock in `modifiers` when those
        // are active, so an exact-equality check against just the Ctrl+Shift bitmask silently
        // never matches on those systems.
        val ctrlShiftMask = GLFW.GLFW_MOD_CONTROL or GLFW.GLFW_MOD_SHIFT
        if (keyCode == InputConstants.KEY_D && (modifiers and ctrlShiftMask) == ctrlShiftMask) {
            topNode.debug = !topNode.debug
        }
        if (topNode.debug && keyCode == InputConstants.KEY_LSHIFT) topNode.extraDebug = true

        val event = processKeyEvent(topNode, keyCode, scanCode, modifiers)
        return event.bypassSuper || super.keyPressed(keyCode, scanCode, modifiers)
    }

    override fun charTyped(codePoint: Char, modifiers: Int): Boolean {
        val topNode = getTopNode() ?: return super.charTyped(codePoint, modifiers)
        val event = processCharEvent(topNode, codePoint, modifiers)
        return event.bypassSuper || super.charTyped(codePoint, modifiers)
    }

    override fun keyReleased(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        val baseNode = layerManager.layers.firstOrNull()?.rootNode
        if (baseNode != null && baseNode.debug && keyCode == InputConstants.KEY_LSHIFT) {
            baseNode.extraDebug = false
        }
        return super.keyReleased(keyCode, scanCode, modifiers)
    }
}