package net.kernelpanicsoft.archie.gui

import kotlinx.coroutines.CoroutineScope

object AUIScopeManager {
	val scopes = mutableSetOf<CoroutineScope>()
}