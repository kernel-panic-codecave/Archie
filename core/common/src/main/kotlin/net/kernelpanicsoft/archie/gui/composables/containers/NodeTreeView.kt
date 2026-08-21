package net.kernelpanicsoft.archie.gui.composables.containers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.mojang.blaze3d.systems.RenderSystem
import net.kernelpanicsoft.archie.gui.layout.Layout
import net.kernelpanicsoft.archie.gui.layout.MeasurePolicy
import net.kernelpanicsoft.archie.gui.layout.MeasureResult
import net.kernelpanicsoft.archie.gui.layout.Placeable
import net.kernelpanicsoft.archie.gui.layout.Renderer
import net.kernelpanicsoft.archie.gui.modifiers.Constraints
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.nodes.UINode
import net.kernelpanicsoft.archie.gui.util.extension.invoke
import net.kernelpanicsoft.archie.gui.util.extension.pose
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.RenderType
import net.minecraft.resources.ResourceLocation
import org.joml.AxisAngle4f
import org.joml.Quaternionf
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private const val DEFAULT_COLUMN_GAP = 26
private const val DEFAULT_ROW_GAP = 6
private const val DEFAULT_LINE_THICKNESS = 1
private const val DEFAULT_LINE_COLOR = 0xFF808080.toInt()
private const val ARROWHEAD_SIZE = 3
private const val DOT_LENGTH = 2
private const val DASH_LENGTH = 5
private const val FALLBACK_ROW_HEIGHT = 12
private const val FLOW_PERIOD_MS = 800L
private const val FLOW_DASH_LENGTH = 4
private const val FLOW_GAP_LENGTH = 4
private const val FLOW_PATTERN_PERIOD = FLOW_DASH_LENGTH + FLOW_GAP_LENGTH
private const val WAVE_PERIOD_MS = 1400L
private const val WAVE_SEGMENT_LENGTH = 4
private const val WAVE_AMPLITUDE = 1.5
private const val DETOUR_MARGIN = 10
private const val BLINK_PERIOD_MS = 1200L
private const val BLINK_MIN_ALPHA = 0.35f
private const val DISABLED_GRAY = 0.55f
private const val STUB_ANGLE_OFFSET = 5
private const val SPLINE_CORNER_RADIUS = 8f
private const val SPLINE_CORNER_SAMPLES = 8

/** How [NodeTreeView] draws the connector between a node and its parent. */
enum class ConnectorStyle {
	/** A plain unbroken elbow line - the default. */
	SOLID,

	/** A finely dotted elbow line. */
	DOTTED,

	/** A coarser dashed elbow line. */
	DASHED,

	/** A solid elbow line ending in an arrowhead pointing into the parent. */
	ARROW,

	/** A short, dim, sine-perturbed stub waving toward the other end - "this connection is severed/inactive." Ignores [ConnectorAnimation]. */
	DISCONNECTED,
}

/** How [NodeTreeView] animates the connector between a node and its parent over time. Layered on top of [ConnectorStyle], except [ConnectorStyle.DISCONNECTED], which ignores this. */
enum class ConnectorAnimation {
	/** No animation - a static line. */
	NONE,

	/** A repeating dash pattern marches from child to parent over a dim full-length base line. */
	FLOWING,
}

/** How [NodeTreeView] routes/renders a connector's own path shape. Orthogonal to [ConnectorStyle]/[ConnectorAnimation]. */
enum class ConnectorShape {
	/** A plain axis-aligned elbow, exactly the waypoints [pointsFor] computes. The default. */
	ELBOW,

	/** The same waypoints, with each corner replaced by a circular arc (see [roundOrthogonalPolyline]). */
	SPLINE,
}

/** How [NodeTreeView] animates a node's own rendered box over time, independent of any connector. Tints the node's content directly. */
enum class NodeAnimation {
	/** No overlay - the node renders exactly as `content` draws it. */
	NONE,

	/** A soft highlight pulses in and out - "this is available right now." */
	BLINKING,

	/** A fixed, greyed-out look - "this can't be obtained right now." */
	DISABLED,
}

/** Which end of a connector [ConnectorStyle.ARROW]'s arrowhead points into. Purely cosmetic; doesn't affect layout. */
enum class ConnectorDirection {
	/** Points into the parent - the default. */
	CHILD_TO_PARENT,

	/** Points into the child. */
	PARENT_TO_CHILD,
}

/** Which side of the drawn forest every tree's own root sits on. */
enum class RootAlignment {
	/** Every root sits at the leftmost column, descendants extending rightward. The default. */
	START,

	/** Every root sits at the rightmost column, descendants extending leftward. */
	END,
}

/** One of the four axis-aligned directions an arrowhead ([drawArrowhead]) or elbow leg can point. */
private enum class ArrowDirection { LEFT, RIGHT, UP, DOWN }

/**
 * One node in [NodeTreeView]'s own flattened, positioned layout. [parents] holds every incoming
 * connector source (via [children] or an optional `parents` callback - see [layoutForest]).
 * [depth] is only finalized by [layoutForest]'s own topological pass. [hiddenParentCount] counts
 * declared prerequisites not present anywhere in the forest at all. Deliberately holds nothing
 * about `visible` - that's read fresh every render frame instead, so live state changes are
 * picked up immediately.
 */
private class FlatNode<T>(val node: T) {
	val parents: MutableList<Int> = mutableListOf()
	var depth: Int = 0
	var hiddenParentCount: Int = 0
}

/**
 * Flattens every tree in [roots]' own forest (via [children], plus [parentsOf] when given) into
 * [FlatNode]s in composition order. When [dedupe] is on, a node already visited (by [key]
 * equality) gets a new [FlatNode.parents] entry instead of being remeasured; off, every occurrence
 * gets its own [FlatNode]. [parentsOf] supplies additional incoming edges for a node already
 * discovered via [children]; a declared parent [children] never reaches at all is counted in
 * [FlatNode.hiddenParentCount] instead. `visible` never prunes anything here - a rejected node is
 * still discovered/measured/laid out, so the forest's shape never shifts depending on what's
 * hidden; [NodeTreeView]'s render loop reads visibility fresh every frame instead.
 * [FlatNode.depth] is set to one more than the deepest of a node's own parents' final depths, via
 * a topological pass once the whole graph is known.
 */
