package net.kernelpanicsoft.archie.gui.modifiers.appearance

import androidx.compose.runtime.Stable
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.minecraft.world.inventory.tooltip.TooltipComponent

/**
 * A [Modifier.Element] that attaches one or more [TooltipComponent]s to a composable.
 *
 * Multiple [TooltipModifier] elements on the same node are merged by concatenating their
 * tooltip lists.
 *
 * @property tooltips The list of tooltip components to display.
 */
data class TooltipModifier(val tooltips: List<TooltipComponent>) : Modifier.Element<TooltipModifier> {
    override fun mergeWith(other: TooltipModifier): TooltipModifier =
        TooltipModifier(tooltips + other.tooltips)
}

/**
 * Attaches one or more [TooltipComponent]s to this composable.
 *
 * The tooltips are merged with any existing [TooltipModifier] on the node.
 *
 * @param tooltips The tooltip components to attach.
 */
@Stable
fun Modifier.tooltip(vararg tooltips: TooltipComponent): Modifier =
    this then TooltipModifier(tooltips.toList())
