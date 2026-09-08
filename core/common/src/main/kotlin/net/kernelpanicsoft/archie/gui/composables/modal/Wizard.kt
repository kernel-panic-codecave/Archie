package net.kernelpanicsoft.archie.gui.composables.modal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import net.kernelpanicsoft.archie.gui.animation.AnimationSpec
import net.kernelpanicsoft.archie.gui.animation.animatePulse
import net.kernelpanicsoft.archie.gui.composables.basic.Text
import net.kernelpanicsoft.archie.gui.composables.containers.Surface
import net.kernelpanicsoft.archie.gui.composables.input.Button
import net.kernelpanicsoft.archie.gui.layer.ModalScope
import net.kernelpanicsoft.archie.gui.layout.Alignment
import net.kernelpanicsoft.archie.gui.layout.Arrangement
import net.kernelpanicsoft.archie.gui.layout.Column
import net.kernelpanicsoft.archie.gui.layout.Layout
import net.kernelpanicsoft.archie.gui.layout.MeasurePolicy
import net.kernelpanicsoft.archie.gui.layout.MeasureResult
import net.kernelpanicsoft.archie.gui.layout.Renderer
import net.kernelpanicsoft.archie.gui.layout.Row
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.modifiers.clipToBounds
import net.kernelpanicsoft.archie.gui.modifiers.height
import net.kernelpanicsoft.archie.gui.modifiers.position.margin
import net.kernelpanicsoft.archie.gui.modifiers.position.padding
import net.kernelpanicsoft.archie.gui.modifiers.width
import net.kernelpanicsoft.archie.gui.nodes.UINode
import net.kernelpanicsoft.archie.gui.theme.LocalTheme
import net.kernelpanicsoft.archie.gui.util.extension.invoke
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import kotlin.math.roundToInt

/** Default width of a wizard's page viewport, in pixels - nine slots, matching the other containers. */
private const val DEFAULT_CONTENT_WIDTH = 9 * 18

/** Default transition, deliberately brief: a page change is navigation feedback, not an animation to watch. */
private val DEFAULT_SLIDE_SPEC = AnimationSpec()

/**
 * Whether a [WizardPage] will let the wizard move forward, and why not when it won't.
 *
 * Evaluated during composition on every pass, so a page backed by ordinary state (a quantity
 * field, a fetched crafting plan) reports its current answer without having to push validity
 * anywhere itself.
 */
sealed interface WizardValidation {

    /** The page is complete; the forward button is enabled. */
    data object Valid : WizardValidation

    /**
     * The page will not let the wizard advance.
     *
     * @property reason Shown beneath the page when non-null. Worth setting: a disabled button with
     *   no explanation leaves the reader guessing which part of the page is unsatisfied.
     */
    data class Blocked(val reason: Component? = null) : WizardValidation
}

/**
 * One step of a [Wizard].
 *
 * @property id Stable identity for the page, independent of its position - a wizard whose pages
 *   change between compositions is matched on this rather than on index.
 * @property title Shown as the wizard's header while this page is the current one.
 * @property forwardText Overrides the forward button's label for this page. A step that commits
 *   something ("Start Crafting") reads better than a generic Next, and the last page falls back to
 *   the wizard's own finish label rather than Next regardless.
 * @property validate Consulted every composition. Defaults to always-valid, which is right for a
 *   page that only displays.
 * @property content The page body.
 */
data class WizardPage(
    val id: String,
    val title: Component,
    val forwardText: Component? = null,
    val validate: () -> WizardValidation = { WizardValidation.Valid },
    val content: @Composable () -> Unit,
)

/** Restricts the [WizardScope.page] DSL to its own receiver scope. */
@DslMarker
annotation class WizardDsl

/** Receiver scope for the [Wizard] DSL `builder` lambda. */
@WizardDsl
class WizardScope internal constructor() {
    private val pages = mutableListOf<WizardPage>()

    /** Declares a page with the given [id] and [title], in order. */
    fun page(
        id: String,
        title: Component,
        forwardText: Component? = null,
        validate: () -> WizardValidation = { WizardValidation.Valid },
        content: @Composable () -> Unit,
    ) {
        pages += WizardPage(id, title, forwardText, validate, content)
    }

    /** Declares a page from a pre-built [WizardPage]. */
    fun page(page: WizardPage) {
        pages += page
    }

    internal fun reset() = pages.clear()

    internal fun build(): List<WizardPage> = pages.toList()
}

/**
 * Tracks which page of a [Wizard] is showing. Create via [rememberWizardState].
 *
 * Hoisting this is what lets a caller drive navigation itself - skipping a page whose answer is
 * already known, or jumping back to the start after a failed submission - rather than being limited
 * to the built-in buttons.
 */
