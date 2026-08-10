package net.kernelpanicsoft.archie.gui.modifiers

import androidx.compose.runtime.Stable
import net.minecraft.network.chat.Component

/**
 * A [Modifier.Element] that attaches arbitrary debug information to a composable node.
 *
 * Debug information is only visible when the debug overlay is active (toggle with
 * **Ctrl + Shift** while the screen is open). When [net.kernelpanicsoft.archie.gui.layout.LayoutNode.extraDebug]
 * is enabled (hold Shift in debug mode), the attached strings and components are rendered
 * inside the debug tooltip alongside node dimensions and coordinates.
 *
 * Multiple [DebugModifier] elements on the same node are merged by concatenation.
 *
 * @property strs  Plain-text debug strings.
 * @property comps Formatted [Component] debug labels.
 */
data class DebugModifier(
    val strs: List<String> = emptyList(),
    val comps: List<Component> = emptyList(),
) : Modifier.Element<DebugModifier> {

    override fun mergeWith(other: DebugModifier): DebugModifier =
        DebugModifier(strs = strs + other.strs, comps = comps + other.comps)

    override fun toString(): String = strs.joinToString(", ").ifEmpty { super.toString() }

    override fun toComponent(): Component = Component.empty().apply {
        strs.map { Component.literal(it) }.forEach { append(it) }
        comps.forEach { append(it) }
    }.takeIf { it != Component.empty() } ?: Component.literal(super.toString())

    /** Returns the debug information as a list of individual [Component]s, one per item. */
    fun toComponents(): List<Component> =
        (strs.map { Component.literal(it) } + comps).ifEmpty { listOf(Component.literal(super.toString())) }
}

/**
 * Attaches one or more plain-text debug strings to the composable.
 *
 * The strings are displayed in the debug overlay when debug mode is active.
 *
 * @param strs The strings to attach.
 */
@Stable
fun Modifier.debug(vararg strs: String): Modifier = this then DebugModifier(strs = strs.toList())

/**
 * Attaches one or more formatted [Component] debug labels to the composable.
 *
 * @param comps The components to attach.
 */
@Stable
fun Modifier.debug(vararg comps: Component): Modifier = this then DebugModifier(comps = comps.toList())
