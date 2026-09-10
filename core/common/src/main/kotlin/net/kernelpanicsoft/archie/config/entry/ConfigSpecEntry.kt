package net.kernelpanicsoft.archie.config.entry

import com.google.common.collect.Lists
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry
import net.kernelpanicsoft.archie.util.requireMinecraftClient
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.narration.NarratableEntry
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import java.util.*

/**
 * Cloth Config list entry rendering a single "Edit" button for [value] that, when clicked, opens
 * the screen [openScreen] builds for it - e.g. navigating from a container's screen into a
 * [net.kernelpanicsoft.archie.config.ConfigSpec]'s own screen, or into a [net.kernelpanicsoft.archie.config.ConfigGroup]'s
 * subscreen. Built by `ConfigFieldBuilder`.
 */
class ConfigSpecEntry<T : Any>(
	fieldName: Component,
	buttonText: Component,
	value: T,
	requiresRestart: Boolean,
	private val openScreen: (Screen) -> Screen,
) : AbstractConfigListEntry<T>(fieldName, requiresRestart)
{
	private var value: T
	private val buttonWidget: Button
	private val widgets: MutableList<AbstractWidget>

	init
	{
		this.value = value
		this.buttonWidget = Button.builder(
			buttonText
		) {
			configScreen?.let { requireMinecraftClient.setScreen(openScreen(it)) }
		}
			.bounds(0, 0, 150, 20).build()
		this.widgets =
			Lists.newArrayList(*arrayOf<AbstractWidget>(this.buttonWidget))
	}

	override fun getValue(): T
	{
		return this.value
	}

	fun setValue(value: T)
	{
		this.value = value
	}

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
		val window = Minecraft.getInstance().window
		this.buttonWidget.active = this.isEditable
		this.buttonWidget.y = y

		val displayedFieldName = this.displayedFieldName
		if (requireMinecraftClient.font.isBidirectional)
		{
			graphics.drawString(
				requireMinecraftClient.font,
				displayedFieldName.visualOrderText,
				window.guiScaledWidth - x - Minecraft.getInstance().font.width(displayedFieldName),
				y + 6,
				16777215
			)
			this.buttonWidget.x = x
		} else
		{
			graphics.drawString(
				requireMinecraftClient.font,
				displayedFieldName.visualOrderText,
				x,
				y + 6,
				this.preferredTextColor
			)
			this.buttonWidget.x = x + entryWidth - 150
		}

		this.buttonWidget.setWidth(150)
		this.buttonWidget.render(graphics, mouseX, mouseY, delta)
	}

	override fun children(): MutableList<out GuiEventListener>
	{
		return this.widgets
	}

	override fun narratables(): MutableList<out NarratableEntry>
	{
		return this.widgets
	}
}