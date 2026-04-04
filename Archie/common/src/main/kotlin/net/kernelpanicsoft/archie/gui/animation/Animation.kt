package net.kernelpanicsoft.archie.gui.animation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import kotlin.math.roundToInt

/** Describes a time-based interpolation used by Archie GUI animations. */
fun interface Easing {
    fun transform(fraction: Float): Float
}

/**
 * Common easing curves for small UI interactions.
 *
 * The curves are intentionally lightweight so they can run smoothly in frequent recompositions.
 */
object Easings {
    val Linear = Easing { it }
    val OutCubic = Easing { t ->
        val inv = 1f - t
        1f - inv * inv * inv
    }
    val OutBack = Easing { t ->
        val c1 = 1.70158f
        val c3 = c1 + 1f
        val shifted = t - 1f
        1f + c3 * shifted * shifted * shifted + c1 * shifted * shifted
    }
}

/** Timing parameters for float/int animations. */
data class AnimationSpec(
    val durationMillis: Int = 220,
    val easing: Easing = Easings.OutCubic,
)

/** Animates a float value toward [targetValue] using [spec]. */
@Composable
fun animateFloat(targetValue: Float, spec: AnimationSpec = AnimationSpec()): Float {
    var value by remember { mutableFloatStateOf(targetValue) }

    LaunchedEffect(targetValue, spec.durationMillis, spec.easing) {
        val duration = spec.durationMillis
        if (duration <= 0) {
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
            val rawProgress = (elapsedNanos / (duration * 1_000_000f)).coerceIn(0f, 1f)
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


