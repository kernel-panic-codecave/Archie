package com.withertech.archie.config.builder

import com.withertech.archie.util.MutableEntry
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder
import me.shedaniel.clothconfig2.api.ModifierKeyCode
import me.shedaniel.clothconfig2.gui.entries.KeyCodeEntry
import me.shedaniel.clothconfig2.gui.entries.MultiElementListEntry
import me.shedaniel.clothconfig2.gui.entries.NestedListListEntry
import me.shedaniel.clothconfig2.impl.builders.FieldBuilder
import net.minecraft.network.chat.Component

class KeycodeMapBuilder(
	resetButtonKey: Component,
	fieldNameKey: Component,
	value: Map<String, ModifierKeyCode>,
	private val factory: () -> ModifierKeyCode
) : MapFieldBuilder<ModifierKeyCode, KeyCodeEntry, KeycodeMapBuilder>(
	resetButtonKey, fieldNameKey, value
)
{
	private var allowModifiers: Boolean = true
	private var _allowKey: Boolean = true
	private var allowKey: Boolean
		get() = _allowKey
		set(allowKey)
		{
			require(!(!this.allowMouse && !allowKey))
			_allowKey = allowKey
		}
	private var _allowMouse: Boolean = true
	private var allowMouse: Boolean
		get() = _allowMouse
		set(allowMouse)
		{
			require(!(!this.allowKey && !allowMouse))
			_allowMouse = allowMouse
		}

	override fun valueFactory(): ModifierKeyCode = factory.invoke()

	override fun ConfigEntryBuilder.valueBuilder(
		title: Component,
		value: ModifierKeyCode,
		list: NestedListListEntry<MutableEntry<String, ModifierKeyCode>, MultiElementListEntry<MutableEntry<String, ModifierKeyCode>>>
	): FieldBuilder<ModifierKeyCode, KeyCodeEntry, *>
	{
		return startModifierKeyCodeField(title, value)
			.setAllowModifiers(allowModifiers)
			.setAllowKey(allowKey)
			.setAllowMouse(allowMouse)
	}
}