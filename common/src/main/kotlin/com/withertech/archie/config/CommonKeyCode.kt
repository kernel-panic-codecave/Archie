package com.withertech.archie.config

import com.mojang.blaze3d.platform.InputConstants
import kotlinx.serialization.Serializable
import me.shedaniel.clothconfig2.api.Modifier
import me.shedaniel.clothconfig2.api.ModifierKeyCode

@Serializable
data class CommonKeyCode(val type: Type, val key: Int, val modifiers: Set<Modifier>)
{
	constructor(type: Type, key: Int, vararg modifiers: Modifier) : this(type, key, modifiers.toSet())
	@Serializable
	enum class Type
	{
		KEYSYM,
		SCANCODE,
		MOUSE;
	}

	@Serializable
	enum class Modifier
	{
		ALT,
		CONTROL,
		SHIFT
	}

	companion object
	{
		val unknown: CommonKeyCode = CommonKeyCode(Type.KEYSYM, -1)
	}
}

private val CommonKeyCode.modifier: Modifier
	get()
	{
		var alt = false
		var control = false
		var shift = false
		modifiers.forEach {
			when (it)
			{
				CommonKeyCode.Modifier.ALT -> alt = true
				CommonKeyCode.Modifier.CONTROL -> control = true
				CommonKeyCode.Modifier.SHIFT -> shift = true
			}
		}
		return Modifier.of(alt, control, shift)
	}

fun CommonKeyCode.toClient(): ModifierKeyCode = when (type)
{
	CommonKeyCode.Type.KEYSYM -> ModifierKeyCode.of(InputConstants.Type.KEYSYM.getOrCreate(key), modifier)
	CommonKeyCode.Type.SCANCODE -> ModifierKeyCode.of(InputConstants.Type.SCANCODE.getOrCreate(key), modifier)
	CommonKeyCode.Type.MOUSE -> ModifierKeyCode.of(InputConstants.Type.MOUSE.getOrCreate(key), modifier)
}

private val ModifierKeyCode.modifiers: Set<CommonKeyCode.Modifier>
	get() = buildSet {
		modifier.apply {
			if (hasAlt()) add(CommonKeyCode.Modifier.ALT)
			if (hasControl()) add(CommonKeyCode.Modifier.CONTROL)
			if (hasShift()) add(CommonKeyCode.Modifier.SHIFT)
		}
	}

@Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
fun ModifierKeyCode.toCommon(): CommonKeyCode = when (type)
{
	InputConstants.Type.KEYSYM -> CommonKeyCode(CommonKeyCode.Type.KEYSYM, keyCode.value, modifiers)
	InputConstants.Type.SCANCODE -> CommonKeyCode(CommonKeyCode.Type.SCANCODE, keyCode.value, modifiers)
	InputConstants.Type.MOUSE -> CommonKeyCode(CommonKeyCode.Type.MOUSE, keyCode.value, modifiers)
}