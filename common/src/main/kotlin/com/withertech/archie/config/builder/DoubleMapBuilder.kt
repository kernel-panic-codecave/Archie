package com.withertech.archie.config.builder

import com.withertech.archie.util.MutableEntry
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder
import me.shedaniel.clothconfig2.gui.entries.DoubleListEntry
import me.shedaniel.clothconfig2.gui.entries.MultiElementListEntry
import me.shedaniel.clothconfig2.gui.entries.NestedListListEntry
import me.shedaniel.clothconfig2.impl.builders.AbstractFieldBuilder
import net.minecraft.network.chat.Component

class DoubleMapBuilder(resetButtonKey: Component, fieldNameKey: Component, value: Map<String, Double>) : MapFieldBuilder<Double, DoubleListEntry, DoubleMapBuilder>(
	resetButtonKey, fieldNameKey, value
)
{
	override fun valueFactory(): Double = 0.0

	override fun ConfigEntryBuilder.valueBuilder(
		title: Component,
		value: Double,
		list: NestedListListEntry<MutableEntry<String, Double>, MultiElementListEntry<MutableEntry<String, Double>>>
	): AbstractFieldBuilder<Double, DoubleListEntry, *>
	{
		return startDoubleField(title, value)
	}
}