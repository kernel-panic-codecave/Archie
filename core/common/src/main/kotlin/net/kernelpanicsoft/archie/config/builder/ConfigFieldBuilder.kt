package net.kernelpanicsoft.archie.config.builder

import me.shedaniel.clothconfig2.impl.builders.AbstractFieldBuilder
import net.kernelpanicsoft.archie.config.ConfigSpec
import net.kernelpanicsoft.archie.config.entry.ConfigSpecEntry
import net.minecraft.network.chat.Component
import java.util.*
import kotlin.jvm.optionals.getOrNull

/**
 * Cloth Config field builder producing a [ConfigSpecEntry] - a single "Edit" button field that
 * navigates into [value]'s own config screen. Built via `ConfigEntryBuilder.startConfigField`,
 * used by [net.kernelpanicsoft.archie.config.ClientConfigContainer] to let a container screen with
 * multiple [ConfigSpec]s drill down into each one.
 */
class ConfigFieldBuilder<T : ConfigSpec>(
	resetButtonKey: Component,
	fieldNameKey: Component,
	private val value: T
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
			requiresRestart
		)
		entry.setErrorSupplier {
			Optional.ofNullable(errorSupplier?.apply(entry.value)?.getOrNull())
		}
		return finishBuilding(entry)
	}
}