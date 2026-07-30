package net.kernelpanicsoft.archie.gui.composables.input

import androidx.compose.runtime.*
import net.kernelpanicsoft.archie.gui.composables.theme.TextureStates
import net.kernelpanicsoft.archie.gui.layout.Alignment
import net.kernelpanicsoft.archie.gui.layout.Box
import net.kernelpanicsoft.archie.gui.layout.BoxMeasurePolicy
import net.kernelpanicsoft.archie.gui.layout.Layout
import net.kernelpanicsoft.archie.gui.layout.Renderer
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.modifiers.debug
import net.kernelpanicsoft.archie.gui.modifiers.input.PointerEventType
import net.kernelpanicsoft.archie.gui.modifiers.input.onPointerEvent
import net.kernelpanicsoft.archie.gui.modifiers.sizeIn
import net.kernelpanicsoft.archie.gui.nodes.UINode
import net.kernelpanicsoft.archie.gui.theme.LocalTheme
import net.kernelpanicsoft.archie.gui.theme.SimpleThemeState
import net.kernelpanicsoft.archie.gui.theme.ThemeVariants
import net.kernelpanicsoft.archie.gui.util.extension.drawThemeState
import net.minecraft.client.gui.GuiGraphics

/**
 * A standard themed checkbox.
 *
 * Renders the themed [texture] state - a combined checked+hovered state is used when both
 * apply and the theme defines it. Built on top of [CheckboxCore]; use that directly for
 * fully custom visuals.
 *
 * @param checked         The current checked state.
 * @param modifier        Additional modifiers applied to the outer container.
 * @param texture         The themed texture key to look up via [LocalTheme].
 * @param variant         The theme variant of [texture] to use. See [ThemeVariants].
 * @param onCheckedChange Called with the new checked value when the user clicks.
 */
@Composable
fun Checkbox(
    checked: Boolean = false,
    modifier: Modifier = Modifier,
    texture: String = "checkbox",
    variant: String = ThemeVariants.DEFAULT,
    onCheckedChange: (Boolean) -> Unit,
) {
    val theme = LocalTheme.current
    val composableTheme = theme.getComposableTheme(texture)
    val sizeModifier = if (!composableTheme.isNineslice) {
        with(composableTheme.states[TextureStates.DEFAULT] as SimpleThemeState) {
            Modifier.sizeIn(
                minWidth = width,
                minHeight = height
            )
        }
    } else Modifier

    CheckboxCore(
        checked,
        sizeModifier.then(modifier),
        onCheckedChange
    ) { isHovered ->
        Layout(
            name = "Checkbox",
            measurePolicy = BoxMeasurePolicy(Alignment.Center),
            renderer = object : Renderer
            {
                override fun render(
	                node: UINode,
	                x: Int,
	                y: Int,
	                guiGraphics: GuiGraphics,
	                mouseX: Int,
	                mouseY: Int,
	                partialTick: Float
                ) {
                    val state = composableTheme.getState(
                        when {
                            checked && isHovered -> TextureStates.CLICKED_AND_HOVERED
                            isHovered -> TextureStates.HOVERED
                            checked -> TextureStates.CLICKED
                            else -> TextureStates.DEFAULT
                        },
                        variant
                    )

                    guiGraphics.drawThemeState(state, x, y, node.width, node.height)

                    super.render(node, x, y, guiGraphics, mouseX, mouseY, partialTick)
                }
            },
            modifier = sizeModifier
        )
    }
}

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
            .debug("Hovered: $hovered")
            .onPointerEvent<UINode>(PointerEventType.ENTER) { _, e -> hovered = true;  e.consume() }
            .onPointerEvent<UINode>(PointerEventType.EXIT)  { _, e -> hovered = false; e.consume() }
            .onPointerEvent<UINode>(PointerEventType.PRESS) { _, e -> onCheckedChange(!checked); e.consume() }
            .then(modifier),
    ) {
        content(hovered)
    }
}
