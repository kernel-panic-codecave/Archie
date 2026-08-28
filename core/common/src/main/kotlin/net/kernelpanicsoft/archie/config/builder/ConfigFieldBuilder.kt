package net.kernelpanicsoft.archie.config.builder

import me.shedaniel.clothconfig2.impl.builders.AbstractFieldBuilder
import net.kernelpanicsoft.archie.config.entry.ConfigSpecEntry
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import java.util.*
import kotlin.jvm.optionals.getOrNull

/**
 * Cloth Config field builder producing a [ConfigSpecEntry] - a single "Edit" button field that
 * navigates into whatever screen [openScreen] builds for [value]. Built via
 * `ConfigEntryBuilder.startConfigField`/`startScreenField`, used by
 * [net.kernelpanicsoft.archie.config.ClientConfigContainer] and [net.kernelpanicsoft.archie.config.ClientConfigGroup]
 * to let a container/group screen drill down into each of its children.
 */
class ConfigFieldBuilder<T : Any>(
	resetButtonKey: Component,
	fieldNameKey: Component,
	private val value: T,
	private val openScreen: (Screen) -> Screen,
) : AbstractFieldBuilder<T, ConfigSpecEntry<T>, ConfigFieldBuilder<T>>(
	resetButtonKey, fieldNameKey
)
{
	var buttonText: Component = Component.literal("Edit")
	var requiresRestart: Boolean = false

	override fun build(): ConfigSpecEntry<T>
	{
		val entry = ConfigSpecEntry(
			fieldNameKey,
			buttonText,
			value,
			requiresRestart,
			openScreen,
		)
		entry.setErrorSupplier {
			Optional.ofNullable(errorSupplier?.apply(entry.value)?.getOrNull())
		}
		return finishBuilding(entry)
	}
}