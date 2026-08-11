package net.kernelpanicsoft.archie.config.builder

import net.kernelpanicsoft.archie.config.DataSpec
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder
import me.shedaniel.clothconfig2.gui.entries.MultiElementListEntry
import me.shedaniel.clothconfig2.impl.builders.AbstractFieldBuilder
import net.minecraft.network.chat.Component
import java.util.*
import java.util.function.Supplier
import kotlin.jvm.optionals.getOrNull

/**
 * Cloth Config builder for a nested [DataSpec] field, rendered as a collapsible group
 * containing [value]'s own fields and subcategories (via [DataSpec.client]).
 */
class SpecFieldBuilder<T : DataSpec>(
	resetButtonKey: Component,
	fieldNameKey: Component,
	value: T
) : AbstractFieldBuilder<T, MultiElementListEntry<T>, SpecFieldBuilder<T>>(
	resetButtonKey, fieldNameKey
)
{
	/** Whether the group starts expanded in the UI. */
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
				value.client.builders.forEach { builder ->
					add(entryBuilder.builder())
				}

//				value.subcategories.forEach { cat ->
//					add(cat.client.buildSub(entryBuilder))
//				}
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