private fun <T> layoutForest(roots: List<T>, children: (T) -> List<T>, parentsOf: ((T) -> List<T>)?, dedupe: Boolean, key: (T) -> Any?): List<FlatNode<T>> {
	val flat = mutableListOf<FlatNode<T>>()
	val indexByKey = HashMap<Any?, Int>()

	fun visit(node: T, parentIndex: Int?) {
		val existingIndex = if (dedupe) indexByKey[key(node)] else null
		if (existingIndex != null) {
			if (parentIndex != null && parentIndex !in flat[existingIndex].parents) flat[existingIndex].parents += parentIndex
			return
		}
		val myIndex = flat.size
		flat += FlatNode(node)
		if (dedupe) indexByKey[key(node)] = myIndex
		if (parentIndex != null) flat[myIndex].parents += parentIndex
		for (child in children(node)) visit(child, myIndex)
	}
	for (root in roots) visit(root, null)

	if (parentsOf != null) {
		for (i in flat.indices) {
			for (declaredParent in parentsOf(flat[i].node)) {
				val parentIndex = indexByKey[key(declaredParent)]
				if (parentIndex == null) {
					flat[i].hiddenParentCount++
					continue
				}
				if (parentIndex != i && parentIndex !in flat[i].parents) flat[i].parents += parentIndex
			}
		}
	}

	// childrenOf: the reverse of every node's own now-final `parents` list, needed to walk the
	// graph parent-to-child for the topological sort below (parents alone only lets us walk
	// child-to-parent).
	val childrenOf = List(flat.size) { mutableListOf<Int>() }
	for (i in flat.indices) for (p in flat[i].parents) childrenOf[p] += i

	// Post-order DFS topological sort: a node's own finishing position always lands after every
	// one of its descendants', so reversing finish order yields a valid topological order (parents
	// before children) for any DAG, regardless of which order the discovery walk above happened to
	// reach things in.
	val topoVisited = BooleanArray(flat.size)
	val topoOrder = IntArray(flat.size)
	var topoCursor = flat.size
	fun topoVisit(i: Int) {
		if (topoVisited[i]) return
		topoVisited[i] = true
		for (child in childrenOf[i]) topoVisit(child)
		topoOrder[--topoCursor] = i
	}
	for (i in flat.indices) topoVisit(i)

	for (i in topoOrder) for (p in flat[i].parents) flat[i].depth = maxOf(flat[i].depth, flat[p].depth + 1)

	return flat
}

/**
 * Assigns every [flat] node a row (a "tidy tree" placement): a leaf gets the next sequential row;
 * a node with owned children centers between its first and last owned child's row. Each tree in
 * the forest stacks below the previous one via a shared running row counter.
 */
private fun <T> assignRows(flat: List<FlatNode<T>>, ownedChildren: List<List<Int>>): FloatArray {
	val rows = FloatArray(flat.size)
	var nextLeafRow = 0
	fun visit(index: Int) {
		val kids = ownedChildren[index]
		if (kids.isEmpty()) {
			rows[index] = (nextLeafRow++).toFloat()
			return
		}
		for (kid in kids) visit(kid)
		rows[index] = (rows[kids.first()] + rows[kids.last()]) / 2f
	}
	for (i in flat.indices) if (flat[i].parents.isEmpty()) visit(i)
	return rows
}

/**
 * Fills an axis-aligned segment from ([x1],[y1]) to ([x2],[y2]) [thickness] px wide, broken into
 * dashes for [ConnectorStyle.DOTTED]/[DASHED], or a marching dash pattern for
 * [ConnectorAnimation.FLOWING] (`animPhase` is `0f..1f`, looping).
 */
private fun drawSegment(guiGraphics: GuiGraphics, x1: Int, y1: Int, x2: Int, y2: Int, thickness: Int, color: Int, style: ConnectorStyle, animation: ConnectorAnimation, animPhase: Float) {
	val horizontal = y1 == y2
	if (animation == ConnectorAnimation.FLOWING) {
		drawFlowingSegment(guiGraphics, x1, y1, x2, y2, thickness, color, horizontal, animPhase)
		return
	}

	if (style == ConnectorStyle.SOLID || style == ConnectorStyle.ARROW) {
		if (horizontal) guiGraphics.fill(minOf(x1, x2), y1, maxOf(x1, x2), y1 + thickness, color)
		else guiGraphics.fill(x1, minOf(y1, y2), x1 + thickness, maxOf(y1, y2), color)
		return
	}

	val dashLength = if (style == ConnectorStyle.DOTTED) DOT_LENGTH else DASH_LENGTH
	val length = if (horizontal) abs(x2 - x1) else abs(y2 - y1)
	val dir = if (horizontal) (if (x2 >= x1) 1 else -1) else (if (y2 >= y1) 1 else -1)
	val start = if (horizontal) x1 else y1
	var pos = 0
	while (pos < length) {
		val segStart = start + dir * pos
		val segEnd = start + dir * minOf(pos + dashLength, length)
		if (horizontal) guiGraphics.fill(minOf(segStart, segEnd), y1, maxOf(segStart, segEnd), y1 + thickness, color)
		else guiGraphics.fill(x1, minOf(segStart, segEnd), x1 + thickness, maxOf(segStart, segEnd), color)
		pos += dashLength * 2
	}
}

/**
 * A dim [color] base line the segment's full length, plus a repeating [FLOW_DASH_LENGTH]-wide,
 * [FLOW_GAP_LENGTH]-spaced dash pattern tiled along it, shifted by [animPhase] (`0f..1f`, looping)
 * - [ConnectorAnimation.FLOWING]'s own rendering.
 */
private fun drawFlowingSegment(guiGraphics: GuiGraphics, x1: Int, y1: Int, x2: Int, y2: Int, thickness: Int, color: Int, horizontal: Boolean, animPhase: Float) {
	val dimColor = dimAlpha(color, 0.35f)
	if (horizontal) guiGraphics.fill(minOf(x1, x2), y1, maxOf(x1, x2), y1 + thickness, dimColor)
	else guiGraphics.fill(x1, minOf(y1, y2), x1 + thickness, maxOf(y1, y2), dimColor)

	val length = if (horizontal) abs(x2 - x1) else abs(y2 - y1)
	if (length <= 0) return
	val dir = if (horizontal) (if (x2 >= x1) 1 else -1) else (if (y2 >= y1) 1 else -1)
	val start = if (horizontal) x1 else y1
	// Shifting *toward* increasing pos (toward x2/y2, the arrowhead end) as animPhase advances -
	// `pos = -shift` would instead make each dash's own position decrease with time, drifting
	// visually from x2 back toward x1 regardless of which end the arrow's actually pointing at.
	val shift = (animPhase * FLOW_PATTERN_PERIOD).toInt().mod(FLOW_PATTERN_PERIOD)
	var pos = shift - FLOW_PATTERN_PERIOD
	while (pos < length) {
		val dashStart = maxOf(pos, 0)
		val dashEnd = minOf(pos + FLOW_DASH_LENGTH, length)
		if (dashEnd > dashStart) {
			val segStart = start + dir * dashStart
			val segEnd = start + dir * dashEnd
			if (horizontal) guiGraphics.fill(minOf(segStart, segEnd), y1, maxOf(segStart, segEnd), y1 + thickness, color)
			else guiGraphics.fill(x1, minOf(segStart, segEnd), x1 + thickness, maxOf(segStart, segEnd), color)
		}
		pos += FLOW_PATTERN_PERIOD
	}
}

