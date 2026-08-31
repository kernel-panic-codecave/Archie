package net.kernelpanicsoft.archie.gui

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.snapshots.ObserverHandle
import androidx.compose.runtime.snapshots.Snapshot
import com.mojang.blaze3d.platform.InputConstants
import kotlinx.coroutines.*
import net.kernelpanicsoft.archie.gui.access.SlotHighlightClipProvider
import net.kernelpanicsoft.archie.gui.access.SlotLayerDepthProvider
import net.kernelpanicsoft.archie.gui.blockentity.LocalBlockEntityState
import net.kernelpanicsoft.archie.gui.composables.containers.RootContainer
import net.kernelpanicsoft.archie.gui.focus.collectFocusableChildren
import net.kernelpanicsoft.archie.gui.item.ComposeItemContainerMenu
import net.kernelpanicsoft.archie.gui.item.LocalItemState
import net.kernelpanicsoft.archie.gui.layer.Layer
import net.kernelpanicsoft.archie.gui.layer.LayerStackManager
import net.kernelpanicsoft.archie.gui.layer.LocalLayerManager
import net.kernelpanicsoft.archie.gui.layer.LocalLayerManagerOrNull
import net.kernelpanicsoft.archie.gui.layout.IntCoordinates
import net.kernelpanicsoft.archie.gui.layout.IntRect
import net.kernelpanicsoft.archie.gui.layout.LayoutNode
import net.kernelpanicsoft.archie.gui.layout.pos
import net.kernelpanicsoft.archie.gui.modifiers.Constraints
import net.kernelpanicsoft.archie.gui.modifiers.input.PointerEventType
import net.kernelpanicsoft.archie.gui.theme.LocalTheme
import net.kernelpanicsoft.archie.gui.util.extension.invoke
import net.kernelpanicsoft.archie.gui.util.extension.pose
import net.kernelpanicsoft.archie.gui.util.extension.processCharEvent
import net.kernelpanicsoft.archie.gui.util.extension.processDragEvent
import net.kernelpanicsoft.archie.gui.util.extension.processKeyEvent
import net.kernelpanicsoft.archie.gui.util.extension.processPointerEvent
import net.kernelpanicsoft.archie.gui.util.extension.processScrollEvent
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.Slot
import org.lwjgl.glfw.GLFW
import kotlin.coroutines.CoroutineContext

/** Provides the current [ComposeContainerScreen] to any composable in its tree. */
val LocalContainerScreen: ProvidableCompositionLocal<ComposeContainerScreen<*>> =
    compositionLocalOf { throw IllegalStateException("Screen has not been provided") }

/** Provides the current [ComposeContainerMenuBase] to any composable in its tree. */
val LocalContainerMenu: ProvidableCompositionLocal<ComposeContainerMenuBase<*>> =
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
 * [ComposeScreen]. Works uniformly for both [ComposeBlockContainerMenu] (BlockEntity-backed) and
 * [ComposeItemContainerMenu] (ItemStack-backed) subclasses - nothing here is holder-specific.
 *
 * @param T The concrete [ComposeContainerMenuBase] subclass driving this screen.
 * @param menu The container menu instance for this screen.
 * @param playerInventory The opening player's inventory.
 * @param title The screen title passed to the vanilla [AbstractContainerScreen] constructor.
 * @param asynchronous When `true` (default), recomposition runs off the main thread and
 *   the result is joined at the start of the next frame for smooth, non-blocking updates.
 *   Set to `false` to force synchronous recomposition (simpler but may stutter).
 */
