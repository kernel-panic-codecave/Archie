package net.kernelpanicsoft.archie.config.builder

import me.shedaniel.clothconfig2.gui.entries.DropdownBoxEntry
import me.shedaniel.clothconfig2.impl.builders.AbstractFieldBuilder
import me.shedaniel.clothconfig2.impl.builders.DropdownMenuBuilder
import me.shedaniel.clothconfig2.impl.builders.DropdownMenuBuilder.CellCreatorBuilder
import me.shedaniel.clothconfig2.impl.builders.DropdownMenuBuilder.TopCellElementBuilder
import net.minecraft.network.chat.Component

/**
 * Cloth Config builder for a dropdown/autocomplete field over [selections]. Set [toObjectFunction]
 * to parse free-typed text back into a `T` (required when [suggestionMode] is enabled); override
 * [toTextFunction] to customize how values are displayed.
 */
open class DropdownFieldBuilder<T : Any>(
	resetButtonKey: Component,
	fieldNameKey: Component,
	private val value: T,
	open var selections: Iterable<T> = emptyList()
) : AbstractFieldBuilder<T, DropdownBoxEntry<T>, DropdownFieldBuilder<T>>(
	resetButtonKey, fieldNameKey
)
{
	open lateinit var toObjectFunction: (String) -> T
	open var toTextFunction: (T) -> Component = { Component.literal(it.toString()) }
	open var suggestionMode: Boolean = true

	override fun build(): DropdownBoxEntry<T>
	{
		val entry = DropdownMenuBuilder(
			resetButtonKey,
			fieldNameKey,
			TopCellElementBuilder.of(value, toObjectFunction, toTextFunction),
			CellCreatorBuilder.of(toTextFunction)
		)
		entry.setSuggestionMode(suggestionMode)
		entry.setSelections(selections)

		entry.setSaveConsumer(saveConsumer)
		entry.setErrorSupplier(errorSupplier)
		entry.setTooltipSupplier(tooltipSupplier)
		entry.setDefaultValue(defaultValue)
		entry.requireRestart(requireRestart)
		return finishBuilding(entry.build())
	}
}