/** [color] with its own alpha channel scaled by [factor] (`0f..1f`) - [ConnectorAnimation.FLOWING]'s dim base line, [ConnectorStyle.DISCONNECTED]'s own faded look. */
private fun dimAlpha(color: Int, factor: Float): Int {
	val alpha = ((color ushr 24) and 0xFF)
	val dimmed = (alpha * factor).roundToInt().coerceIn(0, 255)
	return (dimmed shl 24) or (color and 0x00FFFFFF)
}

/** Which axis-aligned direction travel from ([x1],[y1]) to ([x2],[y2]) points in - [drawPolyline]'s own way of orienting an [ArrowDirection]/wavy-leg check without the caller needing to say so explicitly. */
private fun directionOf(x1: Int, y1: Int, x2: Int, y2: Int): ArrowDirection = when {
	x2 > x1 -> ArrowDirection.RIGHT
	x2 < x1 -> ArrowDirection.LEFT
	y2 > y1 -> ArrowDirection.DOWN
	else -> ArrowDirection.UP
}

/** ([x],[y]) pulled back by [size] against [direction] - where a leg ending in an arrowhead should actually stop drawing, leaving room for [drawArrowhead] to fill in without the line poking through the point. */
private fun pullBack(x: Int, y: Int, direction: ArrowDirection, size: Int): IntArray = when (direction) {
	ArrowDirection.RIGHT -> intArrayOf(x - size, y)
	ArrowDirection.LEFT -> intArrayOf(x + size, y)
	ArrowDirection.DOWN -> intArrayOf(x, y - size)
	ArrowDirection.UP -> intArrayOf(x, y + size)
}

/** A small triangle whose tip sits at ([tipX],[tipY]), [size] px deep, pointing [direction] - the arrowhead [ConnectorStyle.ARROW] appends at a connector's parent end (whichever direction that leg happens to approach the parent from). */
private fun drawArrowhead(guiGraphics: GuiGraphics, tipX: Int, tipY: Int, size: Int, color: Int, direction: ArrowDirection) {
	for (i in 0 until size) {
		val half = size - i
		when (direction) {
			ArrowDirection.RIGHT -> guiGraphics.fill(tipX - size + i, tipY - half, tipX - size + i + 1, tipY + half, color)
			ArrowDirection.LEFT -> guiGraphics.fill(tipX + size - i - 1, tipY - half, tipX + size - i, tipY + half, color)
			ArrowDirection.DOWN -> guiGraphics.fill(tipX - half, tipY - size + i, tipX + half, tipY - size + i + 1, color)
			ArrowDirection.UP -> guiGraphics.fill(tipX - half, tipY + size - i - 1, tipX + half, tipY + size - i, color)
		}
	}
}

/**
 * A short, dim, sine-perturbed stub of [length] px starting at ([startX],[startY]) and advancing
 * along ([dirX],[dirY]) - the shared look for [ConnectorStyle.DISCONNECTED] and a hidden-parent
 * dead-end. Optional [angleSign] fans multiple stubs from the same edge apart instead of stacking.
 */
private fun drawWavyStub(
	guiGraphics: GuiGraphics,
	startX: Float,
	startY: Float,
	dirX: Float,
	dirY: Float,
	length: Int,
	thickness: Int,
	color: Int,
	animPhase: Float,
	angleSign: Int = 0,
) {
	val dirLen = sqrt(dirX * dirX + dirY * dirY)
	if (dirLen < 0.0001f || length <= 0) return
	val ux = dirX / dirLen
	val uy = dirY / dirLen
	val nx = -uy
	val ny = ux
	val dimColor = dimAlpha(color, 0.5f)
	val t = thickness.toFloat()
	var pos = 0
	while (pos < length) {
		val segEnd = minOf(pos + WAVE_SEGMENT_LENGTH, length)
		val phase = pos.toDouble() / WAVE_SEGMENT_LENGTH
		val wave = (sin(phase * 0.9 - animPhase * 2 * Math.PI) * WAVE_AMPLITUDE).toFloat()
		val ramp = angleSign * STUB_ANGLE_OFFSET * pos / length.toFloat()
		val perp = wave + ramp
		val x1 = startX + ux * pos + nx * perp
		val y1 = startY + uy * pos + ny * perp
		val x2 = startX + ux * segEnd + nx * perp
		val y2 = startY + uy * segEnd + ny * perp
		fillLine(guiGraphics, x1, y1, x2, y2, t, dimColor)
		pos = segEnd
	}
}

/**
 * Draws a connector along [points] (an already-routed axis-aligned path, [points].first() the
 * *child* end, [points].last() the *parent* end - see [NodeTreeView]'s own `pointsFor`) in
 * [style]/[animation]. [ConnectorStyle.DISCONNECTED] is handled at the call site (short
 * [drawWavyStub]s toward the other end). Every other [style] defers to [drawSegment] per leg,
 * with [ConnectorStyle.ARROW]'s arrowhead landing on the *last* leg.
 */
private fun drawPolyline(guiGraphics: GuiGraphics, points: List<IntArray>, thickness: Int, color: Int, style: ConnectorStyle, animation: ConnectorAnimation, animPhase: Float) {
	val segmentCount = points.size - 1
	for (i in 0 until segmentCount) {
		val (x1, y1) = points[i]
		val (x2, y2) = points[i + 1]
		val isLast = i == segmentCount - 1
		if (isLast && style == ConnectorStyle.ARROW) {
			val direction = directionOf(x1, y1, x2, y2)
			val (trimmedX, trimmedY) = pullBack(x2, y2, direction, ARROWHEAD_SIZE)
			drawSegment(guiGraphics, x1, y1, trimmedX, trimmedY, thickness, color, style, animation, animPhase)
			drawArrowhead(guiGraphics, x2, y2, ARROWHEAD_SIZE, color, direction)
		} else {
			drawSegment(guiGraphics, x1, y1, x2, y2, thickness, color, style, animation, animPhase)
		}
	}
}

private operator fun IntArray.component1() = this[0]
private operator fun IntArray.component2() = this[1]
private operator fun FloatArray.component1() = this[0]
private operator fun FloatArray.component2() = this[1]

