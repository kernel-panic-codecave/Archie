package net.kernelpanicsoft.archie.gui.composables.input

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import net.kernelpanicsoft.archie.gui.composables.basic.Text
import net.kernelpanicsoft.archie.gui.composables.containers.Panel
import net.kernelpanicsoft.archie.gui.composables.containers.Scrollable
import net.kernelpanicsoft.archie.gui.layer.LocalLayerManagerOrNull
import net.kernelpanicsoft.archie.gui.layout.Alignment
import net.kernelpanicsoft.archie.gui.layout.Arrangement
import net.kernelpanicsoft.archie.gui.layout.Box
import net.kernelpanicsoft.archie.gui.layout.Column
import net.kernelpanicsoft.archie.gui.layout.IntCoordinates
import net.kernelpanicsoft.archie.gui.layout.Row
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.modifiers.fillMaxSize
import net.kernelpanicsoft.archie.gui.modifiers.height
import net.kernelpanicsoft.archie.gui.modifiers.input.PointerEventType
import net.kernelpanicsoft.archie.gui.modifiers.input.onPointerEvent
import net.kernelpanicsoft.archie.gui.modifiers.onGloballyPositioned
import net.kernelpanicsoft.archie.gui.modifiers.onSizeChanged
import net.kernelpanicsoft.archie.gui.modifiers.position.offset
import net.kernelpanicsoft.archie.gui.modifiers.position.zIndex
import net.kernelpanicsoft.archie.gui.modifiers.width
import net.kernelpanicsoft.archie.gui.nodes.UINode
import net.kernelpanicsoft.archie.gui.theme.LocalTheme
import net.minecraft.network.chat.Component

/** Height of one option row, in pixels - a line of text plus the padding either side of it. */
private const val OPTION_HEIGHT = 14

/**
 * Z-index an expanded dropdown takes among its siblings.
 *
 * Only relevant to the inline fallback, where the list shares a parent with whatever follows it.
 * The layer-hosted list sits above the entire base layer and needs no help.
 */
private const val EXPANDED_Z = 10f

/** Glyphs on the trigger showing which way the list will go. */
private const val COLLAPSED_GLYPH = "▼"
private const val EXPANDED_GLYPH = "▲"

/**
 * One selectable entry of a [Dropdown].
 *
 * @property value What [Dropdown]'s `onSelected` reports when this entry is picked.
 * @property label What the entry reads as, both in the list and on the trigger once selected.
 * @property enabled A shown-but-unpickable entry - worth preferring over omitting it, when its
 *   absence would leave the reader wondering where it went.
 */
data class DropdownOption<T>(
    val value: T,
    val label: Component,
    val enabled: Boolean = true,
)

/**
 * A single-choice select: a trigger showing the current selection, which opens into a list.
 *
 * The open list is hosted in a **layer of its own**, anchored under the trigger. That is what lets
 * it hang outside whatever contains the dropdown: an inline list is drawn inside its ancestors'
 * scissors, so one inside a fixed-height page or a
 * [net.kernelpanicsoft.archie.gui.composables.containers.Scrollable] was simply cut off at that
 * boundary - and no amount of z-index fixes that, because clipping happens while drawing rather
 * than being resolved by depth.
 *
 * Falls back to expanding inline where there is no layer manager to host the list, which keeps the
 * component usable on a plain screen at the cost of the clipping described above.
 *
 * Clicking anywhere outside the open list closes it without changing the selection.
 *
 * @param selected The currently chosen value, or `null` to show [placeholder].
 * @param onSelected Invoked with the newly picked value. The list closes itself either way.
 * @param enabled When `false` the trigger is dead and the list cannot be opened.
 */
