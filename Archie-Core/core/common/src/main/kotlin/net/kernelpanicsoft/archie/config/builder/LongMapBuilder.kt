package net.kernelpanicsoft.archie.config.builder

import net.kernelpanicsoft.archie.util.MutableEntry
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder
import me.shedaniel.clothconfig2.gui.entries.LongListEntry
import me.shedaniel.clothconfig2.gui.entries.MultiElementListEntry
import me.shedaniel.clothconfig2.gui.entries.NestedListListEntry
import me.shedaniel.clothconfig2.impl.builders.AbstractFieldBuilder
import net.minecraft.network.chat.Component

/** [MapFieldBuilder] for `Long` values, using Cloth Config's `startLongField` per row. */
class LongMapBuilder(resetButtonKey: Component, fieldNameKey: Component, value: Map<String, Long>) : MapFieldBuilder<Long, LongListEntry, LongMapBuilder>(
	resetButtonKey, fieldNameKey, value
)
{
	override fun valueFactory(): Long = 0

	override fun ConfigEntryBuilder.valueBuilder(
		title: Component,
		value: Long,
		list: NestedListListEntry<MutableEntry<String, Long>, MultiElementListEntry<MutableEntry<String, Long>>>
	): AbstractFieldBuilder<Long, LongListEntry, *>
	{
		return startLongField(title, value)
	}
}