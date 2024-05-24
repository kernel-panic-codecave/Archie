package com.withertech.archie.config.builder

import com.withertech.archie.config.CategorySpec
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder
import me.shedaniel.clothconfig2.gui.entries.MultiElementListEntry
import me.shedaniel.clothconfig2.impl.builders.AbstractFieldBuilder
import net.minecraft.network.chat.Component
import java.util.*
import java.util.function.Supplier
import kotlin.jvm.optionals.getOrNull

class SpecFieldBuilder<T : CategorySpec>(
	resetButtonKey: Component,
	fieldNameKey: Component,
	value: T
) : AbstractFieldBuilder<T, MultiElementListEntry<T>, SpecFieldBuilder<T>>(
	resetButtonKey, fieldNameKey
)
{
	var isExpanded: Boolean = false
	init
	{
		this.value = value
	}
	@Suppress("UnstableApiUsage")
	override fun build(): MultiElementListEntry<T>
	{
		val entryBuilder: ConfigEntryBuilder = ConfigEntryBuilder.create()
		val entry = MultiElementListEntry(
			fieldNameKey,
			value,
			buildList {
				value.builders.forEach { builder ->
					add(entryBuilder.builder())
				}

				value.subcategories.forEach { cat ->
					add(cat.buildSub(entryBuilder))
				}
			},
			isExpanded
		)
		entry.tooltipSupplier = Supplier {
			tooltipSupplier.apply(entry.value)
		}
		entry.setErrorSupplier {
			Optional.ofNullable(errorSupplier?.apply(entry.value)?.getOrNull())
		}
		return finishBuilding(entry)
	}
}