package net.kernelpanicsoft.archie.gui.composables.input

import androidx.compose.runtime.*
import net.kernelpanicsoft.archie.gui.layout.Box
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.modifiers.DebugModifier
import net.kernelpanicsoft.archie.gui.modifiers.input.PointerEventType
import net.kernelpanicsoft.archie.gui.modifiers.input.onPointerEvent
import net.kernelpanicsoft.archie.gui.nodes.AUINode

/**
 * A stateless, unstyled toggle composable.
 *
 * `CheckboxCore` manages hover state internally and exposes it to [content]. All visual
 * styling (textures, colours, checked indicator) is the responsibility of [content]. Use
 * this as the base for custom or theme-driven checkbox implementations.
 *
 * ### Example
 * ```kotlin
 * var checked by remember { mutableStateOf(false) }
 * CheckboxCore(checked = checked, onCheckedChange = { checked = it }) { isHovered ->
 *     Box(modifier = Modifier.size(16, 16).background(if (checked) KColor.GREEN else KColor.GRAY))
 * }
 * ```
 *
 * @param checked         The current checked state.
 * @param modifier        Additional modifiers applied to the outer [Box].
 * @param onCheckedChange Called with the new checked value when the user clicks.
 * @param content         The visual content; receives `isHovered` for styling.
 */
@Composable
fun CheckboxCore(
    checked: Boolean = false,
    modifier: Modifier = Modifier,
    onCheckedChange: (Boolean) -> Unit,
    content: @Composable (isHovered: Boolean) -> Unit,
) {
    var hovered by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .then(DebugModifier(strs = listOf("Hovered: $hovered")))
            .onPointerEvent<AUINode>(PointerEventType.ENTER) { _, e -> hovered = true;  e.consume() }
            .onPointerEvent<AUINode>(PointerEventType.EXIT)  { _, e -> hovered = false; e.consume() }
            .onPointerEvent<AUINode>(PointerEventType.PRESS) { _, e -> onCheckedChange(!checked); e.consume() }
            .then(modifier),
    ) {
        content(hovered)
    }
}
