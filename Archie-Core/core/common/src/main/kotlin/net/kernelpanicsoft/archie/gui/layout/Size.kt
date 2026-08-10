package net.kernelpanicsoft.archie.gui.layout

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * An integer width/height pair. Unlike [IntSize], this is a regular [data class][Size] (not an
 * inline value class), which makes it convenient where a boxed, nullable, or default-constructed
 * size is needed, e.g. component configuration.
 */
@Immutable
@Serializable
data class Size(
	val width: Int = 0,
	val height: Int = 0
)