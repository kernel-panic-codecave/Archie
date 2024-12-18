package com.withertech.archie.gui

import kotlinx.coroutines.CoroutineScope

object AUIScopeManager {
	val scopes = mutableSetOf<CoroutineScope>()
}