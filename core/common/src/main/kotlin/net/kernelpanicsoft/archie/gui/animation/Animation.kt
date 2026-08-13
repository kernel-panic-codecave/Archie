package net.kernelpanicsoft.archie.gui.animation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/** Describes a time-based interpolation used by Archie GUI animations. */
fun interface Easing {
    /** Maps a linear progress [fraction] in `0f..1f` to an eased progress value. */
    fun transform(fraction: Float): Float
}

/**
 * Common easing curves for small UI interactions.
 *
 * The curves are intentionally lightweight so they can run smoothly in frequent recompositions.
 */
object Easings {
    /** No easing; progress is directly proportional to elapsed time. */
    val Linear = Easing { it }

    /** Starts fast and decelerates into the target value, with no overshoot. */
    val OutCubic = Easing { t ->
        val inv = 1f - t
        1f - inv * inv * inv
    }

    /** Like [OutCubic] but overshoots the target slightly before settling. */
    val OutBack = Easing { t ->
        val c1 = 1.70158f
        val c3 = c1 + 1f
        val shifted = t - 1f
        1f + c3 * shifted * shifted * shifted + c1 * shifted * shifted
    }
}

/**
 * Timing parameters for float/int animations.
 *
 * @param durationMillis How long the animation takes to reach its target value.
 * @param easing The curve applied to progress over that duration.
 */
data class AnimationSpec(
    val durationMillis: Duration = 220.milliseconds,
    val easing: Easing = Easings.OutCubic,
)

/** Animates a float value toward [targetValue] using [spec]. */
@Composable
fun animateFloat(targetValue: Float, spec: AnimationSpec = AnimationSpec()): Float {
    var value by remember { mutableFloatStateOf(targetValue) }

    LaunchedEffect(targetValue, spec.durationMillis, spec.easing) {
        val duration = spec.durationMillis
        if (duration <= 0.milliseconds) {
            value = targetValue
            return@LaunchedEffect
        }

        val start = value
        val delta = targetValue - start
        if (delta == 0f) return@LaunchedEffect

        val startTime = withFrameNanos { it }
        var frameTime = startTime
        do {
            val elapsedNanos = frameTime - startTime
            val rawProgress = (elapsedNanos / (duration.inWholeMilliseconds * 1_000_000f)).coerceIn(0f, 1f)
            val eased = spec.easing.transform(rawProgress)
            value = start + delta * eased
            frameTime = withFrameNanos { it }
        } while (rawProgress < 1f)

        value = targetValue
    }

    return value
}

/** Animates an integer by interpolating as float and rounding to the nearest pixel. */
@Composable
fun animateInt(targetValue: Int, spec: AnimationSpec = AnimationSpec()): Int {
    val animatedFloat = animateFloat(targetValue.toFloat(), spec)
    return animatedFloat.roundToInt()
}

/**
 * Sweeps from [from] to [to] every time [key] changes, holding at [to] otherwise - a one-shot
 * "pulse"/"pop" effect for a discrete event (a checkbox toggling, a radio button being
 * selected) rather than the continuous value [animateFloat]/[animateInt] track toward a moving
 * target. Pair with [Easings.OutBack] for a satisfying overshoot-then-settle bounce.
 *
 * Does not pulse on the composable's initial composition - only on a later change of [key].
 */
@Composable
fun animatePulse(key: Any?, from: Float = 0.8f, to: Float = 1f, spec: AnimationSpec = AnimationSpec()): Float {
    var value by remember { mutableFloatStateOf(to) }
    var initialized by remember { mutableStateOf(false) }

    LaunchedEffect(key) {
        if (!initialized) {
            initialized = true
            return@LaunchedEffect
        }

        val duration = spec.durationMillis
        if (duration <= 0.milliseconds) {
            value = to
            return@LaunchedEffect
        }

        value = from
        val startTime = withFrameNanos { it }
        var frameTime = startTime
        var rawProgress: Float
        do {
            val elapsedNanos = frameTime - startTime
            rawProgress = (elapsedNanos / (duration.inWholeMilliseconds * 1_000_000f)).coerceIn(0f, 1f)
            value = from + (to - from) * spec.easing.transform(rawProgress)
            frameTime = withFrameNanos { it }
        } while (rawProgress < 1f)

        value = to
    }

    return value
}


