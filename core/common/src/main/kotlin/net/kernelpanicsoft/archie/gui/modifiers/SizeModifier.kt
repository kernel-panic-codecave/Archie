package net.kernelpanicsoft.archie.gui.modifiers

import androidx.compose.runtime.Stable
import kotlin.math.roundToInt

/**
 * A [Modifier.Element] that constrains the intrinsic size of a composable node by clamping
 * the [Constraints] passed to it during measurement.
 *
 * Multiple [SizeModifier] elements on the same node are merged by intersecting their ranges,
 * so the resulting constraints satisfy all modifiers simultaneously.
 *
 * Prefer the extension functions ([size], [sizeIn], [width], [height]) over constructing
 * this class directly.
 *
 * @property constraints The [Constraints] to enforce.
 */
data class SizeModifier(
	val constraints: Constraints
) : Modifier.Element<SizeModifier>, LayoutChangingModifier {
	override fun mergeWith(other: SizeModifier) = with(constraints) {
		SizeModifier(
			Constraints(
				other.constraints.minWidth.coerceIn(minWidth, maxWidth),
				other.constraints.maxWidth.coerceIn(minWidth, maxWidth),
				other.constraints.minHeight.coerceIn(minHeight, maxHeight),
				other.constraints.maxHeight.coerceIn(minHeight, maxHeight),
			)
		)
	}

	override fun modifyInnerConstraints(constraints: Constraints): Constraints {
		return SizeModifier(constraints).mergeWith(this).constraints
	}
}

/**
 * A [LayoutChangingModifier] that forces the node to fill a [percent] fraction of the
 * available horizontal space.
 *
 * @property percent Fraction of available width to fill (0.0–1.0, default 1.0 = full width).
 */
data class HorizontalFillModifier(
	val percent: Double
) : Modifier.Element<HorizontalFillModifier>, LayoutChangingModifier {
	override fun mergeWith(other: HorizontalFillModifier) = other

	override fun modifyInnerConstraints(constraints: Constraints): Constraints {
		val fillWidth = (constraints.minWidth + percent * (constraints.maxWidth - constraints.minWidth)).roundToInt()
		return constraints.copy(
			minWidth = fillWidth,
			maxWidth = fillWidth
		)
	}
}

/**
 * A [LayoutChangingModifier] that forces the node to fill a [percent] fraction of the
 * available vertical space.
 *
 * @property percent Fraction of available height to fill (0.0–1.0, default 1.0 = full height).
 */
data class VerticalFillModifier(
	val percent: Double
) : Modifier.Element<VerticalFillModifier>, LayoutChangingModifier {
	override fun mergeWith(other: VerticalFillModifier) = other

	override fun modifyInnerConstraints(constraints: Constraints): Constraints {
		val fillHeight =
			(constraints.minHeight + percent * (constraints.maxHeight - constraints.minHeight)).roundToInt()
		return constraints.copy(
			minHeight = fillHeight,
			maxHeight = fillHeight
		)
	}
}

/**
 * Forces the node to fill [percent] of the maximum available width.
 *
 * @param percent Fraction of available width (0.0–1.0). Default `1.0` fills all available width.
 */
@Stable
fun Modifier.fillMaxWidth(percent: Double = 1.0) = then(HorizontalFillModifier(percent))

/**
 * Forces the node to fill [percent] of the maximum available height.
 *
 * @param percent Fraction of available height (0.0–1.0). Default `1.0` fills all available height.
 */
@Stable
fun Modifier.fillMaxHeight(percent: Double = 1.0) = then(VerticalFillModifier(percent))

/**
 * Forces the node to fill [percent] of both the available width and height.
 *
 * @param percent Fraction of available space (0.0–1.0). Default `1.0` fills all available space.
 */
@Stable
fun Modifier.fillMaxSize(percent: Double = 1.0) = then(HorizontalFillModifier(percent)).then(VerticalFillModifier(percent))

/**
 * Constrains the node's width and height to be within the given min/max bounds.
 *
 * @param minWidth  Minimum width in pixels.
 * @param maxWidth  Maximum width in pixels.
 * @param minHeight Minimum height in pixels.
 * @param maxHeight Maximum height in pixels.
 */
@Stable
fun Modifier.sizeIn(
	minWidth: Int = 0,
	maxWidth: Int = Integer.MAX_VALUE,
	minHeight: Int = 0,
	maxHeight: Int = Integer.MAX_VALUE,
) = then(SizeModifier(Constraints(minWidth, maxWidth, minHeight, maxHeight)))

/** Sets an exact fixed size of [width] × [height] pixels. */
@Stable
fun Modifier.size(width: Int, height: Int) = sizeIn(width, width, height, height)

/** Sets an exact fixed square size of [size] × [size] pixels. */
@Stable
fun Modifier.size(size: Int) = size(size, size)

/** Sets an exact fixed width of [width] pixels (height unconstrained). */
@Stable
fun Modifier.width(width: Int) = sizeIn(width, width, 0, Integer.MAX_VALUE)

/** Sets an exact fixed height of [height] pixels (width unconstrained). */
@Stable
fun Modifier.height(height: Int) = sizeIn(0, Integer.MAX_VALUE, height, height)