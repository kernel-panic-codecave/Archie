package net.kernelpanicsoft.archie.gui.composables.containers

import androidx.compose.runtime.*
import net.kernelpanicsoft.archie.gui.layout.*
import net.kernelpanicsoft.archie.gui.modifiers.Constraints
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.modifiers.input.*
import net.kernelpanicsoft.archie.gui.nodes.AUINode
import net.kernelpanicsoft.archie.gui.util.KColor
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.util.Mth
import org.joml.Vector4f
import org.lwjgl.glfw.GLFW
import kotlin.math.max
import kotlin.math.roundToInt

private const val SCROLLBAR_THICKNESS    = 4
private const val SCROLL_SENSITIVITY     = 15.0
private const val SCROLLBAR_FADE_DURATION_MS = 1000L
private const val MIN_SCROLLBAR_THUMB_SIZE   = 10

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
                state.childSize = direction.choose(placeable.width.toDouble(), placeable.height.toDouble()).toInt()
                state.containerSize = direction.choose(constraints.maxWidth.toDouble(), constraints.maxHeight.toDouble()).toInt()
                state.maxScroll = max(0, state.childSize - state.containerSize)
                state.scrollOffset = state.scrollOffset.coerceIn(0.0, state.maxScroll.toDouble())

                return MeasureResult(constraints.maxWidth, constraints.maxHeight) {
                    val scrollPos = state.currentScrollPosition.roundToInt()
                    if (direction == ScrollDirection.VERTICAL) placeable.placeAt(0, -scrollPos)
                    else placeable.placeAt(-scrollPos, 0)
                }
            }
        }
    }

    Layout(
        name = "Scrollable",
        measurePolicy = measurePolicy,
        renderer = object : Renderer {
            override fun render(node: AUINode, x: Int, y: Int, guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
                guiGraphics.enableScissor(x, y, x + node.width, y + node.height)
            }

            override fun renderAfterChildren(node: AUINode, x: Int, y: Int, guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
                state.currentScrollPosition += (state.scrollOffset - state.currentScrollPosition) * 0.4 * partialTick

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
            .onScroll<AUINode> { _, event ->
                state.scrollBy(-event.scrollY * SCROLL_SENSITIVITY)
                event.consume()
            }
            .onPointerEvent<AUINode>(PointerEventType.PRESS) { node, event ->
                val scrollbarBounds = if (direction == ScrollDirection.VERTICAL)
                    Vector4f((node.x + node.width - SCROLLBAR_THICKNESS).toFloat(), node.y.toFloat(), (node.x + node.width).toFloat(), (node.y + node.height).toFloat())
                else
                    Vector4f(node.x.toFloat(), (node.y + node.height - SCROLLBAR_THICKNESS).toFloat(), (node.x + node.width).toFloat(), (node.y + node.height).toFloat())

                if (event.mouseX >= scrollbarBounds.x && event.mouseX <= scrollbarBounds.z
                    && event.mouseY >= scrollbarBounds.y && event.mouseY <= scrollbarBounds.w) {
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