@Stable
class WizardState internal constructor(initialIndex: Int) {

    /** Index of the page currently showing. */
    var currentIndex by mutableIntStateOf(initialIndex)
        private set

    /**
     * Which way the last move went: `1` forward, `-1` back.
     *
     * The slide has to know this rather than infer it from the indices, because by the time the
     * transition runs the old index is already gone.
     */
    internal var direction by mutableIntStateOf(1)
        private set

    /** Whether there is a page before the current one. */
    val canGoBack: Boolean get() = currentIndex > 0

    /** Whether [currentIndex] is the final page of a wizard with [pageCount] pages. */
    fun isLastPage(pageCount: Int): Boolean = currentIndex >= pageCount - 1

    /** Moves to [index], sliding in the direction the move implies. A move to the current page does nothing. */
    fun moveTo(index: Int) {
        if (index == currentIndex) return
        direction = if (index > currentIndex) 1 else -1
        currentIndex = index
    }

    /** Advances one page, stopping at the last of [pageCount]. */
    fun next(pageCount: Int) {
        if (currentIndex < pageCount - 1) moveTo(currentIndex + 1)
    }

    /** Steps back one page, stopping at the first. */
    fun back() {
        if (canGoBack) moveTo(currentIndex - 1)
    }

    /** Returns to the first page. Worth calling when reopening a wizard whose state outlives one showing. */
    fun reset() {
        direction = -1
        currentIndex = 0
    }

    /**
     * Clamps [currentIndex] into range for a wizard of [pageCount] pages.
     *
     * A caller's page list can shrink between compositions - a conditional step dropping out once
     * its answer is known - and an index left past the end would otherwise read off the list.
     */
    internal fun ensureRange(pageCount: Int) {
        if (pageCount <= 0) return
        if (currentIndex > pageCount - 1) currentIndex = pageCount - 1
        if (currentIndex < 0) currentIndex = 0
    }
}

/** Creates and remembers a [WizardState] starting on [initialIndex]. */
@Composable
fun rememberWizardState(initialIndex: Int = 0): WizardState = remember { WizardState(initialIndex) }

/**
 * Bookkeeping for one page change: which page is leaving, and the progress to draw the slide at.
 *
 * Exists as a testable unit because getting it wrong is silent - the pages simply cut from one to
 * the next and nothing reports a fault. Two details both have to hold, and the first version had
 * neither:
 *
 * 1. The outgoing page has to survive the composition that *starts* the transition. Clearing it
 *    based on the animation's progress cleared it immediately, because on that first pass the
 *    animation has not restarted yet and still reads as finished - so only one page was ever
 *    composed and there was nothing to slide against.
 * 2. That same staleness would draw the first frame at the animation's *end* position. Treating
 *    the pass as progress `0` until the sweep really restarts avoids a one-frame jump to the
 *    finished layout and back.
 */
@Stable
internal class WizardTransition(initialIndex: Int) {

    /** The page currently being navigated to. */
    var shownIndex by mutableIntStateOf(initialIndex)
        private set

    /** The page being left, or `null` when settled on one page. */
    var outgoingIndex by mutableStateOf<Int?>(null)
        private set

    /** Set from the composition a page change is seen until the animation has actually restarted. */
    private var awaitingSweep by mutableStateOf(false)

    /** What the animation read when the change was seen - the baseline a restart has to drop below. */
    private var progressAtChange by mutableFloatStateOf(1f)

    /**
     * Folds one composition's worth of state in and returns the progress to draw at.
     *
     * @param rawProgress The animation's own current value, which lags a page change by one pass.
     */
    fun advance(currentIndex: Int, pageIndices: IntRange, rawProgress: Float): Float {
        if (shownIndex != currentIndex) {
            outgoingIndex = shownIndex.takeIf { it in pageIndices }
            shownIndex = currentIndex
            awaitingSweep = true
            progressAtChange = rawProgress
        }
        // The sweep has restarted once its value drops below what it read when the change was seen,
        // since it jumps straight back to its start. Testing against 1f instead would mistake a
        // still-running slide's own value for a restarted one, so a page change made mid-slide would
        // draw one frame at the old slide's position before snapping back.
        if (awaitingSweep && (rawProgress < progressAtChange || progressAtChange <= 0f)) awaitingSweep = false

        val progress = if (awaitingSweep) 0f else rawProgress
        if (!awaitingSweep && progress >= 1f) outgoingIndex = null
        return progress
    }
}

