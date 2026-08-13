package net.kernelpanicsoft.archie.gui.composables.input

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import net.kernelpanicsoft.archie.gui.interaction.MutableInteractionSource
import net.kernelpanicsoft.archie.gui.interaction.collectIsFocusedAsState
import net.kernelpanicsoft.archie.gui.interaction.collectIsHoveredAsState
import net.kernelpanicsoft.archie.gui.interaction.collectIsPressedAsState
import net.kernelpanicsoft.archie.gui.layout.Alignment
import net.kernelpanicsoft.archie.gui.layout.Box
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.modifiers.input.focusable
import net.kernelpanicsoft.archie.gui.modifiers.input.hoverable
import net.kernelpanicsoft.archie.gui.modifiers.input.onKeyEvent
import net.kernelpanicsoft.archie.gui.modifiers.input.pressable
import net.kernelpanicsoft.archie.gui.nodes.UINode
import net.kernelpanicsoft.archie.util.minecraftClient
import net.minecraft.client.Minecraft
import org.lwjgl.glfw.GLFW

private object CursorCache {
    val handCursor: Long by lazy { GLFW.glfwCreateStandardCursor(GLFW.GLFW_HAND_CURSOR) }
}

/** GLFW key codes that activate a focused clickable, mirroring vanilla `AbstractWidget` activation. */
private val CLICK_ACTIVATION_KEYS = intArrayOf(GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER, GLFW.GLFW_KEY_SPACE)

private fun setHandCursor(enabled: Boolean) {
    // GLFW calls must happen on the render thread; DisposableEffect callbacks run on the
    // recomposition dispatcher, so hop over via Minecraft's thread-safe task queue.
    minecraftClient.execute {
        val window = minecraftClient.window.window
        GLFW.glfwSetCursor(window, if (enabled) CursorCache.handCursor else 0L)
    }
}

/**
 * Low-level unstyled clickable container used by higher-level inputs like [ButtonCore].
 *
 * Tracks hover/press state via [hoverable]/[pressable] and fires [onClick] on press (not
 * release), showing the system hand cursor on hover when [showHandCursor] is `true`. Also
 * registers as a vanilla keyboard/controller focus-navigation stop (see [focusable]) and fires
 * [onClick] on Enter/Space while focused - the same activation model Compose Foundation's own
 * `Modifier.clickable` bakes in, rather than treating focus as a separate concern from click.
 * Applies no visual styling itself - that is entirely up to [content].
 *
 * @param onClick           Invoked with the receiving [UINode] on press, or on Enter/Space
 *   while vanilla-focused.
 * @param modifier          Additional modifiers applied to the outer [Box].
 * @param enabled           When `false`, pointer and activation-key events are ignored, no
 *   cursor change occurs, and this drops out of the vanilla focus graph entirely.
 * @param showHandCursor    Whether to switch to the hand cursor while hovered.
 * @param interactionSource Backs `isHovered`/`isPressed`/`isFocused`, collectible independently
 *   via `net.kernelpanicsoft.archie.gui.interaction.collectIsXAsState` too.
 * @param content           The visual content; receives `isHovered`/`isPressed`/`isFocused` for styling.
 */
@Composable
fun Clickable(
	onClick: (UINode) -> Unit,
	modifier: Modifier = Modifier,
	enabled: Boolean = true,
	showHandCursor: Boolean = true,
	interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
	content: @Composable (isHovered: Boolean, isPressed: Boolean, isFocused: Boolean) -> Unit,
) {
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()

    DisposableEffect(enabled, isHovered, showHandCursor) {
        if (showHandCursor) setHandCursor(enabled && isHovered)
        onDispose {
            if (showHandCursor) setHandCursor(false)
        }
    }

    Box(
        modifier = Modifier
            .focusable(enabled = enabled, interactionSource = interactionSource)
            .onKeyEvent { node, e ->
                if (enabled && isFocused && e.keyCode in CLICK_ACTIVATION_KEYS) {
                    onClick(node)
                    e.consume(bypassSuperCall = true)
                }
            }
            .hoverable(interactionSource, enabled = enabled)
            .pressable(interactionSource, enabled = enabled, onPress = onClick)
            .then(modifier),
        contentAlignment = Alignment.Center,
    ) {
        content(isHovered, isPressed, isFocused)
    }
}
