@file:Suppress("unused")
@file:OptIn(ExperimentalContracts::class)
package net.kernelpanicsoft.archie.util

import net.minecraft.network.chat.*
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import java.util.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@DslMarker
annotation class ComponentBuilderDsl

@ComponentBuilderDsl
class ComponentBuilder @PublishedApi internal constructor(private val component: MutableComponent = Component.empty())
{
	fun build(): Component
	{
		return component
	}

	fun text(text: String, block: ComponentBuilder.() -> Unit = {})
	{
		component.append(ComponentBuilder(Component.literal(text)).apply(block).build())
	}

	fun translate(key: String, vararg args: Any, block: ComponentBuilder.() -> Unit = {})
	{
		component.append(ComponentBuilder(Component.translatable(key, *args)).apply(block).build())
	}

	fun style(style: Style)
	{
		component.withStyle(style)
	}

	fun style(block: StyleBuilder.() -> Unit)
	{
		component.withStyle(StyleBuilder(component.style).apply(block).build())
	}
}

@ComponentBuilderDsl
class StyleBuilder @PublishedApi internal constructor(private var style: Style = Style.EMPTY)
{
	fun build(): Style
	{
		return style
	}

	var color: TextColor?
		get() = style.color
		set(color)
		{
			style = style.withColor(color)
		}

	var bold: Boolean?
		get() = style.isBold
		set(bold)
		{
			style = style.withBold(bold)
		}

	var italic: Boolean?
		get() = style.isItalic
		set(italic)
		{
			style = style.withItalic(italic)
		}

	var underlined: Boolean?
		get() = style.isUnderlined
		set(underlined)
		{
			style = style.withUnderlined(underlined)
		}

	var strikethrough: Boolean?
		get() = style.isStrikethrough
		set(strikethrough)
		{
			style = style.withStrikethrough(strikethrough)
		}

	var obfuscated: Boolean?
		get() = style.isObfuscated
		set(obfuscated)
		{
			style = style.withObfuscated(obfuscated)
		}

	var clickEvent: ClickEvent?
		get() = style.clickEvent
		set(clickEvent)
		{
			style = style.withClickEvent(clickEvent)
		}

	fun clickEvent(block: ClickEventBuilder.() -> Unit)
	{
		clickEvent = ClickEventBuilder().apply(block).build()
	}

	var hoverEvent: HoverEvent?
		get() = style.hoverEvent
		set(hoverEvent)
		{
			style = style.withHoverEvent(hoverEvent)
		}

	fun hoverEvent(block: HoverEventBuilder.() -> Unit)
	{
		hoverEvent = HoverEventBuilder().apply(block).build()
	}

	var insertion: String?
		get() = style.insertion
		set(insertion)
		{
			style = style.withInsertion(insertion)
		}

	var font: ResourceLocation?
		get() = style.font
		set(font)
		{
			style = style.withFont(font)
		}
}

@ComponentBuilderDsl
class ClickEventBuilder @PublishedApi internal constructor()
{
	private lateinit var action: ClickEvent.Action
	private lateinit var value: String

	fun build(): ClickEvent
	{
		return ClickEvent(action, value)
	}

	fun openUrl(value: String)
	{
		this.action = ClickEvent.Action.OPEN_URL
		this.value = value
	}

	fun openFile(value: String)
	{
		this.action = ClickEvent.Action.OPEN_FILE
		this.value = value
	}

	fun runCommand(value: String)
	{
		this.action = ClickEvent.Action.RUN_COMMAND
		this.value = value
	}

	fun suggestCommand(value: String)
	{
		this.action = ClickEvent.Action.SUGGEST_COMMAND
		this.value = value
	}

	fun changePage(value: String)
	{
		this.action = ClickEvent.Action.CHANGE_PAGE
		this.value = value
	}

	fun copyToClipboard(value: String)
	{
		this.action = ClickEvent.Action.COPY_TO_CLIPBOARD
		this.value = value
	}
}

@ComponentBuilderDsl
class HoverEventBuilder @PublishedApi internal constructor()
{
	private lateinit var action: HoverEvent.Action<out Any>
	private lateinit var value: Any

	fun build(): HoverEvent
	{
		@Suppress("UNCHECKED_CAST")
		return HoverEvent(action as HoverEvent.Action<Any>, value)
	}

	fun text(block: ComponentBuilder.() -> Unit)
	{
		action = HoverEvent.Action.SHOW_TEXT
		value = buildComponent(block)
	}

	fun item(stack: ItemStack)
	{
		action = HoverEvent.Action.SHOW_ITEM
		value = HoverEvent.ItemStackInfo(stack)
	}

	fun entity(type: EntityType<*>, uuid: UUID, name: Component? = null, block: (ComponentBuilder.() -> Unit)? = null)
	{
		action = HoverEvent.Action.SHOW_ENTITY
		value = HoverEvent.EntityTooltipInfo(type, uuid, name ?: block?.let { ComponentBuilder().apply(it).build() })
	}
}

inline operator fun Component.invoke(
	builderAction: ComponentBuilder.() -> Unit
): Component
{
	return ComponentBuilder(copy()).apply(builderAction).build()
}

inline fun Player.sendSystemMessage(
	builderAction: ComponentBuilder.() -> Unit
)
{
	contract { callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE) }
	sendSystemMessage(buildComponent(builderAction))
}

inline fun buildComponent(
	builderAction: ComponentBuilder.() -> Unit
): Component
{
	contract { callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE) }
	return ComponentBuilder().apply(builderAction).build()
}

inline fun buildStyle(
	builderAction: StyleBuilder.() -> Unit
): Style
{
	contract { callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE) }
	return StyleBuilder().apply(builderAction).build()
}