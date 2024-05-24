package com.withertech.archie.config.builder

import com.withertech.archie.config.CategorySpec
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder
import me.shedaniel.clothconfig2.gui.entries.MultiElementListEntry
import me.shedaniel.clothconfig2.gui.entries.NestedListListEntry
import me.shedaniel.clothconfig2.impl.builders.AbstractFieldBuilder
import net.minecraft.network.chat.Component

class SpecListBuilder<T : CategorySpec>(
	resetButtonKey: Component,
	fieldNameKey: Component,
	value: List<T>,
	private val factory: () -> T
) : ListFieldBuilder<T, MultiElementListEntry<T>, SpecListBuilder<T>>(
	resetButtonKey, fieldNameKey, value
)
{
	override fun factory(): T = this.factory.invoke()

	override fun ConfigEntryBuilder.builder(
		title: Component,
		value: T,
		list: NestedListListEntry<T, MultiElementListEntry<T>>
	): AbstractFieldBuilder<T, MultiElementListEntry<T>, *>
	{
		return startSpecField(title, value).also { it.isExpanded = list.isExpanded }
	}
}