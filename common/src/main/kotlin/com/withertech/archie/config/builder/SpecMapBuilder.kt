package com.withertech.archie.config.builder

import com.withertech.archie.config.CategorySpec
import com.withertech.archie.util.MutableEntry
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder
import me.shedaniel.clothconfig2.gui.entries.MultiElementListEntry
import me.shedaniel.clothconfig2.gui.entries.NestedListListEntry
import me.shedaniel.clothconfig2.impl.builders.AbstractFieldBuilder
import net.minecraft.network.chat.Component

class SpecMapBuilder<T : CategorySpec>(
	resetButtonKey: Component,
	fieldNameKey: Component,
	value: Map<String, T>,
	private val valueFactory: () -> T
) : MapFieldBuilder<T, MultiElementListEntry<T>, SpecMapBuilder<T>>(
	resetButtonKey, fieldNameKey, value
)
{
	override fun valueFactory(): T = this.valueFactory.invoke()

	override fun ConfigEntryBuilder.valueBuilder(
		title: Component,
		value: T,
		list: NestedListListEntry<MutableEntry<String, T>, MultiElementListEntry<MutableEntry<String, T>>>
	): AbstractFieldBuilder<T, MultiElementListEntry<T>, *>
	{
		return startSpecField(title, value).also { it.isExpanded = list.isExpanded }
	}
}