package com.withertech.archie.config.builder

import me.shedaniel.clothconfig2.api.ConfigEntryBuilder
import me.shedaniel.clothconfig2.api.Modifier
import me.shedaniel.clothconfig2.api.ModifierKeyCode
import me.shedaniel.clothconfig2.gui.entries.KeyCodeEntry
import me.shedaniel.clothconfig2.gui.entries.NestedListListEntry
import me.shedaniel.clothconfig2.impl.builders.FieldBuilder
import me.shedaniel.clothconfig2.impl.builders.KeyCodeBuilder
import net.minecraft.network.chat.Component

class KeycodeListBuilder(
	resetButtonKey: Component,
	fieldNameKey: Component,
	value: List<ModifierKeyCode>,
	private val factory: () -> ModifierKeyCode
) :
	ListFieldBuilder<ModifierKeyCode, KeyCodeEntry, KeycodeListBuilder>(
		resetButtonKey,
		fieldNameKey,
		value
	)
{
	var allowModifiers: Boolean = true
	private var _allowKey: Boolean = true
	var allowKey: Boolean
		get() = _allowKey
		set(allowKey)
		{
			require(!(!this.allowMouse && !allowKey))
			_allowKey = allowKey
		}
	private var _allowMouse: Boolean = true
	var allowMouse: Boolean
		get() = _allowMouse
		set(allowMouse)
		{
			require(!(!this.allowKey && !allowMouse))
			_allowMouse = allowMouse
		}

	override fun factory(): ModifierKeyCode = factory.invoke()

	override fun ConfigEntryBuilder.builder(
		title: Component,
		value: ModifierKeyCode,
		list: NestedListListEntry<ModifierKeyCode, KeyCodeEntry>
	): FieldBuilder<ModifierKeyCode, KeyCodeEntry, *>
	{
		return startModifierKeyCodeField(title, value)
			.setAllowModifiers(allowModifiers)
			.setAllowKey(allowKey)
			.setAllowMouse(allowMouse)
	}

}