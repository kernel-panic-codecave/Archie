package net.kernelpanicsoft.archie.gui

import kotlinx.coroutines.CoroutineScope

/**
 * Global registry of active [CoroutineScope]s created by open GUI screens.
 *
 * Each [ComposeScreen] or [ComposeContainerScreen] registers its `composeScope` here on
 * startup so that external systems (e.g. the event bus) can broadcast work to all live
 * GUI coroutines without holding direct references to individual screens.
 *
 * Scopes are removed automatically when their screen closes.
 */
object AUIScopeManager {
    /** The set of all currently active GUI coroutine scopes. */
    val scopes = mutableSetOf<CoroutineScope>()
}