/**
 * Fills a [thickness]-px-wide quad between ([x1],[y1]) and ([x2],[y2]) at whatever angle they
 * describe - [ConnectorShape.SPLINE]'s thick stroke. Pose-rotates so local +X points along the
 * segment, then fills a horizontal strip via [GuiGraphics.fill]. Canonicalizes which endpoint
 * drives the rotation (always the one pointing into the right half-plane, or straight down if
 * vertical) so the same physical segment covers the same pixels regardless of argument order.
 */
private fun fillLine(guiGraphics: GuiGraphics, x1: Float, y1: Float, x2: Float, y2: Float, thickness: Float, color: Int) {
	var originX = x1
	var originY = y1
	var dx = x2 - x1
	var dy = y2 - y1
	val length = sqrt(dx * dx + dy * dy)
	if (length < 0.0001f) return

	if (dx < 0f || (dx == 0f && dy < 0f)) {
		originX = x2
		originY = y2
		dx = -dx
		dy = -dy
	}

	val t = maxOf(1, thickness.roundToInt())
	val half = t / 2
	val len = maxOf(1, length.roundToInt())
	val angle = atan2(dy, dx)
	guiGraphics {
		pose {
			translate(originX.toDouble(), originY.toDouble(), 0.0)
			mulPose(Quaternionf(AxisAngle4f(angle, 0f, 0f, 1f)))
			fill(0, -half, len, t - half, color)
		}
	}
}

/**
 * Fills a solid, [size]-px-deep triangle whose tip sits at ([tipX],[tipY]) pointing along
 * ([dirX],[dirY]) (any angle, need not be normalized) - [ConnectorShape.SPLINE]'s own arrowhead.
 * Same pose-rotate trick as [fillLine]: translate to the tip, rotate so local +X matches the
 * direction, then reuse the axis-aligned strip [drawArrowhead] already used for [ConnectorShape.ELBOW]
 * (pointing [ArrowDirection.RIGHT] in local space).
 */
private fun fillTriangle(guiGraphics: GuiGraphics, tipX: Float, tipY: Float, dirX: Float, dirY: Float, size: Float, color: Int) {
	val length = sqrt(dirX * dirX + dirY * dirY)
	if (length < 0.0001f) return
	val s = maxOf(1, size.roundToInt())
	val angle = atan2(dirY, dirX)
	guiGraphics {
		pose {
			translate(tipX.toDouble(), tipY.toDouble(), 0.0)
			mulPose(Quaternionf(AxisAngle4f(angle, 0f, 0f, 1f)))
			drawArrowhead(guiGraphics, 0, 0, s, color, ArrowDirection.RIGHT)
		}
	}
}

/**
 * Rounds each sharp corner of an axis-aligned orthogonal polyline [rawPoints] into a circular arc
 * of radius at most [cornerRadius] (clamped to half of each adjacent leg), leaving the first and
 * last legs straight. Consecutive duplicate waypoints are collapsed first.
 */
private fun roundOrthogonalPolyline(rawPoints: List<IntArray>, cornerRadius: Float): List<FloatArray> {
	val points = rawPoints.filterIndexed { i, p -> i == 0 || p[0] != rawPoints[i - 1][0] || p[1] != rawPoints[i - 1][1] }
	if (points.size < 2) return points.map { floatArrayOf(it[0].toFloat(), it[1].toFloat()) }
	if (points.size == 2) {
		return listOf(
			floatArrayOf(points[0][0].toFloat(), points[0][1].toFloat()),
			floatArrayOf(points[1][0].toFloat(), points[1][1].toFloat()),
		)
	}

	val result = mutableListOf<FloatArray>()
	result += floatArrayOf(points[0][0].toFloat(), points[0][1].toFloat())

	for (i in 1 until points.size - 1) {
		val x0 = points[i - 1][0].toFloat(); val y0 = points[i - 1][1].toFloat()
		val x1 = points[i][0].toFloat(); val y1 = points[i][1].toFloat()
		val x2 = points[i + 1][0].toFloat(); val y2 = points[i + 1][1].toFloat()
		val dxIn = x1 - x0
		val dyIn = y1 - y0
		val dxOut = x2 - x1
		val dyOut = y2 - y1
		val lenIn = hypot(dxIn.toDouble(), dyIn.toDouble()).toFloat()
		val lenOut = hypot(dxOut.toDouble(), dyOut.toDouble()).toFloat()
		if (lenIn < 0.0001f || lenOut < 0.0001f) {
			result += floatArrayOf(x1, y1)
			continue
		}
		val r = minOf(cornerRadius, lenIn / 2f, lenOut / 2f)
		if (r < 1f) {
			result += floatArrayOf(x1, y1)
			continue
		}
		val uxIn = dxIn / lenIn
		val uyIn = dyIn / lenIn
		val uxOut = dxOut / lenOut
		val uyOut = dyOut / lenOut

		val cross = uxIn * uyOut - uyIn * uxOut
		if (abs(cross) < 0.0001f) {
			result += floatArrayOf(x1, y1)
			continue
		}

		val ax = x1 - uxIn * r
		val ay = y1 - uyIn * r
		val bx = x1 + uxOut * r
		val by = y1 + uyOut * r

		val px: Float
		val py: Float
		if (cross > 0f) {
			px = -uyIn
			py = uxIn
		} else {
			px = uyIn
			py = -uxIn
		}
		val cx = ax + px * r
		val cy = ay + py * r

		val startAngle = atan2(ay - cy, ax - cx)
		val endAngle = atan2(by - cy, bx - cx)
		var delta = endAngle - startAngle
		while (delta > Math.PI) delta -= (2.0 * Math.PI).toFloat()
		while (delta < -Math.PI) delta += (2.0 * Math.PI).toFloat()
		if (cross > 0f && delta < 0f) delta += (2.0 * Math.PI).toFloat()
		if (cross < 0f && delta > 0f) delta -= (2.0 * Math.PI).toFloat()

		result += floatArrayOf(ax, ay)
		for (s in 1 until SPLINE_CORNER_SAMPLES) {
			val t = s.toFloat() / SPLINE_CORNER_SAMPLES
			val a = startAngle + delta * t
			result += floatArrayOf(cx + cos(a) * r, cy + sin(a) * r)
		}
		result += floatArrayOf(bx, by)
	}

	val last = points.last()
	result += floatArrayOf(last[0].toFloat(), last[1].toFloat())
	return result
}