@Composable
fun <T> Dropdown(
    options: List<DropdownOption<T>>,
    selected: T?,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: Component = Component.literal("Select..."),
    enabled: Boolean = true,
    width: Int = 120,
    maxVisibleOptions: Int = 5,
) {
    var expanded by remember { mutableStateOf(false) }
    var anchor by remember { mutableStateOf(IntCoordinates(0, 0)) }
    var triggerHeight by remember { mutableStateOf(0) }
    val layers = LocalLayerManagerOrNull.current

    // A dropdown whose options vanished must not keep claiming one is open.
    if (options.isEmpty() && expanded) expanded = false

    val selectedLabel = options.firstOrNull { it.value == selected }?.label ?: placeholder

    // Read inside the layer's own composition, so the hosted list tracks changes to these rather
    // than freezing whatever they happened to be when the layer was pushed.
    val currentOptions by rememberUpdatedState(options)
    val currentSelected by rememberUpdatedState(selected)
    val currentOnSelected by rememberUpdatedState(onSelected)

    DisposableEffect(expanded, layers) {
        if (!expanded || layers == null) return@DisposableEffect onDispose { }
        val dismiss = layers.push {
            DropdownOverlay(
                anchor = anchor,
                triggerHeight = triggerHeight,
                width = width,
                maxVisibleOptions = maxVisibleOptions,
                options = currentOptions,
                selected = currentSelected,
                onPicked = { picked ->
                    expanded = false
                    if (picked != currentSelected) currentOnSelected(picked)
                },
                onDismiss = { expanded = false },
            )
        }
        // Covers both closing and the whole dropdown leaving composition - a layer outliving its
        // owner would be an orphan nothing can close.
        onDispose { dismiss() }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(2),
        modifier = Modifier.zIndex(if (expanded && layers == null) EXPANDED_Z else 0f).then(modifier),
    ) {
        Button(
            onClick = { if (options.isNotEmpty()) expanded = !expanded },
            enabled = enabled && options.isNotEmpty(),
            modifier = Modifier
                .width(width)
                .onGloballyPositioned { anchor = it }
                .onSizeChanged { triggerHeight = it.height },
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.width(width - 8),
            ) {
                Text(selectedLabel, dropShadow = false)
                Text(Component.literal(if (expanded) EXPANDED_GLYPH else COLLAPSED_GLYPH), dropShadow = false)
            }
        }

        // Inline fallback only - with a layer manager present the list lives in the overlay above.
        if (expanded && layers == null) {
            DropdownList(
                options = options,
                selected = selected,
                width = width,
                maxVisibleOptions = maxVisibleOptions,
                onPicked = { picked ->
                    expanded = false
                    if (picked != selected) onSelected(picked)
                },
            )
        }
    }
}

/**
 * The layer contents: a full-screen catcher that closes on any click through it, with the option
 * list positioned under the trigger.
 *
 * The catcher is a sibling *behind* the list rather than a wrapper around it, so a click on an
 * option is consumed by the option and never reaches the catcher - input is dispatched to children
 * before their parent, and the first handler to consume ends it.
 *
 * Pushed as a plain layer rather than a modal on purpose. A modal paints a dimming backdrop over
 * everything beneath it, which is right for a dialog and quite wrong for a list of four entries -
 * the screen would darken every time someone opened a dropdown. A plain layer draws only what it
 * contains.
 *
 * It is also deliberately not wrapped in a
 * [net.kernelpanicsoft.archie.gui.composables.containers.RootContainer]:
 * [net.kernelpanicsoft.archie.gui.layer.LayerStackManager.screenPos] and `screenSize` fold over
 * every layer's root container to decide a container screen's origin, and a full-screen overlay
 * joining that fold would drag the screen's own origin - and the slot clipping derived from it -
 * out to the window edges.
 */
@Composable
private fun <T> DropdownOverlay(
    anchor: IntCoordinates,
    triggerHeight: Int,
    width: Int,
    maxVisibleOptions: Int,
    options: List<DropdownOption<T>>,
    selected: T?,
    onPicked: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onPointerEvent<UINode>(PointerEventType.PRESS) { _, event ->
                onDismiss()
                event.consume()
            },
        contentAlignment = Alignment.TopStart,
    ) {
        Box(modifier = Modifier.offset(x = anchor.x, y = anchor.y + triggerHeight)) {
            DropdownList(
                options = options,
                selected = selected,
                width = width,
                maxVisibleOptions = maxVisibleOptions,
                onPicked = onPicked,
            )
        }
    }
}

/** The option list itself, shared by the layer-hosted and inline paths so the two cannot drift apart. */
@Composable
private fun <T> DropdownList(
    options: List<DropdownOption<T>>,
    selected: T?,
    width: Int,
    maxVisibleOptions: Int,
    onPicked: (T) -> Unit,
) {
    val theme = LocalTheme.current
    val listHeight = minOf(options.size, maxVisibleOptions) * OPTION_HEIGHT

    Panel(variant = "slot") {
        Scrollable(modifier = Modifier.width(width).height(listHeight)) {
            Column {
                for ((value, label, enabled) in options) {
                    Button(
                        onClick = { onPicked(value) },
                        texture = "surface",
                        variant = "slot_inverted",
                        enabled = enabled,
                        modifier = Modifier.width(width - 6),
                    ) {
                        Text(
	                        label,
                            dropShadow = false,
                            color = if (value == selected) theme.darkTextColor else theme.lightTextColor,
                        )
                    }
                }
            }
        }
    }
}
