package net.kernelpanicsoft.archie.gui

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.snapshots.Snapshot
import com.mojang.blaze3d.platform.InputConstants
import net.kernelpanicsoft.archie.gui.layout.Alignment
import net.kernelpanicsoft.archie.gui.layout.Box
import net.kernelpanicsoft.archie.gui.layout.LayoutNode
import net.kernelpanicsoft.archie.gui.modifiers.Constraints
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.modifiers.fillMaxSize
import net.kernelpanicsoft.archie.gui.nodes.AUINodeApplier
import kotlinx.coroutines.*
import net.kernelpanicsoft.archie.gui.layer.LayerStackManager
import net.kernelpanicsoft.archie.gui.layer.LocalLayerManager
import net.kernelpanicsoft.archie.gui.modifiers.input.PointerEventType
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.AbstractContainerMenu
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
                LocalLayerManager provides layerManager,
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
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
    open fun renderNodes(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        if (asynchronous) {
            recomposeJob?.let { runBlocking { it.join() } }
            recomposeJob = null
        } else if (hasFrameWaiters) {
            hasFrameWaiters = false
            clock.sendFrame(System.nanoTime())
        }

        var zOffset = 0f
        for (layer in layerManager.layers) {
            val root = layer.rootNode
            root.measure(Constraints(maxWidth = width, maxHeight = height))
            root.render(0, 0, guiGraphics, mouseX, mouseY, partialTick, zOffset)
            zOffset = root.getMaxZ(zOffset) + 10f
        }

        if (asynchronous && hasFrameWaiters) {
            hasFrameWaiters = false
            recomposeJob = composeScope.launch { clock.sendFrame(System.nanoTime()) }
        }
    }

	override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float)
	{
		super.render(guiGraphics, mouseX, mouseY, partialTick)
		renderTooltip(guiGraphics, mouseX, mouseY)
	}

	override fun renderBg(guiGraphics: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int)
	{
        // Keep the menu's screen-offset in sync each frame so that slot coordinates
        // are always relative to leftPos/topPos (which vanilla adds back during item rendering).
        menu.screenLeftPos = leftPos
        menu.screenTopPos  = topPos
		renderNodes(guiGraphics, mouseX, mouseY, partialTick)
	}

    // ── Lifecycle ─────────────────────────────────────────────────────────

    override fun onClose() {
        GLFW.glfwSetCursor(minecraft!!.window.window, GLFW.glfwCreateStandardCursor(GLFW.GLFW_ARROW_CURSOR))
        super.onClose()
        recomposeJob?.cancel("GUI closing")
        recomposer.close()
        snapshotHandle.dispose()
        layerManager.layers.forEach { it.dispose() }
        composeScope.cancel()
    }

    // ── Input ─────────────────────────────────────────────────────────────

    private fun topNode() = layerManager.top?.rootNode

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val top = topNode() ?: return super.mouseClicked(mouseX, mouseY, button)
        processPointerEvent(top, mouseX, mouseY, PointerEventType.GLOBAL_PRESS, global = true)
        val event = processPointerEvent(top, mouseX, mouseY, PointerEventType.PRESS)
        return event.bypassSuper || super.mouseClicked(mouseX, mouseY, button)
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val top = topNode() ?: return super.mouseReleased(mouseX, mouseY, button)
        processPointerEvent(top, mouseX, mouseY, PointerEventType.GLOBAL_RELEASE, global = true)
        val event = processPointerEvent(top, mouseX, mouseY, PointerEventType.RELEASE)
        return event.bypassSuper || super.mouseReleased(mouseX, mouseY, button)
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        val top = topNode() ?: return super.mouseMoved(mouseX, mouseY)
        processPointerEvent(top, mouseX, mouseY, PointerEventType.MOVE)
        processPointerEvent(top, mouseX, mouseY, PointerEventType.ENTER) {
            it.isBounded(mouseX.toInt(), mouseY.toInt()) && !it.isBounded(lastMouseX.toInt(), lastMouseY.toInt())
        }
        processPointerEvent(top, mouseX, mouseY, PointerEventType.EXIT) {
            !it.isBounded(mouseX.toInt(), mouseY.toInt()) && it.isBounded(lastMouseX.toInt(), lastMouseY.toInt())
        }
        lastMouseX = mouseX; lastMouseY = mouseY
        super.mouseMoved(mouseX, mouseY)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        val top = topNode() ?: return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)
        val event = processScrollEvent(top, mouseX, mouseY, scrollX, scrollY, PointerEventType.SCROLL)
        return event.bypassSuper || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, dragX: Double, dragY: Double): Boolean {
        val top = topNode() ?: return super.mouseDragged(mouseX, mouseY, button, dragX, dragY)
        val event = processDragEvent(top, mouseX, mouseY, button, dragX, dragY, PointerEventType.DRAG)
        return event.bypassSuper || super.mouseDragged(mouseX, mouseY, button, dragX, dragY)
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        val top  = topNode() ?: return super.keyPressed(keyCode, scanCode, modifiers)
        val base = layerManager.layers.firstOrNull()?.rootNode
        if (base != null) {
            if (keyCode == InputConstants.KEY_LSHIFT && modifiers == 3) base.debug = !base.debug
            if (base.debug && keyCode == InputConstants.KEY_LSHIFT) base.extraDebug = true
        }
        val event = processKeyEvent(top, keyCode, scanCode, modifiers)
        return event.bypassSuper || super.keyPressed(keyCode, scanCode, modifiers)
    }

    override fun keyReleased(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        val base = layerManager.layers.firstOrNull()?.rootNode
        if (base != null && base.debug && keyCode == InputConstants.KEY_LSHIFT) base.extraDebug = false
        return super.keyReleased(keyCode, scanCode, modifiers)
    }

    override fun charTyped(codePoint: Char, modifiers: Int): Boolean {
        val top = topNode() ?: return super.charTyped(codePoint, modifiers)
        val event = processCharEvent(top, codePoint, modifiers)
        return event.bypassSuper || super.charTyped(codePoint, modifiers)
    }
}