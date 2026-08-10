package net.kernelpanicsoft.archie.config

import me.shedaniel.clothconfig2.api.ConfigEntryBuilder
import me.shedaniel.clothconfig2.gui.entries.TextListEntry
import java.util.*

/**
 * Converts this string to `snake_case`, splitting on both spaces and camelCase humps. Used to
 * derive field/category ids from titles and delegated property names (e.g. `"Max Items"` and
 * `maxItems` both become `max_items`).
 */
fun String.toSnakeCase() =
	split(" ")
		.joinToString("") { word ->
			word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
		}
		.replace(humps, "_").lowercase()

private val humps = "(?<=.)(?=\\p{Upper})".toRegex()

/** A comment-list entry id paired with a factory for the Cloth Config [TextListEntry] it renders as. */
typealias Comment = Pair<String, ConfigEntryBuilder.() -> TextListEntry>