/**
 * Where the outgoing and incoming pages sit during a slide, as x offsets from the viewport's left
 * edge.
 *
 * At [progress] `0` the incoming page is exactly one viewport width off-screen and the outgoing one
 * fills the viewport; at `1` they have swapped. [direction] is `1` moving forward, which brings the
 * new page in from the right and pushes the old one off to the left, and `-1` moving back, which
 * mirrors both - the detail most easily got backwards, hence its own function.
 *
 * @return The outgoing page's x, then the incoming page's.
 */
internal fun wizardSlideOffsets(width: Int, progress: Float, direction: Int): Pair<Int, Int> {
    val travelled = (width * progress).roundToInt()
    return -direction * travelled to direction * (width - travelled)
}

/**
 * The page viewport: shows one page, and during a change shows the outgoing and incoming pages
 * side by side, sliding horizontally.
 *
 * Both pages are composed for the length of the transition, so a page that is mid-animation is
 * still a live subtree - which is why this clips input as well as pixels ([Modifier.clipToBounds]).
 * Without that, the half of a page currently sitting outside the viewport would still take clicks
 * from whatever it happens to be sliding over.
 *
 * @param outgoing The page being left, or `null` when settled on one page.
 * @param progress `0` at the start of a transition, `1` once settled.
 * @param direction `1` when moving forward, `-1` when moving back.
 */
@Composable
private fun WizardViewport(
    progress: Float,
    direction: Int,
    modifier: Modifier = Modifier,
    outgoing: (@Composable () -> Unit)?,
    incoming: @Composable () -> Unit,
) {
    // Not remembered: it closes over `progress`, which changes every frame of the transition, and a
    // remembered policy would keep placing pages at the offset the animation started with.
    val measurePolicy = MeasurePolicy { _, measurables, constraints ->
        val pages = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0, maxHeight = Int.MAX_VALUE)) }
        val width = if (constraints.maxWidth != Int.MAX_VALUE) constraints.maxWidth else (pages.maxOfOrNull { it.width } ?: 0)
        // The taller of the two pages, so the frame does not jolt to a new height partway through a
        // slide and then settle back.
        val height = pages.maxOfOrNull { it.height } ?: 0

        MeasureResult(width, height) {
            if (pages.size >= 2) {
                val (outgoingX, incomingX) = wizardSlideOffsets(width, progress, direction)
                pages[0].placeAt(outgoingX, 0)
                pages[1].placeAt(incomingX, 0)
            } else {
                pages.firstOrNull()?.placeAt(0, 0)
            }
        }
    }

    Layout(
        name = "WizardViewport",
        measurePolicy = measurePolicy,
        renderer = object : Renderer {
            override fun render(node: UINode, x: Int, y: Int, guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) = guiGraphics {
                enableScissor(x, y, x + node.width, y + node.height)
            }

            override fun renderAfterChildren(node: UINode, x: Int, y: Int, guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) = guiGraphics {
                disableScissor()
            }
        },
        modifier = Modifier.clipToBounds().then(modifier),
    ) {
        // Composition order is load-bearing: the measure policy places child 0 as the outgoing page.
        outgoing?.invoke()
        incoming()
    }
}

/**
 * A multi-page dialog body: one page at a time, with Back/Cancel/forward navigation, a horizontal
 * slide between pages, and per-page validation gating the forward button.
 *
 * Suited to a flow whose later steps depend on earlier answers - asking for a quantity, then showing
 * what that quantity actually entails and letting the reader commit or go back and change it. A
 * single scrolling form is the better shape when the steps are independent.
 *
 * The forward button reads [nextText] on every page but the last, where it becomes [finishText] and
 * runs [onFinish]; either can be overridden per page via [WizardPage.forwardText]. It is disabled
 * while the current page reports [WizardValidation.Blocked], whose reason is shown beneath the page.
 *
 * @param pages The steps, in order. Rendering nothing is the correct response to an empty list.
 * @param contentHeight Fixes the viewport height. Left `null`, the wizard is as tall as its current
 *   page, so the frame resizes as pages change; worth setting when the pages differ a lot in height.
 * @param onCancel Invoked by the Cancel button. Dismissing the containing modal is the caller's to do.
 * @param onFinish Invoked by the forward button on the last page, only while that page validates.
 */