/** [points]' own cumulative arc length up to and including each point, `[0] == 0f` - parallels [points] index-for-index, what every dash/wave walker in [drawSplinePolyline] measures its own on/off or perpendicular-offset phase against, instead of a single axis-aligned coordinate the way [drawSegment]'s own family does for a plain elbow leg. */
private fun cumulativeLengths(points: List<FloatArray>): FloatArray {
	val lengths = FloatArray(points.size)
	for (i in 1 until points.size) {
		val (x1, y1) = points[i - 1]
		val (x2, y2) = points[i]
		lengths[i] = lengths[i - 1] + hypot((x2 - x1).toDouble(), (y2 - y1).toDouble()).toFloat()
	}
	return lengths
}

/**
 * [dense] (with [lengths], its own [cumulativeLengths]) cut off at arc length [maxLength] - every
 * point up to the last one still short of it, plus one new point interpolated onto the segment
 * [maxLength] falls inside. [ConnectorShape.SPLINE]'s own equivalent of [pullBack] for
 * [ConnectorShape.ELBOW]'s arrowhead.
 */
private fun clipToLength(dense: List<FloatArray>, lengths: FloatArray, maxLength: Float): Pair<List<FloatArray>, FloatArray> {
	if (maxLength >= lengths.last()) return dense to lengths
	var i = 0
	while (i < lengths.size - 2 && lengths[i + 1] < maxLength) i++
	val segStart = lengths[i]
	val segEnd = lengths[i + 1]
	val segLen = segEnd - segStart
	val segT = if (segLen < 0.0001f) 0f else ((maxLength - segStart) / segLen).coerceIn(0f, 1f)
	val (x0, y0) = dense[i]
	val (x1, y1) = dense[i + 1]
	val cutPoint = floatArrayOf(x0 + (x1 - x0) * segT, y0 + (y1 - y0) * segT)
	return (dense.subList(0, i + 1) + listOf(cutPoint)) to (lengths.copyOfRange(0, i + 1) + floatArrayOf(maxLength))
}

/**
 * Fills a [thickness]-px square centered on ([x],[y]) - [drawSplinePolyline]'s own joint filler,
 * covering the small gap two adjacent [fillLine] quads meeting at an angle otherwise leave at
 * their shared vertex.
 */
private fun fillJointDot(guiGraphics: GuiGraphics, x: Float, y: Float, thickness: Float, color: Int) {
	val t = maxOf(1, thickness.roundToInt())
	val half = t / 2
	val cx = x.roundToInt()
	val cy = y.roundToInt()
	guiGraphics.fill(cx - half, cy - half, cx + (t - half), cy + (t - half), color)
}

/**
 * Draws the portion of [dense] (with [lengths], its own [cumulativeLengths]) falling within arc
 * length [rangeStart]..[rangeEnd], plus a [fillJointDot] at every interior vertex strictly inside
 * that range - [drawSplinePolyline]'s own shared primitive for both a static
 * [ConnectorStyle.DOTTED]/[DASHED] dash and one instant of [ConnectorAnimation.FLOWING]'s
 * traveling one.
 */
private fun drawRange(guiGraphics: GuiGraphics, dense: List<FloatArray>, lengths: FloatArray, rangeStart: Float, rangeEnd: Float, thickness: Float, color: Int) {
	if (rangeEnd <= rangeStart) return
	for (i in 0 until dense.size - 1) {
		val segStart = lengths[i]
		val segEnd = lengths[i + 1]
		if (segEnd <= rangeStart) continue
		if (segStart >= rangeEnd) break
		val segLen = segEnd - segStart
		val clipStart = maxOf(segStart, rangeStart)
		val clipEnd = minOf(segEnd, rangeEnd)
		val tStart = if (segLen < 0.0001f) 0f else (clipStart - segStart) / segLen
		val tEnd = if (segLen < 0.0001f) 1f else (clipEnd - segStart) / segLen
		val (x0, y0) = dense[i]
		val (x1, y1) = dense[i + 1]
		val ax = x0 + (x1 - x0) * tStart
		val ay = y0 + (y1 - y0) * tStart
		val bx = x0 + (x1 - x0) * tEnd
		val by = y0 + (y1 - y0) * tEnd
		fillLine(guiGraphics, ax, ay, bx, by, thickness, color)
		if (i > 0 && segStart > rangeStart && segStart < rangeEnd) fillJointDot(guiGraphics, x0, y0, thickness, color)
	}
}

/**
 * [ConnectorShape.SPLINE]'s own counterpart to [drawPolyline] - same [points]/[style]/[animation]
 * contract, but first runs [points] through [roundOrthogonalPolyline] and draws the resulting
 * dense curve via [fillLine]/[fillJointDot]/[drawRange]/[fillTriangle]. [ConnectorStyle.DOTTED]/
 * [DASHED]/[ConnectorAnimation.FLOWING]'s pattern is tiled via [drawRange] along the curve's arc
 * length rather than decided per dense segment. [ConnectorStyle.DISCONNECTED] is handled at the
 * call site (short [drawWavyStub]s).
 */
private fun drawSplinePolyline(guiGraphics: GuiGraphics, points: List<IntArray>, thickness: Int, color: Int, style: ConnectorStyle, animation: ConnectorAnimation, animPhase: Float, cornerRadius: Float) {
	val fullDense = roundOrthogonalPolyline(points, cornerRadius)
	val fullLengths = cumulativeLengths(fullDense)
	val t = thickness.toFloat()

	val dashLength = when (style) {
		ConnectorStyle.DOTTED -> DOT_LENGTH.toFloat()
		ConnectorStyle.DASHED -> DASH_LENGTH.toFloat()
		else -> null
	}
	val bodyLength = if (style == ConnectorStyle.ARROW) (fullLengths.last() - ARROWHEAD_SIZE).coerceAtLeast(0f) else fullLengths.last()
	val (dense, lengths) = clipToLength(fullDense, fullLengths, bodyLength)
	val total = lengths.last()

	if (animation == ConnectorAnimation.FLOWING) {
		val dimColor = dimAlpha(color, 0.35f)
		for (i in 0 until dense.size - 1) {
			val (x1, y1) = dense[i]
			val (x2, y2) = dense[i + 1]
			fillLine(guiGraphics, x1, y1, x2, y2, t, dimColor)
			if (i > 0) fillJointDot(guiGraphics, x1, y1, t, dimColor)
		}
		val shift = animPhase * FLOW_PATTERN_PERIOD
		var dashStart = shift.mod(FLOW_PATTERN_PERIOD.toFloat()) - FLOW_PATTERN_PERIOD
		while (dashStart < total) {
			drawRange(guiGraphics, dense, lengths, dashStart.coerceAtLeast(0f), (dashStart + FLOW_DASH_LENGTH).coerceAtMost(total), t, color)
			dashStart += FLOW_PATTERN_PERIOD
		}
	} else if (dashLength != null) {
		var dashStart = 0f
		var on = true
		while (dashStart < total) {
			val dashEnd = (dashStart + dashLength).coerceAtMost(total)
			if (on) drawRange(guiGraphics, dense, lengths, dashStart, dashEnd, t, color)
			dashStart = dashEnd
			on = !on
		}
	} else {
		drawRange(guiGraphics, dense, lengths, 0f, total, t, color)
	}

	if (style == ConnectorStyle.ARROW) {
		val (tipX, tipY) = fullDense.last()
		val (prevX, prevY) = dense.last()
		fillTriangle(guiGraphics, tipX, tipY, tipX - prevX, tipY - prevY, ARROWHEAD_SIZE.toFloat(), color)
	}
}

