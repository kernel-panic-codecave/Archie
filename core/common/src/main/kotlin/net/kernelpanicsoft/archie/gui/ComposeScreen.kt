package net.kernelpanicsoft.archie.gui

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.Snapshot
import com.mojang.blaze3d.platform.InputConstants
import kotlinx.coroutines.*
import net.kernelpanicsoft.archie.gui.focus.collectFocusableChildren
import net.kernelpanicsoft.archie.gui.layer.Layer
import net.kernelpanicsoft.archie.gui.layer.LayerStackManager
import net.kernelpanicsoft.archie.gui.layer.LocalLayerManager
import net.kernelpanicsoft.archie.gui.layer.LocalLayerManagerOrNull
import net.kernelpanicsoft.archie.gui.layout.*
import net.kernelpanicsoft.archie.gui.theme.LocalTheme
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
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW
import kotlin.coroutines.CoroutineContext

/** Provides the current [ComposeScreen] to any composable in its tree. */
val LocalScreen: ProvidableCompositionLocal<ComposeScreen> =
    compositionLocalOf { throw IllegalStateException("Screen has not been provided") }

/**
 * Provides the hosting vanilla [Screen], regardless of whether it's a [ComposeScreen] or a
 * [ComposeContainerScreen] - unlike [LocalScreen]/`LocalContainerScreen`, which are mutually
 * exclusive depending on the screen type, this is provided by both.
 *
 * `Screen.setFocused`/`getFocused`/`clearFocus` are all public vanilla API (unlike
 * `setInitialFocus`/`changeFocus`, which are `protected`), so composables that need to register
 * or release vanilla keyboard/controller focus explicitly - e.g. a pointer-driven text field
 * syncing its local focus state back to vanilla, so Tab navigation and a modal opening over it
 * both stay consistent with what's actually focused - can reach them through this without
 * needing a reference to the concrete screen subclass.
 */
val LocalVanillaScreen: ProvidableCompositionLocal<Screen> =
    compositionLocalOf { throw IllegalStateException("Screen has not been provided") }

/**
 * Implemented by Compose-driven screens that recompose asynchronously, so test harnesses can
 * poll for a settled frame (no pending or in-flight recomposition) before asserting on rendered
 * output - e.g. before taking a screenshot right after simulating a click.
 */
