package com.withertech.archie.config.builder

import com.withertech.archie.util.MutableEntry
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder
import me.shedaniel.clothconfig2.gui.entries.DropdownBoxEntry
import me.shedaniel.clothconfig2.gui.entries.MultiElementListEntry
import me.shedaniel.clothconfig2.gui.entries.NestedListListEntry
import me.shedaniel.clothconfig2.impl.builders.FieldBuilder
import net.minecraft.core.Registry
import net.minecraft.network.chat.Component
import kotlin.reflect.KClass

class RegistryMapBuilder<T : Any, R : T>(
	resetButtonKey: Component,
	fieldNameKey: Component,
	value: Map<String, T>,
	private val factory: () -> T,
	private val subclass: KClass<R>? = null,
	private val registry: Registry<T>
) :
	MapFieldBuilder<T, DropdownBoxEntry<T>, RegistryMapBuilder<T, R>>(
		resetButtonKey, fieldNameKey, value
	)
{
	override fun valueFactory(): T = this.factory.invoke()

	override fun ConfigEntryBuilder.valueBuilder(
		title: Component,
		value: T,
		list: NestedListListEntry<MutableEntry<String, T>, MultiElementListEntry<MutableEntry<String, T>>>
	): FieldBuilder<T, DropdownBoxEntry<T>, *>
	{
		return startRegistryField(title, value, subclass, registry)
	}
}