package com.withertech.archie.config.builder

import me.shedaniel.clothconfig2.api.ConfigEntryBuilder
import me.shedaniel.clothconfig2.gui.entries.DropdownBoxEntry
import me.shedaniel.clothconfig2.gui.entries.NestedListListEntry
import me.shedaniel.clothconfig2.impl.builders.FieldBuilder
import net.minecraft.core.Registry
import net.minecraft.network.chat.Component
import kotlin.reflect.KClass

class RegistryListBuilder<T : Any, R : T>(
	resetButtonKey: Component,
	fieldNameKey: Component,
	value: List<T>,
	private val factory: () -> T,
	private val subclass: KClass<R>? = null,
	private val registry: Registry<T>
) :
	ListFieldBuilder<T, DropdownBoxEntry<T>, RegistryListBuilder<T, R>>(
		resetButtonKey, fieldNameKey, value
	)
{
	override fun factory(): T = this.factory.invoke()

	override fun ConfigEntryBuilder.builder(
		title: Component,
		value: T,
		list: NestedListListEntry<T, DropdownBoxEntry<T>>
	): FieldBuilder<T, DropdownBoxEntry<T>, *>
	{
		return startRegistryField(title, value, subclass, registry)
	}
}