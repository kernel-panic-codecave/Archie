package net.kernelpanicsoft.archie.gui.composables.containers

import androidx.compose.runtime.*
import net.kernelpanicsoft.archie.gui.LocalSlotClipBounds
import net.kernelpanicsoft.archie.gui.SlotClipSource
import net.kernelpanicsoft.archie.gui.layout.*
import net.kernelpanicsoft.archie.gui.modifiers.Constraints
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.modifiers.onGloballyPositioned
import net.kernelpanicsoft.archie.gui.modifiers.onSizeChanged
import net.kernelpanicsoft.archie.gui.modifiers.input.*
import net.kernelpanicsoft.archie.gui.nodes.AUINode
import net.kernelpanicsoft.archie.gui.util.KColor
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.util.Mth
import org.lwjgl.glfw.GLFW
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

private const val SCROLLBAR_THICKNESS    = 4
private const val SCROLL_SENSITIVITY     = 15.0
private const val SCROLLBAR_FADE_DURATION_MS = 1000L
private const val MIN_SCROLLBAR_THUMB_SIZE   = 10
private const val SCROLL_SNAP_EPSILON = 0.1

/**
 * The axis along which a [Scrollable] container scrolls its content.
 */
enum class ScrollDirection {
    VERTICAL, HORIZONTAL;

    /** Returns [horizontal] or [vertical] depending on the direction. */
    fun choose(horizontal: Double, vertical: Double): Double =
        if (this == VERTICAL) vertical else horizontal
}

/**
 * Mutable state holder for a [Scrollable] composable.
 *
 * Create and remember an instance via [rememberScrollableState] and pass it to [Scrollable]
 * when you need programmatic control over the scroll position.
 */
@Stable
class ScrollableState {
    /** Current target scroll offset in pixels. Animate towards [currentScrollPosition]. */
    var scrollOffset by mutableStateOf(0.0)
    /** Smoothly interpolated scroll position used for actual rendering. */
    var currentScrollPosition by mutableStateOf(0.0)
    /** Maximum scroll offset (content size − container size). */
    var maxScroll by mutableStateOf(0)
    /** Size of the scrollable content in the scroll axis, in pixels. */
    var childSize by mutableStateOf(0)
    /** Size of the visible container in the scroll axis, in pixels. */
    var containerSize by mutableStateOf(0)
    /** Whether the user is currently dragging the scrollbar thumb. */
    var isDraggingScrollbar by mutableStateOf(false)
    /** Timestamp of the last user interaction (used for fade-out animation). */
    var lastInteractTime by mutableStateOf(0L)

    /** Records an interaction so the scrollbar fade-out timer resets. */
    fun onInteraction() { lastInteractTime = System.currentTimeMillis() }

    /**
     * Scrolls by [delta] pixels, clamping the result to the valid range.
     *
     * @param delta Positive values scroll forward (down/right); negative scrolls back.
     */
    fun scrollBy(delta: Double) {
        scrollOffset = (scrollOffset + delta).coerceIn(0.0, maxScroll.toDouble())
        onInteraction()
    }
}

/**
 * Creates and remembers a [ScrollableState] for use with [Scrollable].
 */
@Composable
fun rememberScrollableState(): ScrollableState = remember { ScrollableState() }

/**
 * A container that allows its single child to be scrolled when the child's content
 * exceeds the container's bounds.
 *
 * A fade-in/out scrollbar thumb is rendered automatically when content overflows. The
 * scrollbar supports mouse-drag interaction and responds to the keyboard arrow keys,
 * Page Up/Down.
 *
 * ### Example
 * ```kotlin
 * Scrollable(modifier = Modifier.size(200, 100)) {
 *     Column {
 *         repeat(20) { Text(Component.literal("Item $it")) }
 *     }
 * }
 * ```
 *
 * @param direction      The [ScrollDirection] (vertical or horizontal).
 * @param scrollbarColor The fill colour of the scrollbar thumb.
 * @param modifier       Modifiers applied to the Scrollable layout node.
 * @param state          External [ScrollableState]; defaults to a locally remembered instance.
 * @param content        The single scrollable child composable.
 */
