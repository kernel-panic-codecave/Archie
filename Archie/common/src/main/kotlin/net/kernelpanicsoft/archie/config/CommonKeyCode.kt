package net.kernelpanicsoft.archie.config

import com.mojang.blaze3d.platform.InputConstants
import kotlinx.serialization.Serializable
import me.shedaniel.clothconfig2.api.Modifier
import me.shedaniel.clothconfig2.api.ModifierKeyCode

/**
 * A serializable, client-independent representation of a keybind, used by [CategorySpec.keycode]
 * fields so config files don't depend on Cloth Config's [ModifierKeyCode]. Convert to/from the
 * client type with [toClient]/[toCommon].
 */
@Serializable
data class CommonKeyCode(val type: Type, val key: Int, val modifiers: Set<Modifier>)
{
	constructor(type: Type, key: Int, vararg modifiers: Modifier) : this(type, key, modifiers.toSet())

	/** Which [InputConstants] key space [key] is a code in. */
	@Serializable
	enum class Type
	{
		KEYSYM,
		SCANCODE,
		MOUSE;
	}

	/** A modifier key held alongside the base [key]. */
	@Serializable
	enum class Modifier
	{
		ALT,
		CONTROL,
		SHIFT
	}

	companion object
	{
		/** Sentinel for "no key bound". */
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

/** Converts this to Cloth Config's client-side [ModifierKeyCode]. */
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

/** Converts a Cloth Config [ModifierKeyCode] to the serializable [CommonKeyCode]. */
@Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
fun ModifierKeyCode.toCommon(): CommonKeyCode = when (type)
{
	InputConstants.Type.KEYSYM -> CommonKeyCode(CommonKeyCode.Type.KEYSYM, keyCode.value, modifiers)
	InputConstants.Type.SCANCODE -> CommonKeyCode(CommonKeyCode.Type.SCANCODE, keyCode.value, modifiers)
	InputConstants.Type.MOUSE -> CommonKeyCode(CommonKeyCode.Type.MOUSE, keyCode.value, modifiers)
}