/**
 * The axis-aligned waypoints connecting [childEdgeX]/[childCenterY] to [parentEdgeX]/[parentCenterY]
 * - each already the node's own edge facing the other. Ordered child-then-parent, since the
 * connector always draws that direction by default. Adjacent columns
 * (`abs(childDepth - parentDepth) <= 1`) get a plain three-leg elbow; anything further apart
 * routes around via a detour lane at [detourClearanceY] (the [DETOUR_MARGIN]-padded bottom edge of
 * whatever sits in the columns being crossed) instead of cutting straight through them.
 */
private fun pointsFor(childEdgeX: Int, childCenterY: Int, childDepth: Int, parentEdgeX: Int, parentCenterY: Int, parentDepth: Int, halfGap: Int, detourClearanceY: Int): List<IntArray> {
	val towardParent = if (parentEdgeX >= childEdgeX) 1 else -1

	if (abs(childDepth - parentDepth) <= 1) {
		val midX = parentEdgeX - towardParent * halfGap
		return listOf(
			intArrayOf(childEdgeX, childCenterY),
			intArrayOf(midX, childCenterY),
			intArrayOf(midX, parentCenterY),
			intArrayOf(parentEdgeX, parentCenterY),
		)
	}

	val detourDownX = childEdgeX + towardParent * halfGap
	val detourUpX = parentEdgeX - towardParent * halfGap
	val detourY = detourClearanceY + DETOUR_MARGIN
	return listOf(
		intArrayOf(childEdgeX, childCenterY),
		intArrayOf(detourDownX, childCenterY),
		intArrayOf(detourDownX, detourY),
		intArrayOf(detourUpX, detourY),
		intArrayOf(detourUpX, parentCenterY),
		intArrayOf(parentEdgeX, parentCenterY),
	)
}

/**
 * The packed ARGB tint [ZoomedBox] should wrap a node's own content with (via [Tinted]), for
 * [animation] - computed fresh every frame, not memoized. [NodeAnimation.DISABLED] desaturates
 * toward gray at full opacity; [NodeAnimation.BLINKING] pulses alpha instead, full color kept.
 */
private fun tintFor(animation: NodeAnimation): Int = when (animation) {
	NodeAnimation.NONE -> -1
	NodeAnimation.DISABLED -> {
		val gray = (DISABLED_GRAY * 255).roundToInt().coerceIn(0, 255)
		(0xFF shl 24) or (gray shl 16) or (gray shl 8) or gray
	}
	NodeAnimation.BLINKING -> {
		val phase = ((System.currentTimeMillis() % BLINK_PERIOD_MS).toFloat() / BLINK_PERIOD_MS)
		val pulse = (sin(phase * 2 * Math.PI) * 0.5 + 0.5).toFloat()
		val alpha = BLINK_MIN_ALPHA + (1f - BLINK_MIN_ALPHA) * pulse
		((alpha * 255).roundToInt().coerceIn(0, 255) shl 24) or 0xFFFFFF
	}
}

/**
 * A small wrapper reporting its own measured size as [zoom]× its single child's natural size, then
 * GPU-scaling that child's rendering to fill the larger box - [NodeTreeView]'s own per-node zoom.
 * Wraps [content] in [Tinted] for [animation] ([tintFor]); [visible] false forces the tint fully
 * transparent while [content] still measures/lays out at its real size.
 */
@Composable
private fun ZoomedBox(zoom: Float, animation: NodeAnimation, visible: Boolean, content: @Composable () -> Unit) {
	val measurePolicy = remember(zoom) {
		MeasurePolicy { _, measurables, _ ->
			val placeable = measurables.firstOrNull()?.measure(Constraints(minWidth = 0, maxWidth = Int.MAX_VALUE, minHeight = 0, maxHeight = Int.MAX_VALUE))
			val naturalWidth = placeable?.width ?: 0
			val naturalHeight = placeable?.height ?: 0
			MeasureResult((naturalWidth * zoom).roundToInt(), (naturalHeight * zoom).roundToInt()) {
				placeable?.placeAt(0, 0)
			}
		}
	}
	Layout(
		name = "ZoomedBox",
		measurePolicy = measurePolicy,
		renderer = object : Renderer {
			override fun render(node: UINode, x: Int, y: Int, guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
				if (zoom != 1f) {
					guiGraphics.pose().pushPose()
					guiGraphics.pose().translate(x.toDouble(), y.toDouble(), 0.0)
					guiGraphics.pose().scale(zoom, zoom, 1f)
					guiGraphics.pose().translate(-x.toDouble(), -y.toDouble(), 0.0)
				}
			}

			override fun renderAfterChildren(node: UINode, x: Int, y: Int, guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
				if (zoom != 1f) {
					guiGraphics.pose().popPose()
				}
			}
		},
		content = { Tinted(tint = { if (visible) tintFor(animation) else 0 }) { content() } },
	)
}

