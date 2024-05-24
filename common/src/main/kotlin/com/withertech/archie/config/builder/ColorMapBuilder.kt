package com.withertech.archie.config.builder

import com.withertech.archie.util.MutableEntry
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder
import me.shedaniel.clothconfig2.gui.entries.ColorEntry
import me.shedaniel.clothconfig2.gui.entries.MultiElementListEntry
import me.shedaniel.clothconfig2.gui.entries.NestedListListEntry
import me.shedaniel.clothconfig2.impl.builders.FieldBuilder
import me.shedaniel.math.Color
import net.minecraft.network.chat.Component

@Suppress("MemberVisibilityCanBePrivate", "unused")
class ColorMapBuilder(
	resetButtonKey: Component,
	fieldNameKey: Component,
	value: Map<String, Color>,
	private val factory: () -> Color
) :
	MapFieldBuilder<Int, ColorEntry, ColorMapBuilder>(
		resetButtonKey,
		fieldNameKey,
		value.mapValues { it.value.color }
	)
{
	var alphaMode: Boolean = false

	override fun valueFactory(): Int = factory.invoke().color

	override fun ConfigEntryBuilder.valueBuilder(
		title: Component,
		value: Int,
		list: NestedListListEntry<MutableEntry<String, Int>, MultiElementListEntry<MutableEntry<String, Int>>>
	): FieldBuilder<Int, ColorEntry, *>
	{
		return startColorField(title, value)
			.setAlphaMode(alphaMode)
	}

}