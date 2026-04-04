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
import net.kernelpanicsoft.archie.gui.access.SlotLayerDepthProvider
import net.kernelpanicsoft.archie.gui.blockentity.LocalBlockEntityState
import net.kernelpanicsoft.archie.gui.composables.containers.RootContainer
import net.kernelpanicsoft.archie.gui.layer.LayerStackManager
import net.kernelpanicsoft.archie.gui.layer.LocalLayerManager
import net.kernelpanicsoft.archie.gui.layout.IntCoordinates
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

val LocalContainerScreen: ProvidableCompositionLocal<ComposeContainerScreen<*, *>> =
    compositionLocalOf { throw IllegalStateException("Screen has not been provided") }
val LocalContainerMenu: ProvidableCompositionLocal<ComposeContainerMenu<*, *>> =
    compositionLocalOf { throw IllegalStateException("Screen has not been provided") }

abstract class ComposeContainerScreen<T : ComposeContainerMenu<B, T>, B : BlockEntity>(
	menu: T, playerInventory: Inventory, title: Component,
    val asynchronous: Boolean = true,
) : AbstractContainerScreen<T>(menu, playerInventory, title),
    CoroutineScope,
    SlotLayerDepthProvider
{
    companion object {
        private const val BASE_LAYER_Z = 100f
        private const val LAYER_Z_STEP = 200f
        private const val SLOT_LAYER_OFFSET = 120f

        fun layerBaseZ(layerDepth: Int): Float = BASE_LAYER_Z + layerDepth * LAYER_Z_STEP
    }


    private var hasFrameWaiters = false
    private val clock = BroadcastFrameClock { hasFrameWaiters = true }

    private val composeScope = CoroutineScope(Dispatchers.Default) + clock
    final override val coroutineContext: CoroutineContext = composeScope.coroutineContext

    private lateinit var layerManager: LayerStackManager
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


    var titleLabelPos: IntCoordinates
        get() = IntCoordinates(titleLabelX, titleLabelY)
        set(value) {
            titleLabelX = value.x - leftPos
            titleLabelY = value.y - topPos
        }

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

        val layerIndices = if (baseLayer) {
            if (layerManager.layers.isEmpty()) return
            0..0
        } else {
            if (layerManager.layers.size <= 1) return
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

        if (asynchronous && hasFrameWaiters) {
            hasFrameWaiters = false
            recomposeJob = composeScope.launch {
                clock.sendFrame(System.nanoTime())
            }
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
     * Returns `null` when the slot does not intersect the container area.
     */
    protected open fun slotClipRect(slot: Slot): SlotClipRect? {
        val slotMinX = leftPos + slot.x
        val slotMinY = topPos + slot.y
        val slotMaxX = slotMinX + 16
        val slotMaxY = slotMinY + 16

        val clipMinX = leftPos
        val clipMinY = topPos
        val clipMaxX = leftPos + imageWidth
        val clipMaxY = topPos + imageHeight

        val minX = maxOf(slotMinX, clipMinX)
        val minY = maxOf(slotMinY, clipMinY)
        val maxX = minOf(slotMaxX, clipMaxX)
        val maxY = minOf(slotMaxY, clipMaxY)

        if (maxX <= minX || maxY <= minY) return null
        return SlotClipRect(minX, minY, maxX, maxY)
    }

    protected data class SlotClipRect(
        val minX: Int,
        val minY: Int,
        val maxX: Int,
        val maxY: Int,
    )

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
        // CTRL + SHIFT
        // CTRL is detected as modifier 3
        // SHIFT is the detected key
        if (keyCode == InputConstants.KEY_D && modifiers == 3) topNode.debug =
            (!topNode.debug)
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