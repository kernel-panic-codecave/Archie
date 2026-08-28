package net.kernelpanicsoft.archie.config.entry

import com.google.common.collect.Lists
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry
import net.kernelpanicsoft.archie.util.minecraftClient
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.narration.NarratableEntry
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import java.util.*

/**
 * Cloth Config list entry for a [net.kernelpanicsoft.archie.config.DataSpec.configRef] field: a
 * cycle button stepping through [options] (labeled via [titleOf]), and an "Edit" button that, like
 * [ConfigSpecEntry], opens whichever option is currently selected via [openScreen]. Built by
 * `ConfigRefFieldBuilder`.
 */
class ConfigRefEntry<T : Any>(
	fieldName: Component,
	private val options: List<T>,
	initial: T,
	private val titleOf: (T) -> Component,
	private val openScreen: (T, Screen) -> Screen,
	requiresRestart: Boolean,
) : AbstractConfigListEntry<T>(fieldName, requiresRestart)
{
	private var value: T = initial
	private val cycleButton: Button
	private val editButton: Button
	private val widgets: MutableList<AbstractWidget>

	init
	{
		cycleButton = Button.builder(titleOf(value)) {
			value = options[(options.indexOf(value) + 1) % options.size]
			cycleButton.message = titleOf(value)
		}.bounds(0, 0, 100, 20).build()
		editButton = Button.builder(Component.literal("Edit")) {
			configScreen?.let { minecraftClient.setScreen(openScreen(value, it)) }
		}.bounds(0, 0, 46, 20).build()
		widgets = Lists.newArrayList(*arrayOf<AbstractWidget>(cycleButton, editButton))
	}

	override fun getValue(): T = value
	override fun getDefaultValue(): Optional<T> = Optional.empty()

	override fun render(
		graphics: GuiGraphics,
		index: Int,
		y: Int,
		x: Int,
		entryWidth: Int,
		entryHeight: Int,
		mouseX: Int,
		mouseY: Int,
		isHovered: Boolean,
		delta: Float
	)
	{
		super.render(graphics, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta)
		graphics.drawString(minecraftClient.font, displayedFieldName.visualOrderText, x, y + 6, preferredTextColor)
		editButton.x = x + entryWidth - editButton.width
		cycleButton.x = editButton.x - cycleButton.width - 2
		cycleButton.y = y
		editButton.y = y
		cycleButton.render(graphics, mouseX, mouseY, delta)
		editButton.render(graphics, mouseX, mouseY, delta)
	}

	override fun children(): MutableList<out GuiEventListener> = widgets
	override fun narratables(): MutableList<out NarratableEntry> = widgets
}