@Composable
fun Wizard(
    pages: List<WizardPage>,
    state: WizardState = rememberWizardState(),
    modifier: Modifier = Modifier,
    contentWidth: Int = DEFAULT_CONTENT_WIDTH,
    contentHeight: Int? = null,
    backText: Component = Component.literal("Back"),
    cancelText: Component = Component.literal("Cancel"),
    nextText: Component = Component.literal("Next"),
    finishText: Component = Component.literal("Finish"),
    transitionSpec: AnimationSpec = DEFAULT_SLIDE_SPEC,
    onCancel: () -> Unit = {},
    onFinish: () -> Unit = {},
) {
    if (pages.isEmpty()) return
    state.ensureRange(pages.size)

    val theme = LocalTheme.current
    val page = pages[state.currentIndex]
    val validation = page.validate()
    val isLast = state.isLastPage(pages.size)

    // Sweeps 0 -> 1 on each page change and holds at 1 otherwise, including on first composition -
    // so a freshly opened wizard shows its first page settled rather than sliding in from nowhere.
    val rawProgress = animatePulse(key = state.currentIndex, from = 0f, to = 1f, spec = transitionSpec)
    val transition = remember { WizardTransition(state.currentIndex) }
    val progress = transition.advance(state.currentIndex, pages.indices, rawProgress)
    val outgoingIndex = transition.outgoingIndex

    Column(verticalArrangement = Arrangement.spacedBy(4), horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Text(text = page.title, color = theme.darkTextColor, dropShadow = false)

        val viewportModifier = Modifier.width(contentWidth).let { if (contentHeight != null) it.height(contentHeight) else it }
        WizardViewport(
            progress = progress,
            direction = state.direction,
            modifier = viewportModifier,
            outgoing = outgoingIndex?.let { index -> { pages[index].content() } },
            incoming = page.content,
        )

        // Occupies its row only when there is something to say, so a valid page does not carry a
        // blank line where the explanation would go.
        (validation as? WizardValidation.Blocked)?.reason?.let { reason ->
            Text(text = reason, color = theme.darkTextColor, dropShadow = false)
        }

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.margin(top = 4).width(contentWidth),
        ) {
            Button(onClick = { state.back() }, enabled = state.canGoBack) {
                Text(backText, dropShadow = false)
            }
            Button(onClick = { onCancel() }) {
                Text(cancelText, dropShadow = false)
            }
            Button(
                onClick = { if (isLast) onFinish() else state.next(pages.size) },
                enabled = validation is WizardValidation.Valid,
            ) {
                Text(page.forwardText ?: if (isLast) finishText else nextText, dropShadow = false)
            }
        }
    }
}

/**
 * DSL overload declaring pages inline via [WizardScope.page] rather than building a list.
 *
 * @param builder Declares the pages, in order.
 */
@Composable
fun Wizard(
    modifier: Modifier = Modifier,
    state: WizardState? = null,
    contentWidth: Int = DEFAULT_CONTENT_WIDTH,
    contentHeight: Int? = null,
    backText: Component = Component.literal("Back"),
    cancelText: Component = Component.literal("Cancel"),
    nextText: Component = Component.literal("Next"),
    finishText: Component = Component.literal("Finish"),
    transitionSpec: AnimationSpec = DEFAULT_SLIDE_SPEC,
    onCancel: () -> Unit = {},
    onFinish: () -> Unit = {},
    builder: WizardScope.() -> Unit,
) {
    val scope = remember { WizardScope() }
    scope.reset()
    scope.builder()

    Wizard(
        pages = scope.build(),
        state = state ?: rememberWizardState(),
        modifier = modifier,
        contentWidth = contentWidth,
        contentHeight = contentHeight,
        backText = backText,
        cancelText = cancelText,
        nextText = nextText,
        finishText = finishText,
        transitionSpec = transitionSpec,
        onCancel = onCancel,
        onFinish = onFinish,
    )
}

/**
 * A [Wizard] in a modal's [Surface], dismissing the modal on cancel and after finishing.
 *
 * The counterpart to [ConfirmDialog] for a flow that needs more than one step. Push it with
 * [net.kernelpanicsoft.archie.gui.layer.LayerStackManager.wizardDialog].
 */
@Composable
fun ModalScope.WizardDialog(
    pages: List<WizardPage>,
    state: WizardState = rememberWizardState(),
    contentWidth: Int = DEFAULT_CONTENT_WIDTH,
    contentHeight: Int? = null,
    backText: Component = Component.literal("Back"),
    cancelText: Component = Component.literal("Cancel"),
    nextText: Component = Component.literal("Next"),
    finishText: Component = Component.literal("Finish"),
    transitionSpec: AnimationSpec = DEFAULT_SLIDE_SPEC,
    onCancel: () -> Unit = {},
    onFinish: () -> Unit = {},
) {
    // Padding on the Surface rather than margin on the content, matching ModalDialogScaffold.
    Surface(modifier = Modifier.padding(4)) {
        Wizard(
            pages = pages,
            state = state,
            contentWidth = contentWidth,
            contentHeight = contentHeight,
            backText = backText,
            cancelText = cancelText,
            nextText = nextText,
            finishText = finishText,
            transitionSpec = transitionSpec,
            onCancel = {
                onCancel()
                dismiss()
            },
            onFinish = {
                onFinish()
                dismiss()
            },
        )
    }
}
