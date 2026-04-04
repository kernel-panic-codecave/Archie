package net.kernelpanicsoft.archie.gui

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.Snapshot
import com.mojang.blaze3d.platform.InputConstants
import kotlinx.coroutines.*
import net.kernelpanicsoft.archie.gui.layer.LayerStackManager
import net.kernelpanicsoft.archie.gui.layer.LocalLayerManager
import net.kernelpanicsoft.archie.gui.layout.*
import net.kernelpanicsoft.archie.gui.modifiers.Constraints
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.modifiers.fillMaxSize
import net.kernelpanicsoft.archie.gui.modifiers.input.PointerEventType
import net.kernelpanicsoft.archie.gui.util.extension.processCharEvent
import net.kernelpanicsoft.archie.gui.util.extension.processDragEvent
import net.kernelpanicsoft.archie.gui.util.extension.processKeyEvent
import net.kernelpanicsoft.archie.gui.util.extension.processPointerEvent
import net.kernelpanicsoft.archie.gui.util.extension.processScrollEvent
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW
import kotlin.coroutines.CoroutineContext

/** Provides the current [ComposeScreen] to any composable in its tree. */
val LocalScreen: ProvidableCompositionLocal<ComposeScreen> =
    compositionLocalOf { throw IllegalStateException("Screen has not been provided") }

/**
 * A Compose-driven Minecraft [Screen] base class with layer support, async recomposition,
 * and full pointer/keyboard input dispatch.
 *
 * Extend this class and call [start] inside your `init()` override:
 *
 * ```kotlin
 * class MyScreen : ComposeScreen(Component.literal("My Screen")) {
 *     override fun init() {
 *         super.init()
 *         start {
 *             Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
 *                 Text(Component.literal("Hello!"))
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * @param title        The screen title passed to the vanilla [Screen] constructor.
 * @param asynchronous When `true` (default), recomposition runs off the main thread and
 *   the result is joined at the start of the next frame for smooth, non-blocking updates.
 *   Set to `false` to force synchronous recomposition (simpler but may stutter).
 */
abstract class ComposeScreen(
    title: Component,
    val asynchronous: Boolean = true,
) : Screen(title), CoroutineScope {

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
                LocalScreen provides this,
                LocalLayerManager provides layerManager,
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
        setInitialFocus()
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(guiGraphics, mouseX, mouseY, partialTick)
        renderNodes(guiGraphics, mouseX, mouseY, partialTick)
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────

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
        processScrollEvent(top, mouseX, mouseY, scrollX, scrollY, PointerEventType.GLOBAL_SCROLL, global = true)
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
