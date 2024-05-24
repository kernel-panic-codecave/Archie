package com.withertech.archie.config.builder

import com.withertech.archie.util.MutableEntry
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder
import me.shedaniel.clothconfig2.gui.entries.FloatListEntry
import me.shedaniel.clothconfig2.gui.entries.MultiElementListEntry
import me.shedaniel.clothconfig2.gui.entries.NestedListListEntry
import me.shedaniel.clothconfig2.impl.builders.AbstractFieldBuilder
import net.minecraft.network.chat.Component

class FloatMapBuilder(resetButtonKey: Component, fieldNameKey: Component, value: Map<String, Float>) : MapFieldBuilder<Float, FloatListEntry, FloatMapBuilder>(
	resetButtonKey, fieldNameKey, value
)
{
	override fun valueFactory(): Float = 0.0f

	override fun ConfigEntryBuilder.valueBuilder(
		title: Component,
		value: Float,
		list: NestedListListEntry<MutableEntry<String, Float>, MultiElementListEntry<MutableEntry<String, Float>>>
	): AbstractFieldBuilder<Float, FloatListEntry, *>
	{
		return startFloatField(title, value)
	}
}