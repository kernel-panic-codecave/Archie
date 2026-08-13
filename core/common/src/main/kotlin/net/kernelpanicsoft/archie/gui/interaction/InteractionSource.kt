package net.kernelpanicsoft.archie.gui.interaction

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * A read-only stream of [Interaction]s a component emits about itself (press, hover, focus,
 * drag). Several independent pieces of UI - the widget's own visuals, a shared "indication"
 * effect, a test assertion - can all observe the same interaction state this way, instead of
 * each hand-rolling its own separate hovered/pressed/focused booleans.
 */
interface InteractionSource {
    val interactions: Flow<Interaction>
}

/** An [InteractionSource] that can also emit new [Interaction]s into itself. */
interface MutableInteractionSource : InteractionSource {
    /** Emits [interaction] without suspending. Never fails to enqueue - see [MutableInteractionSourceImpl]. */
    fun tryEmit(interaction: Interaction): Boolean
}

/** Creates a new, independent [MutableInteractionSource]. */
fun MutableInteractionSource(): MutableInteractionSource = MutableInteractionSourceImpl()

private class MutableInteractionSourceImpl : MutableInteractionSource {
    // DROP_OLDEST (not the default SUSPEND, which makes a non-suspending tryEmit() on a full
    // buffer fail and silently drop the *newest* interaction instead) - a state-defining
    // interaction like FocusInteraction.Focus/Unfocus has to never go missing, since
    // collectIsFocusedAsState() only reduces off whatever interactions it actually receives.
    // A missed Unfocus is exactly "sometimes Tab-focusing doesn't visually update" (a stale
    // collector - e.g. rapid Tab presses outrunning the Recomposer's own dispatch - could
    // previously fill the 16-slot buffer and start rejecting new emissions outright). Losing a
    // stale buffered event this way is harmless since only the latest of each interaction type
    // ever matters to the reducer; losing the newest one wasn't.
    private val flow = MutableSharedFlow<Interaction>(extraBufferCapacity = 16, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    override val interactions: Flow<Interaction> = flow
    override fun tryEmit(interaction: Interaction): Boolean = flow.tryEmit(interaction)
}

/** Subscribes to [InteractionSource.interactions], reducing matching interactions to a [State]. */
@Composable
private fun <T> InteractionSource.collectAsState(initial: T, reduce: (T, Interaction) -> T): State<T> {
    val state = remember(this) { mutableStateOf(initial) }
    LaunchedEffect(this) {
        interactions.collect { interaction -> state.value = reduce(state.value, interaction) }
    }
    return state
}

/** `true` while a [PressInteraction.Press] is active (until its [PressInteraction.Release]/[PressInteraction.Cancel]). */
@Composable
fun InteractionSource.collectIsPressedAsState(): State<Boolean> = collectAsState(false) { current, interaction ->
    when (interaction) {
        is PressInteraction.Press -> true
        is PressInteraction.Release, is PressInteraction.Cancel -> false
        else -> current
    }
}

/** `true` while the pointer is hovering the component. */
@Composable
fun InteractionSource.collectIsHoveredAsState(): State<Boolean> = collectAsState(false) { current, interaction ->
    when (interaction) {
        is HoverInteraction.Enter -> true
        is HoverInteraction.Exit -> false
        else -> current
    }
}

/** `true` while the component holds vanilla keyboard/controller focus. */
@Composable
fun InteractionSource.collectIsFocusedAsState(): State<Boolean> = collectAsState(false) { current, interaction ->
    when (interaction) {
        is FocusInteraction.Focus -> true
        is FocusInteraction.Unfocus -> false
        else -> current
    }
}

/** `true` while a drag is active (until its [DragInteraction.Stop]/[DragInteraction.Cancel]). */
@Composable
fun InteractionSource.collectIsDraggedAsState(): State<Boolean> = collectAsState(false) { current, interaction ->
    when (interaction) {
        is DragInteraction.Start -> true
        is DragInteraction.Stop, is DragInteraction.Cancel -> false
        else -> current
    }
}
