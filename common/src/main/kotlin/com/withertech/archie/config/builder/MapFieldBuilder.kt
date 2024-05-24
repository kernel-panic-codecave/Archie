package com.withertech.archie.config.builder

import com.withertech.archie.util.MutableEntry
import com.withertech.archie.util.toMutableEntry
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder
import me.shedaniel.clothconfig2.api.ModifierKeyCode
import me.shedaniel.clothconfig2.gui.entries.MultiElementListEntry
import me.shedaniel.clothconfig2.gui.entries.NestedListListEntry
import me.shedaniel.clothconfig2.gui.entries.StringListEntry
import me.shedaniel.clothconfig2.impl.builders.AbstractFieldBuilder
import me.shedaniel.clothconfig2.impl.builders.AbstractListBuilder
import me.shedaniel.clothconfig2.impl.builders.DropdownMenuBuilder
import me.shedaniel.clothconfig2.impl.builders.FieldBuilder
import me.shedaniel.clothconfig2.impl.builders.KeyCodeBuilder
import net.minecraft.network.chat.Component
import java.util.*
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Supplier
import kotlin.jvm.optionals.getOrNull

abstract class MapFieldBuilder<T, A : AbstractConfigListEntry<T>, SELF : MapFieldBuilder<T, A, SELF>>(
	resetButtonKey: Component,
	fieldNameKey: Component,
	value: Map<String, T>
) :
	AbstractListBuilder<MutableEntry<String, T>, NestedListListEntry<MutableEntry<String, T>, MultiElementListEntry<MutableEntry<String, T>>>, SELF>(
		resetButtonKey,
		fieldNameKey
	)
{

	open var keyErrorSupplier: ((String) -> Optional<Component>)? = null
	open var valueErrorSupplier: ((T) -> Optional<Component>)? = null
	open var valueTooltipSupplier: ((T) -> Optional<Array<Component>>)? = null

	init
	{
		this.value = value.entries.toList().map(Map.Entry<String, T>::toMutableEntry)
	}


	abstract fun valueFactory(): T

	abstract fun ConfigEntryBuilder.valueBuilder(title: Component, value: T, list: NestedListListEntry<MutableEntry<String, T>, MultiElementListEntry<MutableEntry<String, T>>>): FieldBuilder<T, A, *>

	override fun build(): NestedListListEntry<MutableEntry<String, T>, MultiElementListEntry<MutableEntry<String, T>>>
	{
		val entryBuilder: ConfigEntryBuilder = ConfigEntryBuilder.create()

		val fields: MutableList<StringListEntry> = mutableListOf()

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
		) { entryNullable: MutableEntry<String, T>?, list: NestedListListEntry<MutableEntry<String, T>, MultiElementListEntry<MutableEntry<String, T>>> ->
			val entry: MutableEntry<String, T> = entryNullable ?: ("" to valueFactory()).toMutableEntry()
			val cell = MultiElementListEntry(
				Component.literal("Entry"),
				entry,
				buildList {
					add(entryBuilder.startStrField(Component.literal("Key"), entry.key).apply {
						saveConsumer = Consumer { key ->
							entryNullable?.key = key
							entry.key = key
						}
						setErrorSupplier { entryKey ->
							Optional.ofNullable(keyErrorSupplier?.invoke(entryKey)?.getOrNull()).or {
								if (fields.count {
										it.value == entryKey
									} > 1)
									Optional.of(Component.literal("Duplicate Key: $entryKey"))
								else
									Optional.empty()
							}
						}
						requireRestart(this@MapFieldBuilder.requireRestart)
						setRequirement(this@MapFieldBuilder.enableRequirement)
						setDisplayRequirement(this@MapFieldBuilder.displayRequirement)
					}.build().also {
						fields.add(it)
					})

					add(entryBuilder.valueBuilder(Component.literal("Value"), entry.value, list).apply {
						when (this)
						{
							is AbstractFieldBuilder ->
							{
								setSaveConsumer { value ->
									entryNullable?.value = value
									entry.value = value
								}
								setErrorSupplier { entryValue ->
									Optional.ofNullable(valueErrorSupplier?.invoke(entryValue)?.getOrNull())
								}
								setTooltipSupplier { entryValue ->
									Optional.ofNullable(valueTooltipSupplier?.invoke(entryValue)?.getOrNull())
								}
								setDefaultValue {
									valueFactory()
								}
							}

							is DropdownMenuBuilder ->
							{
								setSaveConsumer { value ->
									entryNullable?.value = value
									entry.value = value
								}
								setErrorSupplier { entryValue ->
									Optional.ofNullable(valueErrorSupplier?.invoke(entryValue)?.getOrNull())
								}
								setTooltipSupplier { entryValue ->
									Optional.ofNullable(valueTooltipSupplier?.invoke(entryValue)?.getOrNull())
								}
								setDefaultValue {
									valueFactory()
								}
							}

							is KeyCodeBuilder ->
							{
								setModifierSaveConsumer { value ->
									@Suppress("UNCHECKED_CAST")
									entryNullable?.value = value as T
									entry.value = value
								}
								setModifierErrorSupplier { entryValue ->
									@Suppress("UNCHECKED_CAST")
									Optional.ofNullable(valueErrorSupplier?.invoke(entryValue as T)?.getOrNull())
								}
								setModifierTooltipSupplier { entryValue ->
									@Suppress("UNCHECKED_CAST")
									Optional.ofNullable(valueTooltipSupplier?.invoke(entryValue as T)?.getOrNull())
								}
								setModifierDefaultValue {
									valueFactory() as ModifierKeyCode
								}
							}
						}
						requireRestart(this@MapFieldBuilder.requireRestart)
						setRequirement(this@MapFieldBuilder.enableRequirement)
						setDisplayRequirement(this@MapFieldBuilder.displayRequirement)
					}.build())
				},
				list.isExpanded
			)
			cell.setErrorSupplier {
				Optional.ofNullable(cellErrorSupplier?.apply(cell.value)?.getOrNull())
			}
			cell
		}
		entry.setTooltipSupplier {
			tooltipSupplier.apply(entry.value)
		}
		entry.setErrorSupplier {
			Optional.ofNullable(errorSupplier?.apply(entry.value)?.getOrNull())
		}
		return finishBuilding(entry)
	}


}