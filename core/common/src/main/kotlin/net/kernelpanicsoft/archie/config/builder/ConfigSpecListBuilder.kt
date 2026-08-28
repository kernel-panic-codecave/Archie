package net.kernelpanicsoft.archie.config.builder

import me.shedaniel.clothconfig2.api.ConfigEntryBuilder
import me.shedaniel.clothconfig2.gui.entries.NestedListListEntry
import me.shedaniel.clothconfig2.impl.builders.FieldBuilder
import net.kernelpanicsoft.archie.config.ConfigSpec
import net.kernelpanicsoft.archie.config.entry.ConfigSpecEntry
import net.minecraft.network.chat.Component

/** [ListFieldBuilder] for a [net.kernelpanicsoft.archie.config.ConfigSpecList] - each row is an "Edit" button ([ConfigSpecEntry]) via [startConfigField]; a new row comes from [scratchFactory] (auto-named, wired to the collection's own directory). */
class ConfigSpecListBuilder<T : ConfigSpec>(
	resetButtonKey: Component,
	fieldNameKey: Component,
	value: List<T>,
	private val scratchFactory: () -> T,
) : ListFieldBuilder<T, ConfigSpecEntry<T>, ConfigSpecListBuilder<T>>(
	resetButtonKey, fieldNameKey, value
)
{
	override fun factory(): T = scratchFactory.invoke()

	override fun ConfigEntryBuilder.builder(
		title: Component,
		value: T,
		list: NestedListListEntry<T, ConfigSpecEntry<T>>
	): FieldBuilder<T, ConfigSpecEntry<T>, *> = startConfigField(Component.literal(value.id), value)
}
