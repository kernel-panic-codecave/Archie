package net.kernelpanicsoft.archie.config.builder

import me.shedaniel.clothconfig2.impl.builders.AbstractFieldBuilder
import net.kernelpanicsoft.archie.config.ConfigSpec
import net.kernelpanicsoft.archie.config.entry.ConfigRefEntry
import net.minecraft.network.chat.Component
import java.util.*
import kotlin.jvm.optionals.getOrNull

/** Cloth Config field builder producing a [ConfigRefEntry] for a `configRef` field. Built via `ConfigEntryBuilder.startConfigRefField`. */
class ConfigRefFieldBuilder<T : ConfigSpec>(
	resetButtonKey: Component,
	fieldNameKey: Component,
	private val options: List<T>,
	private val value: T,
) : AbstractFieldBuilder<T, ConfigRefEntry<T>, ConfigRefFieldBuilder<T>>(
	resetButtonKey, fieldNameKey
)
{
	var requiresRestart: Boolean = false

	override fun build(): ConfigRefEntry<T>
	{
		val entry = ConfigRefEntry(
			fieldNameKey,
			options,
			value,
			{ it.title },
			{ target, screen -> target.client.buildConfig(screen) },
			requiresRestart,
		)
		entry.setErrorSupplier {
			Optional.ofNullable(errorSupplier?.apply(entry.value)?.getOrNull())
		}
		return finishBuilding(entry)
	}
}