interface ComposeIdleAware {
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
 * Test-only override, installed by the client GameTest harness (`AClientGameTestHarness.kt`),
 * that swaps [ComposeScreen]'s real-time coroutine dispatcher for a virtual one so a composable's
 * `delay(...)` (e.g. a dialog's close animation) resolves deterministically against a scheduler
 * the harness drives itself, instead of racing real wall-clock time, real thread scheduling, and
 * the harness's tick-based polling - the same class of flakiness Jetpack Compose's own test
 * tooling (`ComposeTestRule`/`runComposeUiTest`) avoids by construction, backing composition with
 * a virtual clock/dispatcher that `waitForIdle()` drives forward instead of polling real
 * concurrency. `null` unless a client GameTest explicitly installs one; the running game never
 * touches this.
 *
 * Deliberately untyped against `kotlinx-coroutines-test` (`CoroutineDispatcher` is a
 * kotlinx-coroutines-core type; `pump` is a plain lambda) - that dependency is dev/test-only
 * (`compileOnly` in `common`, `runtimeLibrary` - present for local runs, never bundled - in the
 * loader modules; see `common/build.gradle.kts`). [ComposeScreen] is loaded by every screen in
 * the mod, so its own class file must never reference a symbol that isn't resolvable in a real
 * player's game; only `AClientGameTestHarness`'s method bodies (never invoked outside
 * `AGameTestPlatform.isGameTest`) construct the actual `StandardTestDispatcher`/
 * `TestCoroutineScheduler` instances installed here.
 */
object ComposeTestClockOverride {
    /** The dispatcher to back new [ComposeScreen]s' coroutine scope with, in place of [kotlinx.coroutines.Dispatchers.Default]. */
    @Volatile
    var dispatcher: CoroutineDispatcher? = null

    /**
     * Called once per real rendered frame by every live [ComposeScreen] while installed - drains
     * all outstanding virtual-time work (recomposition, effects, `delay(...)`) synchronously on
     * the render thread. Set alongside [dispatcher] to `{ scheduler.advanceUntilIdle() }`.
     */
    @Volatile
    var pump: (() -> Unit)? = null
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

    // Captured once at construction (not read live from ComposeTestClockOverride elsewhere) so
    // this screen keeps working consistently even if a later test installs/clears the override
    // while this screen is still disposing.
    private val testPump = ComposeTestClockOverride.pump

    private val composeScope = CoroutineScope(ComposeTestClockOverride.dispatcher ?: Dispatchers.Default) + clock
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

    /** The layer [renderNodes] last ran [setInitialFocus] for - see its use there. */
    private var lastTopLayer: Layer? = null

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
        layerManager = LayerStackManager(recomposer) { layerContent ->
            // Applied to every layer this screen ever pushes (base, modal, dropdown, tooltip
            // alike) - see LayerStackManager's screenLocals doc for why a plain
            // CompositionLocalProvider wrapping only this start() call wouldn't reach them.
            CompositionLocalProvider(
                LocalScreen provides this,
                LocalVanillaScreen provides this,
                LocalLayerManager provides layerManager,
                LocalLayerManagerOrNull provides layerManager,
            ) {
                // Re-supplies whatever Theme{} is currently mounted in the base layer (see
                // LayerStackManager.rootTheme) so a later-pushed layer isn't stuck with
                // LocalTheme's own default - it's a separate top-level composition, so it'd
                // never otherwise see a Theme{} that only wraps the base layer's own content.
                val theme = layerManager.rootTheme
                if (theme != null) {
                    CompositionLocalProvider(LocalTheme provides theme) { layerContent() }
                } else {
                    layerContent()
                }
            }
        }

        AUIScopeManager.scopes += composeScope
        launch { recomposer.runRecomposeAndApplyChanges() }

        layerManager.push { _ ->
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                content()
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
        // Under a client GameTest's virtual dispatcher (see ComposeTestClockOverride), nothing
        // launched on composeScope runs on its own - it only progresses when the scheduler backing
        // it is advanced. Doing that here, once per real rendered frame, deterministically flushes
        // recomposition/effects/delay() before the join below, instead of racing real threads.
        testPump?.invoke()
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

        // setInitialFocus() re-runs vanilla's own Tab-navigation search (nextFocusPath) to find
        // something to focus, which visits whatever's already focused first - calling it every
        // frame while nothing changed would auto-advance focus to the next candidate each frame
        // (indistinguishable, to vanilla, from a real Tab press) whenever the keyboard was the
        // last input type. Only re-run it when the top layer actually changed - a modal opening
        // or closing - and clear the old focus first, since a modal opening on top otherwise
        // leaves the base screen's element (now hidden behind it) marked focused indefinitely.
        if (layerManager.top !== lastTopLayer) {
            lastTopLayer = layerManager.top
            clearFocus()
            setInitialFocus()
        }
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        reconcileHoverState(mouseX.toDouble(), mouseY.toDouble())
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

    // Bridges Compose's own focus concept (`Modifier.focusable`) into vanilla's built-in
    // GuiEventListener focus graph. Screen already implements Tab/Shift-Tab/arrow-key
    // navigation, focus tracking (getFocused/setFocused) and ComponentPath-based dispatch
    // entirely in terms of `children()` - overriding just this one method is enough to make
    // all of that (plus anything else that walks GuiEventListener, e.g. Controlify's
    // controller navigation) reach Compose content. Scoped to the top layer only, matching
    // topNode()'s modal-aware input dispatch: a modal's focus stays within the modal.
    override fun children(): List<GuiEventListener> {
        val top = topNode() ?: return super.children()
        return collectFocusableChildren(top)
    }

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
        reconcileHoverState(mouseX, mouseY)
        super.mouseMoved(mouseX, mouseY)
    }

    /**
     * Fires ENTER/EXIT for whatever [topNode] children actually changed hover state between
     * [lastMouseX]/[lastMouseY] and [mouseX]/[mouseY] - shared by [mouseMoved] (the normal,
     * discrete-event path) and [render] (called every frame regardless of whether a `mouseMoved`
     * event ever fires). The per-frame call from [render] matters because a cursor moving straight
     * from over a node to outside the game window entirely fires no further `mouseMoved` - there's
     * nothing left inside the window to move *to* - which otherwise leaves that node's own hover
     * state (and anything driven by it, e.g. a tooltip or slot highlight) stuck indefinitely,
     * self-correcting only once the cursor re-enters and triggers a real `mouseMoved` again.
     */
    private fun reconcileHoverState(mouseX: Double, mouseY: Double) {
        val top = topNode() ?: return
        if (mouseX == lastMouseX && mouseY == lastMouseY) return
        processPointerEvent(top, mouseX, mouseY, PointerEventType.ENTER) {
            it.isBounded(mouseX.toInt(), mouseY.toInt()) && !it.isBounded(lastMouseX.toInt(), lastMouseY.toInt())
        }
        processPointerEvent(top, mouseX, mouseY, PointerEventType.EXIT) {
            !it.isBounded(mouseX.toInt(), mouseY.toInt()) && it.isBounded(lastMouseX.toInt(), lastMouseY.toInt())
        }
        lastMouseX = mouseX; lastMouseY = mouseY
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
