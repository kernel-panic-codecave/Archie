/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.kernelpanicsoft.archie.gui.modifiers

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

@Immutable
/**
 * Immutable size constraints passed from a parent layout to its children during measurement.
 *
 * A child must produce a size whose width is in `[minWidth, maxWidth]` and whose height is
 * in `[minHeight, maxHeight]`. Use [copy] to derive a modified copy with some dimensions changed,
 * and [offset] to shrink the available space by a fixed amount (e.g. for padding).
 *
 * @property minWidth  Minimum allowed width in pixels (inclusive).
 * @property maxWidth  Maximum allowed width in pixels (inclusive).
 * @property minHeight Minimum allowed height in pixels (inclusive).
 * @property maxHeight Maximum allowed height in pixels (inclusive).
 */
class Constraints(
	val minWidth: Int = 0,
	val maxWidth: Int = Int.MAX_VALUE,
	val minHeight: Int = 0,
	val maxHeight: Int = Int.MAX_VALUE
) {
	fun copy(
		minWidth: Int = this.minWidth,
		maxWidth: Int = this.maxWidth,
		minHeight: Int = this.minHeight,
		maxHeight: Int = this.maxHeight
	) = Constraints(
		minWidth.coerceAtMost(maxWidth),
		maxWidth.coerceAtLeast(minWidth),
		minHeight.coerceAtMost(maxHeight),
		maxHeight.coerceAtLeast(minHeight)
	)

	override fun toString(): String
	{
		return "Constraints(minWidth=$minWidth, maxWidth=$maxWidth, minHeight=$minHeight, maxHeight=$maxHeight)"
	}
}

/**
 * Returns a copy of these [Constraints] expanded or shrunk by [horizontal] pixels on each
 * horizontal side and [vertical] pixels on each vertical side.
 *
 * Negative values shrink the available space (useful for padding).
 * [maxWidth] and [maxHeight] are never reduced below zero.
 */
@Stable
fun Constraints.offset(horizontal: Int = 0, vertical: Int = 0) = Constraints(
	(minWidth + horizontal).coerceAtLeast(0),
	addMaxWithMinimum(maxWidth, horizontal),
	(minHeight + vertical).coerceAtLeast(0),
	addMaxWithMinimum(maxHeight, vertical)
)

private fun addMaxWithMinimum(max: Int, value: Int): Int {
	return if (max == Int.MAX_VALUE) {
		max
	} else {
		(max + value).coerceAtLeast(0)
	}
}