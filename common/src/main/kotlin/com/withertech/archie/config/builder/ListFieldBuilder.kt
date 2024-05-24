package com.withertech.archie.config.builder

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder
import me.shedaniel.clothconfig2.api.ModifierKeyCode
import me.shedaniel.clothconfig2.gui.entries.NestedListListEntry
import me.shedaniel.clothconfig2.impl.builders.AbstractFieldBuilder
import me.shedaniel.clothconfig2.impl.builders.AbstractListBuilder
import me.shedaniel.clothconfig2.impl.builders.DropdownMenuBuilder
import me.shedaniel.clothconfig2.impl.builders.FieldBuilder
import me.shedaniel.clothconfig2.impl.builders.KeyCodeBuilder
import net.minecraft.network.chat.Component
import java.util.*
import java.util.function.Supplier
import kotlin.jvm.optionals.getOrNull

abstract class ListFieldBuilder<T, A : AbstractConfigListEntry<T>, SELF : ListFieldBuilder<T, A, SELF>>(
	resetButtonKey: Component,
	fieldNameKey: Component,
	value: List<T>
) : AbstractListBuilder<T, NestedListListEntry<T, A>, SELF>(
	resetButtonKey, fieldNameKey
)
{
	init
	{
		this.value = value
	}

	abstract fun factory(): T

	abstract fun ConfigEntryBuilder.builder(title: Component, value: T, list: NestedListListEntry<T, A>): FieldBuilder<T, A, *>

	override fun build(): NestedListListEntry<T, A>
	{
		val entryBuilder: ConfigEntryBuilder = ConfigEntryBuilder.create()
		@Suppress("UnstableApiUsage")
		val entry = NestedListListEntry(
			fieldNameKey,
			value,
			isExpanded,
			null,
			saveConsumer,
			defaultValue,
			resetButtonKey,
			isDeleteButtonEnabled,
			isInsertInFront
		) { entryNullable: T?, list: NestedListListEntry<T, A> ->
			val entry = entryNullable ?: factory()
			entryBuilder.builder(Component.literal("Entry"), entry, list).apply {
				when (this)
				{
					is AbstractFieldBuilder ->
					{
						setErrorSupplier { cellValue ->
							Optional.ofNullable(cellErrorSupplier?.apply(cellValue)?.getOrNull())
						}
						setDefaultValue {
							factory()
						}
					}

					is DropdownMenuBuilder ->
					{
						setErrorSupplier { cellValue ->
							Optional.ofNullable(cellErrorSupplier?.apply(cellValue)?.getOrNull())
						}
						setDefaultValue {
							factory()
						}
					}

					is KeyCodeBuilder ->
					{
						setModifierErrorSupplier { cellValue ->
							@Suppress("UNCHECKED_CAST")
							Optional.ofNullable(cellErrorSupplier?.apply(cellValue as T)?.getOrNull())
						}
						setModifierDefaultValue {
							factory() as ModifierKeyCode
						}
					}
				}
				requireRestart(this@ListFieldBuilder.isRequireRestart)
				setRequirement(this@ListFieldBuilder.enableRequirement)
				setDisplayRequirement(this@ListFieldBuilder.displayRequirement)
			}.build()
		}
		entry.setTooltipSupplier {
			tooltipSupplier.apply(entry.value)
		}
		entry.setErrorSupplier {
			Optional.ofNullable(errorSupplier?.apply(entry.value)?.getOrNull())
		}
		return entry
	}
}