abstract class ComposeContainerScreen<T : ComposeContainerMenuBase<T>>(
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

        /**
         * Wide enough that a layer's own *highest* real Z push - not just its base - never reaches
         * into the next layer's own band. The tallest thing any one layer draws is a slot's own
         * decorations: [SLOT_LAYER_OFFSET] to clear that layer's own compose content, plus vanilla's
         * own hardcoded `translate(0, 0, 200)` inside `GuiGraphics.renderItemDecorations` (relative
         * to whatever pose is already active, i.e. stacking on top of [SLOT_LAYER_OFFSET], not
         * resetting it) for the count-text/durability-bar overlay - confirmed against the decompiled
         * source, not just vanilla's own visual convention. A 200-wide step left `120 + 200 = 320`
         * overshooting a 200-wide band by 120, which is exactly what let a base layer's own item
         * decorations draw *above* an open modal (defeating its own dim overlay) instead of safely
         * beneath it.
         */
        private const val LAYER_Z_STEP = 500f
        private const val SLOT_LAYER_OFFSET = 120f

        /**
         * [ComposeContainerScreen.renderLabels]'s own Z offset above [layerBaseZ]`(0)` - high enough
         * to clear every other base-layer element (crucially, [SLOT_LAYER_OFFSET] *and* vanilla's own
         * further `+200` for slot decorations, both explained on [LAYER_Z_STEP]) that would otherwise
         * paint over the title/inventory label, while staying under [layerBaseZ]`(1)` so an open modal
         * - which starts painting there - still correctly covers/dims the label rather than the label
         * poking through on top of it.
         */
        private const val LABEL_LAYER_OFFSET = 450f

        /**
         * Extra room past a slot's own 16x16 icon box that [slotClipRect] leaves unclipped, for
         * vanilla's own stack-count/durability-bar decorations - those (and the count text's drop
         * shadow, offset a further 1px down-right) are drawn slightly past that box's bottom-right
         * corner and were never meant to be clipped to it. Only the *group* clip (a [net.kernelpanicsoft.archie.gui.composables.containers.Scrollable]
         * viewport boundary) needs to stay pixel-tight to the icon box itself.
         */
        private const val SLOT_DECORATION_MARGIN = 2

        /** The base Z offset used when rendering the layer at [layerDepth], deepest layers on top. */
        fun layerBaseZ(layerDepth: Int): Float = BASE_LAYER_Z + layerDepth * LAYER_Z_STEP
    }


    private var hasFrameWaiters = false
    private lateinit var clock: BroadcastFrameClock

    // See ComposeScreen's identical fields for why this is captured once and untyped against
    // kotlinx-coroutines-test.
    private val testPump = ComposeTestClockOverride.pump

    // A fresh CoroutineScope/Job every [start] call (including a restart - see [init]'s own KDoc),
    // never the one a prior [disposeCompose] already cancelled: a cancelled Job can never launch
    // further children, so reusing it would silently make every coroutine start() launches
    // (including the recomposer's own) instantly-cancelled no-ops instead of actually restarting
    // anything. A computed [coroutineContext] getter (not a `val` snapshotting it once) means
    // every consumer - including this class's own [CoroutineScope] delegation - always sees
    // whichever scope is currently live.
    private lateinit var composeScope: CoroutineScope
    final override val coroutineContext: CoroutineContext get() = composeScope.coroutineContext

    final override lateinit var layerManager: LayerStackManager
        private set
    private lateinit var recomposer: Recomposer
    private var recomposeJob: Job? = null

    private var applyScheduled = false
    private lateinit var snapshotHandle: ObserverHandle

    /** The content passed to the most recent [start] call - retained so [init] can restart Compose with it after a prior [removed] disposed it. */
    private var contentFn: (@Composable () -> Unit)? = null

    private var lastMouseX = 0.0
    private var lastMouseY = 0.0

    /** The layer [render] last ran [setInitialFocus] for - see its use there. */
    private var lastTopLayer: Layer? = null

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
     * Initialises (or re-initialises - see [init]) the Compose runtime and pushes the base layer
     * with [content].
     *
     * Must be called once from a subclass's own construction. Also called automatically from
     * [init] to restart Compose after a prior [removed] disposed it.
     *
     * @param content The root composable content for this screen.
     */
    protected fun start(content: @Composable () -> Unit) {
        contentFn = content
        composeDisposed = false
        hasFrameWaiters = false
        applyScheduled = false
        clock = BroadcastFrameClock { hasFrameWaiters = true }
        composeScope = CoroutineScope(ComposeTestClockOverride.dispatcher ?: Dispatchers.Default) + clock
        snapshotHandle = Snapshot.registerGlobalWriteObserver {
            if (!applyScheduled) {
                applyScheduled = true
                composeScope.launch {
                    applyScheduled = false
                    Snapshot.sendApplyNotifications()
                }
            }
        }

        recomposer = Recomposer(coroutineContext)
        layerManager = LayerStackManager(recomposer) { layerContent ->
            // Applied to every layer this screen ever pushes (base, modal, dropdown, tooltip
            // alike) - see LayerStackManager's screenLocals doc for why a plain
            // CompositionLocalProvider wrapping only this start() call wouldn't reach them.
            CompositionLocalProvider(
                LocalContainerScreen provides this,
                LocalVanillaScreen provides this,
                LocalContainerMenu provides menu,
                LocalSlotData provides menu.slotData,
                // Only one of these is non-null for any given menu - LocalBlockEntityState /
                // LocalItemState are both nullable-by-default composition locals precisely so
                // composables reaching for the "wrong" one for this menu's holder kind get a
                // clear null rather than a bogus fallback value.
                LocalBlockEntityState provides (menu as? ComposeBlockContainerMenu<*, *>)?.blockEntityState,
                LocalItemState provides (menu as? ComposeItemContainerMenu<*>)?.itemState,
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
            RootContainer {
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
    open fun renderNodes(baseLayer: Boolean, guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        // See ComposeScreen.renderNodes for why this runs first.
        testPump?.invoke()
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
        reconcileHoverState(mouseX.toDouble(), mouseY.toDouble())
        guiGraphics {

            flush()

            pose {
                translate(leftPos.toFloat(), topPos.toFloat(), layerBaseZ(0) + LABEL_LAYER_OFFSET)
                super.renderLabels(guiGraphics, mouseX, mouseY)
            }
            flush()

            if (layerManager.layers.size > 1)
            {
                renderNodes(false, guiGraphics, mouseX, mouseY, partialTick)
                flush()
            }
            pose {
                translate(0f, 0f, layerBaseZ(layerManager.layers.size - 1))
                renderTooltip(guiGraphics, mouseX, mouseY)
            }
            // See ComposeScreen.renderNodes for why this only runs when the top layer actually
            // changed (a modal opening or closing), not every frame.
            if (layerManager.top !== lastTopLayer)
            {
                lastTopLayer = layerManager.top
                clearFocus()
                setInitialFocus()
            }
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
     * No-op here - vanilla's own [AbstractContainerScreen.render] calls this sandwiched between
     * the slot loop and the floating dragged-item render, with `RenderSystem.disableDepthTest()`
     * active for that whole span, so a plain call here can't reliably win against a slot's own
     * icon/decorations. The real call moves to [render] instead, pinned to the *base* layer's own
     * Z-band ([layerBaseZ]`(0) + `[LABEL_LAYER_OFFSET]) - high enough to clear the base layer's
     * own content (slots and their decorations included) so nothing there paints over the label -
     * and drawn *before* this screen's own modal/dropdown layers ([renderNodes]) get their own
     * turn, not after, so an open modal covers/dims the label the same ordinary way it covers a
     * slot icon, through draw order, rather than the label needing to out-rank whatever Z the
     * modal's own content happens to render at.
     */
    override fun renderLabels(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int) {
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
        val slotRect = IntRect(slotMinX, slotMinY, slotMinX + 16 + SLOT_DECORATION_MARGIN, slotMinY + 16 + SLOT_DECORATION_MARGIN)

        return effectiveClip.intersect(slotRect)
    }

    private var composeDisposed = false

    /**
     * Restarts the Compose runtime with [contentFn] if a prior [removed] disposed it - vanilla's
     * `Minecraft.setScreen()` calls [removed] on this screen for *any* swap-away, including one a
     * caller intends to reverse later by handing this exact instance back to `setScreen()` again
     * (a recipe viewer's "view recipe" navigation, say), not just a genuine close. Without this,
     * such a screen never renders its Compose content again once reactivated - only vanilla's own
     * slot/title rendering (untouched by any of this) keeps working, which is what actually
     * surfaces the bug: the screen looks blank apart from its slots after returning from a recipe
     * lookup.
     *
     * [added] rather than the no-arg `init()` hook - `Screen.init(Minecraft, int, int)` only calls
     * that hook down a much more conditional path (gated by vanilla's own `initialized` flag *and*,
     * on NeoForge, a cancellable `ScreenEvent.Init.Pre`), so it doesn't reliably fire on every
     * reactivation. `Minecraft.setScreen()` calls [added] unconditionally on every activation,
     * fresh construction and reactivation alike - confirmed against the decompiled source, not
     * assumed. A no-op on the very first [added] (right after construction), since [composeDisposed]
     * starts `false` until a real [removed] call has actually happened.
     */
    override fun added() {
        super.added()
        if (composeDisposed) contentFn?.let { start(it) }
    }

    override fun onClose() {
        GLFW.glfwSetCursor(minecraft!!.window.window, 0L)
        super.onClose()
        disposeCompose()
    }

    // See ComposeScreen.removed()'s doc comment - vanilla's Minecraft.setScreen() calls removed()
    // on the *old* screen for every transition, not just onClose()'s explicit-close path, and
    // without this override this screen's entire Compose runtime leaked on any such swap.
    override fun removed() {
        super.removed()
        disposeCompose()
    }

    override fun resize(minecraft: Minecraft, width: Int, height: Int)
    {
        super.resize(minecraft, width, height)
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

    private fun getTopNode(): LayoutNode? = layerManager.top?.rootNode

    // See ComposeScreen.children() for why this one override is enough to bridge Compose's
    // `Modifier.focusable` nodes into vanilla's Tab/Shift-Tab/arrow-key navigation and any
    // other GuiEventListener-walking consumer (e.g. Controlify).
    override fun children(): List<GuiEventListener> {
        val topNode = getTopNode() ?: return super.children()
        return collectFocusableChildren(topNode)
    }

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
        reconcileHoverState(mouseX, mouseY)
        super.mouseMoved(mouseX, mouseY)
    }

    /**
     * Fires ENTER/EXIT for whatever [getTopNode] children actually changed hover state between
     * [lastMouseX]/[lastMouseY] and [mouseX]/[mouseY] - shared by [mouseMoved] (the normal,
     * discrete-event path) and [render] (called every frame regardless of whether a `mouseMoved`
     * event ever fires). The per-frame call from [render] matters because a cursor moving straight
     * from over a node to outside the game window entirely fires no further `mouseMoved` - there's
     * nothing left inside the window to move *to* - which otherwise leaves that node's own hover
     * state (and anything driven by it, e.g. a tooltip or slot highlight) stuck indefinitely,
     * self-correcting only once the cursor re-enters and triggers a real `mouseMoved` again. See
     * [ComposeScreen]'s identical [ComposeScreen.reconcileHoverState] for the non-container case.
     */
    private fun reconcileHoverState(mouseX: Double, mouseY: Double) {
        val topNode = getTopNode() ?: return
        if (mouseX == lastMouseX && mouseY == lastMouseY) return
        processPointerEvent(topNode, mouseX, mouseY, PointerEventType.ENTER) {
            it.isBounded(mouseX.toInt(), mouseY.toInt()) && !it.isBounded(lastMouseX.toInt(), lastMouseY.toInt())
        }
        processPointerEvent(topNode, mouseX, mouseY, PointerEventType.EXIT) {
            !it.isBounded(mouseX.toInt(), mouseY.toInt()) && it.isBounded(lastMouseX.toInt(), lastMouseY.toInt())
        }
        lastMouseX = mouseX
        lastMouseY = mouseY
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