package com.withertech.archie.config.builder

import me.shedaniel.clothconfig2.api.ConfigEntryBuilder
import me.shedaniel.clothconfig2.gui.entries.ColorEntry
import me.shedaniel.clothconfig2.gui.entries.NestedListListEntry
import me.shedaniel.clothconfig2.impl.builders.FieldBuilder
import me.shedaniel.math.Color
import net.minecraft.network.chat.Component

@Suppress("MemberVisibilityCanBePrivate", "unused")
class ColorListBuilder(
	resetButtonKey: Component,
	fieldNameKey: Component,
	value: List<Color>,
	private val factory: () -> Color
) :
	ListFieldBuilder<Int, ColorEntry, ColorListBuilder>(
		resetButtonKey,
		fieldNameKey,
		value.map { it.color }
	)
{
	var alphaMode: Boolean = false

	override fun factory(): Int = factory.invoke().color

	override fun ConfigEntryBuilder.builder(
		title: Component,
		value: Int,
		list: NestedListListEntry<Int, ColorEntry>
	): FieldBuilder<Int, ColorEntry, *>
	{
		return startColorField(title, value)
			.setAlphaMode(alphaMode)
	}

}