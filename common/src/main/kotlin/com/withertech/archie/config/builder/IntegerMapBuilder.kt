package com.withertech.archie.config.builder

import com.withertech.archie.util.MutableEntry
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder
import me.shedaniel.clothconfig2.gui.entries.IntegerListEntry
import me.shedaniel.clothconfig2.gui.entries.MultiElementListEntry
import me.shedaniel.clothconfig2.gui.entries.NestedListListEntry
import me.shedaniel.clothconfig2.impl.builders.AbstractFieldBuilder
import net.minecraft.network.chat.Component

class IntegerMapBuilder(resetButtonKey: Component, fieldNameKey: Component, value: Map<String, Int>) : MapFieldBuilder<Int, IntegerListEntry, IntegerMapBuilder>(
	resetButtonKey, fieldNameKey, value
)
{
	override fun valueFactory(): Int = 0

	override fun ConfigEntryBuilder.valueBuilder(
		title: Component,
		value: Int,
		list: NestedListListEntry<MutableEntry<String, Int>, MultiElementListEntry<MutableEntry<String, Int>>>
	): AbstractFieldBuilder<Int, IntegerListEntry, *>
	{
		return startIntField(title, value)
	}
}