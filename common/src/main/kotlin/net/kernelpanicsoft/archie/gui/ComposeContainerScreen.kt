package net.kernelpanicsoft.archie.gui

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.snapshots.Snapshot
import com.mojang.blaze3d.platform.InputConstants
import net.kernelpanicsoft.archie.gui.layout.LayoutNode
import net.kernelpanicsoft.archie.gui.modifiers.Constraints
import kotlinx.coroutines.*
import net.kernelpanicsoft.archie.gui.blockentity.LocalBlockEntityState
import net.kernelpanicsoft.archie.gui.composables.containers.RootContainer
import net.kernelpanicsoft.archie.gui.layer.LayerStackManager
import net.kernelpanicsoft.archie.gui.layer.LocalLayerManager
import net.kernelpanicsoft.archie.gui.layout.IntCoordinates
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
) : AbstractContainerScreen<T>(menu, playerInventory, title), CoroutineScope
{

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

        var zOffset = if (baseLayer) 100f else 0f
        val layers = if (!baseLayer && layerManager.layers.size > 1)
            layerManager.layers.slice(1 until layerManager.layers.size)
        else
            layerManager.layers.firstOrNull()?.let { listOf(it) } ?: return

        for (layer in layers) {
            val rootNode = layer.rootNode
            rootNode.measure(Constraints(maxWidth = width, maxHeight = height))
            rootNode.render(0, 0, guiGraphics, mouseX, mouseY, partialTick, zOffset)
            zOffset = rootNode.getMaxZ(zOffset) + 10.0f
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

        if (asynchronous and hasFrameWaiters) {
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

    override fun onClose() {
        GLFW.glfwSetCursor(
            minecraft!!.window.window,
            GLFW.glfwCreateStandardCursor(GLFW.GLFW_ARROW_CURSOR)
        )
        super.onClose()
        recomposeJob?.cancel("GUI closing")
        recomposer.close()
        snapshotHandle.dispose()
        layerManager.layers.forEach { it.dispose() }
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