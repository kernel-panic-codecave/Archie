package net.kernelpanicsoft.archie.gui.composables.containers

import androidx.compose.runtime.Composable
import net.kernelpanicsoft.archie.gui.composables.input.Clickable
import net.kernelpanicsoft.archie.gui.composables.theme.TextureStates
import net.kernelpanicsoft.archie.gui.layout.Alignment
import net.kernelpanicsoft.archie.gui.layout.Box
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.modifiers.position.padding
import net.kernelpanicsoft.archie.gui.nodes.UINode
import net.kernelpanicsoft.archie.gui.theme.ThemeVariants

/** [NodeFrame]'s own alias for [ThemeVariants.DEFAULT] - "task" reads better than an empty
 *  string alongside its sibling kinds ("goal", "challenge") at a call site, but there's no
 *  reason for a theme to declare a whole separate `"task"` variant duplicating what the base
 *  state map (the actual default variant) already provides. */
private const val TASK_VARIANT = "task"

/**
 * A themed, shaped frame for one [NodeTreeView] node's own content - the vanilla advancements
 * screen's "normal"/"goal"/"challenge" icon frames (square, rounded, spiky) are exactly this same
 * idea: one differently-bordered box per node kind, chosen by [variant] rather than by any
 * Kotlin-side shape logic, the same "the theme decides the art, the composable decides the
 * structure" split every other themed container in this package ([Panel], [Surface]) already
 * follows - a theme not declaring a `node_frame` texture at all just falls back to [Surface]'s own
 * default sprite behavior.
 *
 * [obtained] is a separate axis from [variant] - a [TextureStates.OBTAINED]/[TextureStates.UNOBTAINED]
 * *state* within whichever variant's own texture (matching how a themed button already picks a
 * different sprite per interactive state), not a whole other variant to declare per kind. A theme
 * only needs to declare `"goal"`/`"challenge"` as variants at all if it wants their frames to look
 * different in *shape* from the default (`"task"`) frame - the default's own state map already
 * covers the unshaped default+obtained pair.
 *
 * Nothing here is [NodeTreeView]-specific beyond its own default sizing suiting a small icon-sized
 * node - use a plain [Panel]/[Surface] (or anything else) instead if a tree's nodes don't want a
 * frame at all.
 *
 * @param variant  The theme variant of the `node_frame` texture to draw - one per distinct node
 *   shape/kind a theme wants to distinguish (e.g. `"goal"`, `"challenge"`), on top of `"task"`
 *   (an alias for [ThemeVariants.DEFAULT]) for an ordinary node.
 * @param obtained Whether this node's own progress is complete - selects
 *   [TextureStates.OBTAINED] vs. [TextureStates.UNOBTAINED] within [variant]'s own texture.
 * @param onClick  When given, the whole frame becomes a [Clickable] - a [NodeTreeView] node this
 *   wraps can be tapped to open it, request it, etc. without needing its own separate button.
 *   `null` (the default) renders a plain, non-interactive frame with none of [Clickable]'s own
 *   focus/hover/cursor machinery attached at all.
 */
@Composable
fun NodeFrame(
	modifier: Modifier = Modifier,
	variant: String = ThemeVariants.DEFAULT,
	obtained: Boolean = false,
	contentAlignment: Alignment = Alignment.Center,
	contentPadding: Int = 8,
	onClick: ((UINode) -> Unit)? = null,
	content: @Composable () -> Unit,
) {
	val frame: @Composable (Modifier) -> Unit = { frameModifier ->
		Surface(
			modifier = frameModifier,
			texture = "node_frame",
			variant = if (variant == TASK_VARIANT) ThemeVariants.DEFAULT else variant,
			stateName = if (obtained) TextureStates.OBTAINED else TextureStates.UNOBTAINED,
			contentAlignment = contentAlignment,
		) {
			Box(modifier = Modifier.padding(contentPadding), contentAlignment = contentAlignment) {
				content()
			}
		}
	}
	if (onClick != null) {
		Clickable(onClick = onClick, modifier = modifier) { _, _, _ -> frame(Modifier) }
	} else {
		frame(modifier)
	}
}
