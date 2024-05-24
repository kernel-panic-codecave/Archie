package com.withertech.archie.config

import me.shedaniel.clothconfig2.api.ConfigEntryBuilder
import me.shedaniel.clothconfig2.gui.entries.TextListEntry
import java.util.*

fun String.toSnakeCase() =
	split(" ")
		.joinToString("") { word ->
			word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
		}
		.replace(humps, "_").lowercase()

private val humps = "(?<=.)(?=\\p{Upper})".toRegex()

typealias Comment = Pair<String, ConfigEntryBuilder.() -> TextListEntry>