/**
 * A pannable, zoomable, node-based forest view - one or more independent trees ([roots], plural),
 * one column per depth, tidily packed rows connected by elbow connectors (see [assignRows]).
 * [children] extracts a node's own children; [content] draws one node's own visual content at its
 * measured natural size. Connectors draw (and, for [ConnectorStyle.ARROW]/
 * [ConnectorAnimation.FLOWING], animate) child-to-parent by default - see [connectorDirection] to
 * flip that - and route around, never behind, whatever else sits between their two ends (see
 * [pointsFor]). Draws a genuine tree by default (a node reachable through more than one parent is
 * drawn once per parent); pass `dedupe = true` (with [key]) to collapse repeats into one visual
 * node with multiple incoming connectors instead.
 *
 * @param parents An optional second way to declare an edge, alongside [children] - a caller whose
 *   own domain model tracks both directions can declare a node's extra prerequisites from that
 *   node itself. Only adds connectors between nodes [children] already reached from [roots]; a
 *   declared parent it never reaches draws as a short dead-end stub instead of being dropped.
 * @param connectorStyle Chooses a [ConnectorStyle] per node, keyed by the child end of its own
 *   connector to its parent. Overridden by [ConnectorStyle.DISCONNECTED] whenever either end is
 *   rejected by [visible].
 * @param connectorColor/[connectorThickness] Per-node color/thickness for that node's connector.
 * @param connectorAnimation Per-node [ConnectorAnimation], layered on top of [connectorStyle].
 * @param connectorShape Per-node [ConnectorShape] - the connector's path shape, orthogonal to
 *   [connectorStyle]/[connectorAnimation].
 * @param connectorDirection Per-node [ConnectorDirection] - which end the arrowhead/flow lands on.
 * @param nodeAnimation Per-node [NodeAnimation] tinting that node's own `content` directly.
 * @param rootAlignment Which side of the drawn forest every tree's root sits on.
 * @param visible A node this rejects is still fully discovered/measured/laid out, so the forest's
 *   shape never shifts depending on what's hidden - see [layoutForest]. Its `content` never draws,
 *   and every connector touching it draws as [ConnectorStyle.DISCONNECTED] instead.
 * @param onVisibilityChanged Called whenever a node already admitted by [visible] scrolls into or
 *   out of the current pan/zoom viewport - a screen-space fact, unrelated to tree membership.
 * @param state Exposed so a caller can reset/center the pan/zoom itself.
 * @param backgroundParallax/[backgroundTint]/[panelTexture]/[panelVariant]/[panelContentPadding]
 *   Forwarded straight to [PannableCanvas]'s own parameters of the same names.
 */
