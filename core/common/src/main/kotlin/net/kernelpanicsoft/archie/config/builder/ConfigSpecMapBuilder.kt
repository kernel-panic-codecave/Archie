package net.kernelpanicsoft.archie.config.builder

import me.shedaniel.clothconfig2.api.ConfigEntryBuilder
import me.shedaniel.clothconfig2.gui.entries.MultiElementListEntry
import me.shedaniel.clothconfig2.gui.entries.NestedListListEntry
import me.shedaniel.clothconfig2.impl.builders.AbstractFieldBuilder
import net.kernelpanicsoft.archie.config.ConfigSpec
import net.kernelpanicsoft.archie.config.entry.ConfigSpecEntry
import net.kernelpanicsoft.archie.util.MutableEntry
import net.minecraft.network.chat.Component

/**
 * [MapFieldBuilder] for a [net.kernelpanicsoft.archie.config.ConfigSpecMap] - each row's value
 * column is an "Edit" button ([ConfigSpecEntry]) via [startConfigField]. A [ConfigSpec]'s [id] is
 * fixed at creation, so the row's key text only names a brand-new entry (see
 * [net.kernelpanicsoft.archie.config.ConfigSpecCollection.reconcile]) - retyping an existing row's
 * key doesn't rename its file.
 */
class ConfigSpecMapBuilder<T : ConfigSpec>(
	resetButtonKey: Component,
	fieldNameKey: Component,
	value: Map<String, T>,
	private val scratchFactory: () -> T,
) : MapFieldBuilder<T, ConfigSpecEntry<T>, ConfigSpecMapBuilder<T>>(
	resetButtonKey, fieldNameKey, value
)
{
	override fun valueFactory(): T = scratchFactory.invoke()

	override fun ConfigEntryBuilder.valueBuilder(
		title: Component,
		value: T,
		list: NestedListListEntry<MutableEntry<String, T>, MultiElementListEntry<MutableEntry<String, T>>>
	): AbstractFieldBuilder<T, ConfigSpecEntry<T>, *> = startConfigField(title, value)
}
