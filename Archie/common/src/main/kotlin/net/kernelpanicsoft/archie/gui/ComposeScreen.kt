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
 * Implemented by Compose-driven screens that recompose asynchronously, so test harnesses can
 * poll for a settled frame (no pending or in-flight recomposition) before asserting on rendered
 * output - e.g. before taking a screenshot right after simulating a click.
 */
internal interface ComposeIdleAware {
    /** `true` when there is no snapshot-write notification, frame request, or recompose job pending. */
    fun isComposeIdle(): Boolean
}

/** Implemented by hosts (screens) that own a [LayerStackManager] for their layer stack. */
interface LayerManagerProvider
{
    /** The layer stack owned by this host. */
    val layerManager: LayerStackManager
}

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
) : Screen(title), CoroutineScope, ComposeIdleAware, LayerManagerProvider {

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

    // Not 0.0 - a node can legitimately sit at the literal origin (e.g. the first item in a
    // top-left-aligned Column), and mouseMoved()'s ENTER condition (`nowBounded && !wasBounded`)
    // would then read the initial "no prior position" sentinel as if the mouse had already been
    // sitting inside that node before any real movement, silently suppressing its very first
    // ENTER event. No real screen coordinate is ever negative, so this can never coincide.
    private var lastMouseX = Double.NEGATIVE_INFINITY
    private var lastMouseY = Double.NEGATIVE_INFINITY

    // `Recomposer.hasPendingWork` is Compose's own atomically-maintained "is there recomposition,
    // apply-changes, or effect work outstanding" signal - the same one Compose's own test tooling
    // (ComposeTestRule.waitForIdle()) uses. Reimplementing this by hand via applyScheduled/
    // hasFrameWaiters/recomposeJob had a real gap: recomposeJob only wraps `clock.sendFrame(...)`,
    // and resuming a dispatched withFrameNanos continuation doesn't block until that continuation's
    // *own* subsequent work (the actual recompose + apply-changes) finishes - it's dispatched, not
    // synchronous. So recomposeJob could complete (and isComposeIdle() report idle) while the
    // Recomposer was still mid-flight applying the very change a test's click()/hover() just
    // triggered, letting an assertion race a stale pre-interaction render. hasPendingWork has no
    // such gap since the Recomposer updates it itself as part of the same state transition.
    override fun isComposeIdle(): Boolean = !recomposer.hasPendingWork

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

        val layersSnapshot = Snapshot.takeMutableSnapshot()
        try {
            layersSnapshot.enter {
                var zOffset = 0f
                for (layer in layerManager.layers) {
                    val root = layer.rootNode
                    root.measure(Constraints(maxWidth = width, maxHeight = height))
                    root.render(0, 0, guiGraphics, mouseX, mouseY, partialTick, zOffset)
                    zOffset = root.getMaxZ(zOffset) + 10f
                }
            }
            layersSnapshot.apply().check()
        } finally {
            layersSnapshot.dispose()
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

    private var composeDisposed = false

    override fun onClose() {
        GLFW.glfwSetCursor(minecraft!!.window.window, 0L)
        super.onClose()
        disposeCompose()
    }

    // vanilla's Minecraft.setScreen() calls removed() on the *old* screen for every screen
    // transition - including a caller directly swapping to a new screen, which never goes
    // through onClose() at all (that only fires when a screen closes itself, e.g. Escape).
    // Without this override, every such swap - including the GameTest harness moving from one
    // test's screen straight to the next's - leaked this screen's entire Compose runtime
    // (Recomposer, composeScope and all its coroutines) running forever in the background.
    // Confirmed the mechanism (not yet reproduced standalone): a CI-only crash deep inside
    // Compose's own SlotTable/Recomposer internals surfaced as a suppressed exception logged
    // between two unrelated, otherwise-passing tests - consistent with a leaked prior screen's
    // recomposer still running concurrently against Compose-runtime state a newer screen's
    // recomposition is also touching.
    override fun removed() {
        super.removed()
        disposeCompose()
    }

    private fun disposeCompose() {
        if (composeDisposed) return
        composeDisposed = true
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
