package net.kernelpanicsoft.archie.config.builder

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

/**
 * Base Cloth Config builder for a list field, rendered as an editable list of rows sharing one
 * element type. Concrete subclasses (e.g. [KeycodeListBuilder]) only need to implement [factory]
 * (the default for a newly-inserted row) and [builder] (the field builder for each row); this
 * class handles add/remove wiring and error/tooltip propagation.
 */
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

	/** Value assigned to a row inserted via the UI's "add" button. */
	abstract fun factory(): T

	/** Builds the Cloth Config field for a single row. */
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