package net.kernelpanicsoft.archie.gui.composables.basic

import androidx.compose.runtime.Composable
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.theme.ThemeVariants
import net.kernelpanicsoft.archie.transfer.ArchieEnergyStorage

/**
 * A themed energy-level indicator (looked up in the current theme as `"energy_bar"`), filled
 * with a solid color up to `energy / capacity`.
 *
 * Shares [ProgressBar]'s rendering core but defaults to a bottom-up fill and an energy-flavored
 * color, matching how most tech mods orient a power gauge.
 *
 * @param energy    The current stored amount (see [ArchieEnergyStorage.getAmount]).
 * @param capacity  The maximum capacity (see [ArchieEnergyStorage.getCapacity]); a non-positive
 *   value renders as empty rather than dividing by zero.
 * @param modifier  Additional modifiers applied to the outer container.
 * @param direction Which edge the fill grows from.
 * @param fillColor ARGB color of the filled portion.
 * @param variant   The theme variant used for the track texture.
 */
@Composable
fun EnergyBar(
	energy: Long,
	capacity: Long,
	modifier: Modifier = Modifier,
	direction: ProgressDirection = ProgressDirection.BOTTOM_TO_TOP,
	fillColor: Int = 0xFFFF5C33.toInt(),
	variant: String = ThemeVariants.DEFAULT,
)
{
	val fraction = if (capacity <= 0L) 0f else (energy.toDouble() / capacity.toDouble()).toFloat().coerceIn(0f, 1f)
	ThemedFillBar("energy_bar", fraction, modifier, direction, fillColor, variant)
}

/** Convenience overload reading directly from an [ArchieEnergyStorage]. */
@Composable
fun EnergyBar(
	storage: ArchieEnergyStorage,
	modifier: Modifier = Modifier,
	direction: ProgressDirection = ProgressDirection.BOTTOM_TO_TOP,
	fillColor: Int = 0xFFFF5C33.toInt(),
	variant: String = ThemeVariants.DEFAULT,
) = EnergyBar(storage.getAmount(), storage.getCapacity(), modifier, direction, fillColor, variant)