@Composable
fun <T> NodeTreeView(
	roots: List<T>,
	children: (T) -> List<T>,
	parents: ((T) -> List<T>)? = null,
	modifier: Modifier = Modifier,
	columnGap: Int = DEFAULT_COLUMN_GAP,
	rowGap: Int = DEFAULT_ROW_GAP,
	connectorStyle: (T) -> ConnectorStyle = { ConnectorStyle.SOLID },
	connectorColor: (T) -> Int = { DEFAULT_LINE_COLOR },
	connectorThickness: (T) -> Int = { DEFAULT_LINE_THICKNESS },
	connectorAnimation: (T) -> ConnectorAnimation = { ConnectorAnimation.NONE },
	connectorShape: (T) -> ConnectorShape = { ConnectorShape.ELBOW },
	connectorDirection: (T) -> ConnectorDirection = { ConnectorDirection.CHILD_TO_PARENT },
	nodeAnimation: (T) -> NodeAnimation = { NodeAnimation.NONE },
	rootAlignment: RootAlignment = RootAlignment.START,
	dedupe: Boolean = false,
	showNodeHints: Boolean = true,
	key: (T) -> Any? = { it },
	visible: (T) -> Boolean = { true },
	onVisibilityChanged: (T, Boolean) -> Unit = { _, _ -> },
	state: PannableCanvasState = rememberPannableCanvasState(),
	backgroundTexture: ResourceLocation? = null,
	backgroundTextureSize: Int = 32,
	backgroundTint: Int = -1,
	backgroundParallax: Float = 1f,
	panelTexture: String = "surface",
	panelVariant: String? = "inset_transparent",
	panelContentPadding: Int = 2,
	content: @Composable (T) -> Unit,
) {
	val flat = remember(roots, dedupe) { layoutForest(roots, children, parents, dedupe, key) }
	val ownedChildren = remember(flat) {
		val owned = List(flat.size) { mutableListOf<Int>() }
		for (i in flat.indices) flat[i].parents.firstOrNull()?.let { owned[it] += i }
		owned
	}
	val rows = remember(flat) { assignRows(flat, ownedChildren) }
	val zoom = state.zoom

	val positioned = remember { mutableListOf<IntArray>() }
	val previousVisibility = remember { mutableListOf<Boolean?>() }

	val measurePolicy = remember(flat, rows, columnGap, rowGap, zoom, rootAlignment) {
		MeasurePolicy { _, measurables, _ ->
			val sizes = measurables.map { it.measure(Constraints(minWidth = 0, maxWidth = Int.MAX_VALUE, minHeight = 0, maxHeight = Int.MAX_VALUE)) }
			val scaledColumnGap = (columnGap * zoom).roundToInt()
			val scaledRowGap = (rowGap * zoom).roundToInt()

			val maxDepth = flat.maxOfOrNull { it.depth } ?: 0
			val columnWidth = IntArray(maxDepth + 1)
			for (i in flat.indices) columnWidth[flat[i].depth] = maxOf(columnWidth[flat[i].depth], sizes[i].width)
			val columnX = IntArray(maxDepth + 1)
			for (d in 1..maxDepth) columnX[d] = columnX[d - 1] + columnWidth[d - 1] + scaledColumnGap
			val totalContentWidth = if (maxDepth >= 0) columnX[maxDepth] + columnWidth[maxDepth] else 0
			fun columnXFor(depth: Int): Int =
				if (rootAlignment == RootAlignment.END) totalContentWidth - columnX[depth] - columnWidth[depth] else columnX[depth]

			val leafRowCount = (rows.maxOrNull()?.let { floor(it).toInt() } ?: 0) + 1
			val rowHeight = IntArray(leafRowCount) { FALLBACK_ROW_HEIGHT }
			for (i in flat.indices) {
				val row = rows[i]
				if (row == floor(row)) {
					val slot = row.toInt().coerceIn(0, leafRowCount - 1)
					rowHeight[slot] = maxOf(rowHeight[slot], sizes[i].height)
				}
			}
			val rowY = IntArray(leafRowCount + 1)
			for (r in 1..leafRowCount) rowY[r] = rowY[r - 1] + rowHeight[r - 1] + scaledRowGap

			fun rowCenterY(row: Float): Int {
				val lower = floor(row).toInt().coerceIn(0, leafRowCount - 1)
				val upper = ceil(row).toInt().coerceIn(0, leafRowCount - 1)
				val lowerCenter = rowY[lower] + rowHeight[lower] / 2f
				if (lower == upper) return lowerCenter.roundToInt()
				val upperCenter = rowY[upper] + rowHeight[upper] / 2f
				return (lowerCenter + (upperCenter - lowerCenter) * (row - lower)).roundToInt()
			}

			positioned.clear()
			for (i in flat.indices) {
				val x = columnXFor(flat[i].depth)
				val y = rowCenterY(rows[i]) - sizes[i].height / 2
				positioned += intArrayOf(x, y, sizes[i].width, sizes[i].height)
			}

			val canvasWidth = flat.indices.maxOfOrNull { positioned[it][0] + sizes[it].width } ?: 0
			val canvasHeight = if (leafRowCount > 0) rowY[leafRowCount] - scaledRowGap else 0

			MeasureResult(canvasWidth, canvasHeight) {
				for (i in flat.indices) sizes[i].placeAt(positioned[i][0], positioned[i][1])
			}
		}
	}

	PannableCanvas(
		modifier = modifier,
		state = state,
		backgroundTexture = backgroundTexture,
		backgroundTextureSize = backgroundTextureSize,
		backgroundTint = backgroundTint,
		backgroundParallax = backgroundParallax,
		panelTexture = panelTexture,
		panelVariant = panelVariant,
		panelContentPadding = panelContentPadding,
	) {
		Layout(
			name = "NodeTreeViewCanvas",
			measurePolicy = measurePolicy,
			renderer = object : Renderer {
				override fun render(node: UINode, x: Int, y: Int, guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
					if (positioned.size != flat.size) return

					if (previousVisibility.size != flat.size) {
						previousVisibility.clear()
						repeat(flat.size) { previousVisibility += null }
					}
					val viewportLeft = state.originX
					val viewportTop = state.originY
					val viewportRight = viewportLeft + state.viewportWidth
					val viewportBottom = viewportTop + state.viewportHeight
					for (i in flat.indices) {
						val pos = positioned[i]
						val nodeLeft = x + pos[0]
						val nodeTop = y + pos[1]
						val visible = nodeLeft < viewportRight && nodeLeft + pos[2] > viewportLeft &&
								nodeTop < viewportBottom && nodeTop + pos[3] > viewportTop
						if (previousVisibility[i] != visible) {
							previousVisibility[i] = visible
							onVisibilityChanged(flat[i].node, visible)
						}
					}

					val flowPhase = ((System.currentTimeMillis() % FLOW_PERIOD_MS).toFloat() / FLOW_PERIOD_MS)
					val wavePhase = ((System.currentTimeMillis() % WAVE_PERIOD_MS).toFloat() / WAVE_PERIOD_MS)
					val halfGap = ((columnGap * zoom).roundToInt()) / 2
					val cornerRadius = SPLINE_CORNER_RADIUS * zoom
					fun detourClearanceY(minDepth: Int, maxDepth: Int, floor: Int): Int {
						var clearance = floor
						for (j in flat.indices) {
							val depth = flat[j].depth
							if (depth <= minDepth || depth >= maxDepth) continue
							val bottom = y + positioned[j][1] + positioned[j][3]
							if (bottom > clearance) clearance = bottom
						}
						return clearance
					}
					for (i in flat.indices) {
						val flatNode = flat[i]
						if (flatNode.parents.isEmpty() && flatNode.hiddenParentCount == 0) continue
						val childPos = positioned[i]
						val childLeft = x + childPos[0]
						val childRight = childLeft + childPos[2]
						val childCenterY = y + childPos[1] + childPos[3] / 2
						val thickness = maxOf(1, (connectorThickness(flatNode.node) * zoom).roundToInt())
						val chosenStyle = connectorStyle(flatNode.node)
						val color = connectorColor(flatNode.node)
						val chosenAnimation = connectorAnimation(flatNode.node)
						val shape = connectorShape(flatNode.node)
						val direction = connectorDirection(flatNode.node)
						for (parentIndex in flatNode.parents) {
							val parentPos = positioned[parentIndex]
							val parentLeft = x + parentPos[0]
							val parentRight = parentLeft + parentPos[2]
							val parentCenterY = y + parentPos[1] + parentPos[3] / 2
							val childFacesRight = childLeft <= parentLeft
							val childEdgeX = if (childFacesRight) childRight else childLeft
							val parentEdgeX = if (childFacesRight) parentLeft else parentRight
							val minDepth = minOf(flatNode.depth, flat[parentIndex].depth)
							val maxDepth = maxOf(flatNode.depth, flat[parentIndex].depth)
							val clearanceFloor = maxOf(childCenterY, parentCenterY)
							val clearanceY = detourClearanceY(minDepth, maxDepth, clearanceFloor)
							val points = pointsFor(childEdgeX, childCenterY, flatNode.depth, parentEdgeX, parentCenterY, flat[parentIndex].depth, halfGap, clearanceY)
							val orientedPoints = if (direction == ConnectorDirection.PARENT_TO_CHILD) points.reversed() else points
							val childVisible = visible(flatNode.node)
							val parentVisible = visible(flat[parentIndex].node)
							if (showNodeHints && (!childVisible || !parentVisible)) {
								if (childVisible) {
									drawWavyStub(
										guiGraphics,
										childEdgeX.toFloat(), childCenterY.toFloat(),
										(parentEdgeX - childEdgeX).toFloat(), (parentCenterY - childCenterY).toFloat(),
										halfGap, thickness, color, wavePhase,
									)
								}
								if (parentVisible) {
									drawWavyStub(
										guiGraphics,
										parentEdgeX.toFloat(), parentCenterY.toFloat(),
										(childEdgeX - parentEdgeX).toFloat(), (childCenterY - parentCenterY).toFloat(),
										halfGap, thickness, color, wavePhase,
									)
								}
							} else {
								val animPhase = if (chosenAnimation == ConnectorAnimation.FLOWING) flowPhase else 0f
								if (shape == ConnectorShape.SPLINE) {
									drawSplinePolyline(guiGraphics, orientedPoints, thickness, color, chosenStyle, chosenAnimation, animPhase, cornerRadius)
								} else {
									drawPolyline(guiGraphics, orientedPoints, thickness, color, chosenStyle, chosenAnimation, animPhase)
								}
							}
						}
						if (flatNode.hiddenParentCount > 0) {
							val stubFacesRight = rootAlignment == RootAlignment.END
							val stubEdgeX = if (stubFacesRight) childRight else childLeft
							val stubDirection = if (stubFacesRight) 1f else -1f
							repeat(flatNode.hiddenParentCount) { index ->
								val angleSign = if (index % 2 == 0) 1 else -1
								drawWavyStub(
									guiGraphics,
									stubEdgeX.toFloat(), childCenterY.toFloat(),
									stubDirection, 0f,
									halfGap, thickness, color, wavePhase, angleSign,
								)
							}
						}
					}
				}
			},
			content = {
				for (flatNode in flat) ZoomedBox(zoom, nodeAnimation(flatNode.node), visible(flatNode.node)) { content(flatNode.node) }
			},
		)
	}
}