@Composable
fun Scrollable(
    direction: ScrollDirection = ScrollDirection.VERTICAL,
    scrollbarColor: KColor = KColor.DARK_GRAY,
    modifier: Modifier = Modifier,
    state: ScrollableState = rememberScrollableState(),
    content: @Composable () -> Unit,
) {
    val clipSource = remember { SlotClipSource() }

    val measurePolicy = remember(direction) {
        object : MeasurePolicy {
            override fun measure(
                scope: MeasureScope,
                measurables: List<Measurable>,
                constraints: Constraints,
            ): MeasureResult {
                if (measurables.isEmpty()) return MeasureResult(constraints.minWidth, constraints.minHeight) {}

                val contentConstraints = if (direction == ScrollDirection.VERTICAL)
                    constraints.copy(minHeight = 0, maxHeight = Int.MAX_VALUE)
                else
                    constraints.copy(minWidth = 0, maxWidth = Int.MAX_VALUE)

                val placeable = measurables.first().measure(contentConstraints)

                val resolvedWidth = if (direction == ScrollDirection.HORIZONTAL) {
                    resolveScrollableViewportAxis(placeable.width, constraints.minWidth, constraints.maxWidth)
                } else {
                    resolveScrollableContentAxis(placeable.width, constraints.minWidth, constraints.maxWidth)
                }

                val resolvedHeight = if (direction == ScrollDirection.VERTICAL) {
                    resolveScrollableViewportAxis(placeable.height, constraints.minHeight, constraints.maxHeight)
                } else {
                    resolveScrollableContentAxis(placeable.height, constraints.minHeight, constraints.maxHeight)
                }

                state.childSize = direction.choose(placeable.width.toDouble(), placeable.height.toDouble()).toInt()
                state.containerSize = direction.choose(resolvedWidth.toDouble(), resolvedHeight.toDouble()).toInt()
                state.maxScroll = max(0, state.childSize - state.containerSize)
                state.scrollOffset = state.scrollOffset.coerceIn(0.0, state.maxScroll.toDouble())

                return MeasureResult(resolvedWidth, resolvedHeight) {
                    val scrollPos = state.currentScrollPosition.roundToInt()
                    if (direction == ScrollDirection.VERTICAL) placeable.placeAt(0, -scrollPos)
                    else placeable.placeAt(-scrollPos, 0)
                }
            }
        }
    }

    CompositionLocalProvider(LocalSlotClipBounds provides clipSource) {
        Layout(
            name = "Scrollable",
            measurePolicy = measurePolicy,
            renderer = object : Renderer {
            override fun render(node: AUINode, x: Int, y: Int, guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
                guiGraphics.enableScissor(x, y, x + node.width, y + node.height)
            }

            override fun renderAfterChildren(node: AUINode, x: Int, y: Int, guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
                val lerpFactor = (0.4f * partialTick).coerceIn(0.05f, 1f).toDouble()
                val next = state.currentScrollPosition + (state.scrollOffset - state.currentScrollPosition) * lerpFactor
                state.currentScrollPosition = if (abs(state.scrollOffset - next) <= SCROLL_SNAP_EPSILON) state.scrollOffset else next

                if (state.maxScroll > 0) {
                    val timeSinceInteract = System.currentTimeMillis() - state.lastInteractTime
                    if (!(timeSinceInteract > SCROLLBAR_FADE_DURATION_MS && !state.isDraggingScrollbar)) {
                        val fadeAlpha = if (state.isDraggingScrollbar) 1f else 1f - (timeSinceInteract.toFloat() / SCROLLBAR_FADE_DURATION_MS)
                        val alpha = Mth.clamp((fadeAlpha * scrollbarColor.alpha).toInt(), 0, 255)
                        if (alpha > 0) {
                            val colorWithAlpha = scrollbarColor.rgb or (alpha shl 24)
                            val trackSize = state.containerSize
                            val thumbSize = max(MIN_SCROLLBAR_THUMB_SIZE, (trackSize.toFloat() / state.childSize * trackSize).toInt())
                            val scrollPct = if (state.maxScroll > 0) state.currentScrollPosition / state.maxScroll else 0.0
                            val thumbPos = scrollPct * (trackSize - thumbSize)

                            if (direction == ScrollDirection.VERTICAL) {
                                val tx = x + node.width - SCROLLBAR_THICKNESS
                                val ty = y + thumbPos.roundToInt()
                                guiGraphics.fill(tx, ty, tx + SCROLLBAR_THICKNESS, ty + thumbSize, colorWithAlpha)
                            } else {
                                val tx = x + thumbPos.roundToInt()
                                val ty = y + node.height - SCROLLBAR_THICKNESS
                                guiGraphics.fill(tx, ty, tx + thumbSize, ty + SCROLLBAR_THICKNESS, colorWithAlpha)
                            }
                        }
                    }
                }
                guiGraphics.disableScissor()
            }
            },
            modifier = modifier
            .onGloballyPositioned { coords ->
                clipSource.updateOrigin(coords)
            }
            .onSizeChanged { size ->
                clipSource.updateSize(size)
            }
            .onScroll<AUINode> { _, event ->
                state.scrollBy(-event.scrollY * SCROLL_SENSITIVITY)
                event.consume()
            }
            .onPointerEvent<AUINode>(PointerEventType.PRESS) { node, event ->
                val minX = if (direction == ScrollDirection.VERTICAL) node.x + node.width - SCROLLBAR_THICKNESS else node.x
                val minY = if (direction == ScrollDirection.VERTICAL) node.y else node.y + node.height - SCROLLBAR_THICKNESS
                val maxX = node.x + node.width
                val maxY = node.y + node.height

                if (event.mouseX >= minX && event.mouseX <= maxX && event.mouseY >= minY && event.mouseY <= maxY) {
                    state.isDraggingScrollbar = true
                    state.onInteraction()
                    event.consume()
                }
            }
            .onPointerEvent<AUINode>(PointerEventType.GLOBAL_RELEASE) { _, _ -> state.isDraggingScrollbar = false }
            .onDrag<AUINode> { _, event ->
                if (!state.isDraggingScrollbar) return@onDrag
                val pixelDelta = direction.choose(event.dragX, event.dragY)
                val trackSize = state.containerSize
                val thumbSize = max(MIN_SCROLLBAR_THUMB_SIZE, (trackSize.toFloat() / state.childSize * trackSize).toInt())
                if (trackSize > thumbSize) {
                    state.scrollBy(pixelDelta * (state.maxScroll.toFloat() / (trackSize - thumbSize)))
                    // Keep drag feedback immediate while preserving smoothing for wheel/key input.
                    state.currentScrollPosition = state.scrollOffset
                }
                event.consume()
            }
            .onKeyEvent { _, event ->
                val amount = state.containerSize * 0.8
                when (event.keyCode) {
                    GLFW.GLFW_KEY_DOWN   -> if (direction == ScrollDirection.VERTICAL)   state.scrollBy(SCROLL_SENSITIVITY)
                    GLFW.GLFW_KEY_UP     -> if (direction == ScrollDirection.VERTICAL)   state.scrollBy(-SCROLL_SENSITIVITY)
                    GLFW.GLFW_KEY_RIGHT  -> if (direction == ScrollDirection.HORIZONTAL) state.scrollBy(SCROLL_SENSITIVITY)
                    GLFW.GLFW_KEY_LEFT   -> if (direction == ScrollDirection.HORIZONTAL) state.scrollBy(-SCROLL_SENSITIVITY)
                    GLFW.GLFW_KEY_PAGE_DOWN -> state.scrollBy(amount)
                    GLFW.GLFW_KEY_PAGE_UP   -> state.scrollBy(-amount)
                    else -> return@onKeyEvent
                }
                event.consume()
            },
            content = content,
        )
    }
}

internal fun resolveScrollableViewportAxis(childSize: Int, min: Int, max: Int): Int {
    // For the scrolling axis, fill the available finite viewport so overflow can scroll.
    if (max == Int.MAX_VALUE) return childSize.coerceAtLeast(min)
    return max.coerceAtLeast(min)
}

internal fun resolveScrollableContentAxis(childSize: Int, min: Int, max: Int): Int {
    if (max == Int.MAX_VALUE) return childSize.coerceAtLeast(min)
    return childSize.coerceIn(min, max)
}

