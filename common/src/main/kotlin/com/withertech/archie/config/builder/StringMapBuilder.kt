package com.withertech.archie.config.builder

import com.withertech.archie.util.MutableEntry
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder
import me.shedaniel.clothconfig2.gui.entries.MultiElementListEntry
import me.shedaniel.clothconfig2.gui.entries.NestedListListEntry
import me.shedaniel.clothconfig2.gui.entries.StringListEntry
import me.shedaniel.clothconfig2.impl.builders.AbstractFieldBuilder
import net.minecraft.network.chat.Component

class StringMapBuilder(resetButtonKey: Component, fieldNameKey: Component, value: Map<String, String>) : MapFieldBuilder<String, StringListEntry, StringMapBuilder>(
	resetButtonKey, fieldNameKey, value
)
{
	override fun valueFactory(): String = ""

	override fun ConfigEntryBuilder.valueBuilder(
		title: Component,
		value: String,
		list: NestedListListEntry<MutableEntry<String, String>, MultiElementListEntry<MutableEntry<String, String>>>
	): AbstractFieldBuilder<String, StringListEntry, *>
	{
		return startStrField(title, value)
	}
}