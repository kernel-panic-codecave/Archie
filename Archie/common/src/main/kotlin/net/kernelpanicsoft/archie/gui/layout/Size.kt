package net.kernelpanicsoft.archie.gui.layout

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class Size(
	val width: Int = 0,
	val height: Int = 0
)