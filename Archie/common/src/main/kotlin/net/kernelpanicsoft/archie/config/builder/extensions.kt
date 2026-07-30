package net.kernelpanicsoft.archie.config.builder

import net.kernelpanicsoft.archie.config.CategorySpec
import net.kernelpanicsoft.archie.util.getReflection
import net.kernelpanicsoft.archie.util.setReflection
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder
import me.shedaniel.clothconfig2.api.ModifierKeyCode
import me.shedaniel.clothconfig2.gui.entries.DropdownBoxEntry
import me.shedaniel.clothconfig2.impl.builders.ColorFieldBuilder
import me.shedaniel.math.Color
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType
import java.lang.reflect.Field
import kotlin.reflect.KClass

/**
 * A [DropdownBoxEntry] cell creator that renders each [BlockEntityType] option as its owning
 * block's item icon plus registry name, for use with a dropdown field over block entity types.
 */
fun ofBlockEntityTypeObject(): DropdownBoxEntry.SelectionCellCreator<BlockEntityType<*>>
{
	return object : DropdownBoxEntry.DefaultSelectionCellCreator<BlockEntityType<*>>({
		Component.literal(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(it).toString())
	})
	{
		@Suppress("UNCHECKED_CAST")
		override fun create(selection: BlockEntityType<*>): DropdownBoxEntry.SelectionCellElement<BlockEntityType<*>>
		{
			val blocksField: Field = BlockEntityType::class.java.getDeclaredField("validBlocks")
			blocksField.isAccessible = true
			val blocks = blocksField.get(selection) as Set<Block>
			val block = blocks.first()
			val stack = ItemStack(block)
			return object : DropdownBoxEntry.DefaultSelectionCellElement<BlockEntityType<*>>(selection, toTextFunction)
			{
				override fun render(
					graphics: GuiGraphics?,
					mouseX: Int,
					mouseY: Int,
					x: Int,
					y: Int,
					width: Int,
					height: Int,
					delta: Float
				)
				{
					this.rendering = true
					this.x = x
					this.y = y
					this.width = width
					this.height = height
					val b = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height
					if (b)
					{
						graphics!!.fill(x + 1, y + 1, x + width - 1, y + height - 1, -15132391)
					}

					graphics!!.drawString(
						Minecraft.getInstance().font,
						(this.toTextFunction.apply(
							r
						) as Component).visualOrderText, x + 6 + 18, y + 6, if (b) 16777215 else 8947848
					)
					graphics.renderItem(stack, x + 4, y + 2)
				}
			}
		}

		override fun getCellHeight(): Int
		{
			return 20
		}

		override fun getCellWidth(): Int
		{
			return 146
		}

		override fun getDropBoxMaxHeight(): Int
		{
			return cellHeight * 7
		}
	}
}

/** Reflectively exposes Cloth Config's private `alpha` flag on [ColorFieldBuilder], since it has no public getter/setter. */
var ColorFieldBuilder.alphaMode: Boolean
	get() = getReflection("alpha")
	set(value) = setReflection("alpha", value)

/**
 * The `start*Field`/`start*List`/`start*Map` functions below extend [ConfigEntryBuilder] the same
 * way Cloth Config's own built-ins do (`startBooleanToggle`, `startIntField`, ...), so the field
 * types Archie adds - nested specs, registry entries, keybind/color lists and maps - are used the
 * same way. Each one just forwards to the matching builder class's constructor.
 */
fun <T : CategorySpec> ConfigEntryBuilder.startSpecField(fieldNameKey: Component, value: T): SpecFieldBuilder<T>
{
	return SpecFieldBuilder(resetButtonKey, fieldNameKey, value)
}

/** See [startSpecField]. Builds a single registry-entry field, resolved against [registry] and optionally narrowed to [subclass]. */
fun <T : Any, R : T> ConfigEntryBuilder.startRegistryField(
	fieldNameKey: Component,
	value: T,
	subclass: KClass<R>? = null,
	registry: Registry<T>
): RegistryFieldBuilder<T, R>
{
	return RegistryFieldBuilder(resetButtonKey, fieldNameKey, subclass, registry, value)
}

/** See [startSpecField]. Builds a dropdown field over arbitrary [selections], accepting free-text input for a `String` value. */
fun ConfigEntryBuilder.startStringDropdownField(
	fieldNameKey: Component,
	value: String,
	selections: Iterable<String> = emptyList()
): DropdownFieldBuilder<String>
{
	return DropdownFieldBuilder(resetButtonKey, fieldNameKey, value, selections).apply {
		toObjectFunction = { it }
	}
}

/** See [startSpecField]. Builds a dropdown field over arbitrary [selections] of any type [T]. */
fun <T : Any> ConfigEntryBuilder.startDropdownField(
	fieldNameKey: Component,
	value: T,
	selections: Iterable<T> = emptyList()
): DropdownFieldBuilder<T>
{
	return DropdownFieldBuilder(resetButtonKey, fieldNameKey, value, selections)
}

/** See [startSpecField]. Builds a list of nested [CategorySpec] entries. */
fun <T : CategorySpec> ConfigEntryBuilder.startSpecList(
	fieldNameKey: Component,
	value: List<T>,
	factory: () -> T
): SpecListBuilder<T>
{
	return SpecListBuilder(resetButtonKey, fieldNameKey, value, factory)
}

/** See [startSpecField]. Builds a list of [registry] entries; [factory] supplies a value for newly-inserted rows. */
fun <T : Any, R : T> ConfigEntryBuilder.startRegistryList(
	fieldNameKey: Component,
	value: List<T>,
	factory: () -> T,
	subclass: KClass<R>? = null,
	registry: Registry<T>
): RegistryListBuilder<T, R>
{
	return RegistryListBuilder(resetButtonKey, fieldNameKey, value, factory, subclass, registry)
}

/** See [startSpecField]. Builds a list of keybind entries; [factory] supplies a value for newly-inserted rows. */
fun ConfigEntryBuilder.startKeycodeList(
	fieldNameKey: Component,
	value: List<ModifierKeyCode>,
	factory: () -> ModifierKeyCode
): KeycodeListBuilder
{
	return KeycodeListBuilder(resetButtonKey, fieldNameKey, value, factory)
}

/** See [startSpecField]. Builds a list of color entries; [factory] supplies a value for newly-inserted rows. */
fun ConfigEntryBuilder.startColorList(
	fieldNameKey: Component,
	value: List<Color>,
	factory: () -> Color
): ColorListBuilder
{
	return ColorListBuilder(resetButtonKey, fieldNameKey, value, factory)
}

/** See [startSpecField]. Builds a `String`-keyed map of nested [CategorySpec] entries. */
fun <T : CategorySpec> ConfigEntryBuilder.startSpecMap(
	fieldNameKey: Component,
	value: Map<String, T>,
	factory: () -> T
): SpecMapBuilder<T>
{
	return SpecMapBuilder(resetButtonKey, fieldNameKey, value, factory)
}

/** See [startSpecField]. Builds a `String`-keyed map of [registry] entries; [factory] supplies a value for newly-inserted rows. */
fun <T : Any, R : T> ConfigEntryBuilder.startRegistryMap(
	fieldNameKey: Component,
	value: Map<String, T>,
	factory: () -> T,
	subclass: KClass<R>? = null,
	registry: Registry<T>
): RegistryMapBuilder<T, R>
{
	return RegistryMapBuilder(resetButtonKey, fieldNameKey, value, factory, subclass, registry)
}

/** See [startSpecField]. Builds a `String`-keyed map of keybind entries; [factory] supplies a value for newly-inserted rows. */
fun ConfigEntryBuilder.startKeycodeMap(
	fieldNameKey: Component,
	value: Map<String, ModifierKeyCode>,
	factory: () -> ModifierKeyCode
): KeycodeMapBuilder
{
	return KeycodeMapBuilder(resetButtonKey, fieldNameKey, value, factory)
}

/** See [startSpecField]. Builds a `String`-keyed map of color entries; [factory] supplies a value for newly-inserted rows. */
fun ConfigEntryBuilder.startColorMap(
	fieldNameKey: Component,
	value: Map<String, Color>,
	factory: () -> Color
): ColorMapBuilder
{
	return ColorMapBuilder(resetButtonKey, fieldNameKey, value, factory)
}

/** See [startSpecField]. Builds a `String`-keyed map of `Int` entries. */
fun ConfigEntryBuilder.startIntMap(fieldNameKey: Component, value: Map<String, Int>): IntegerMapBuilder
{
	return IntegerMapBuilder(resetButtonKey, fieldNameKey, value)
}

/** See [startSpecField]. Builds a `String`-keyed map of `Long` entries. */
fun ConfigEntryBuilder.startLongMap(fieldNameKey: Component, value: Map<String, Long>): LongMapBuilder
{
	return LongMapBuilder(resetButtonKey, fieldNameKey, value)
}

/** See [startSpecField]. Builds a `String`-keyed map of `Float` entries. */
fun ConfigEntryBuilder.startFloatMap(fieldNameKey: Component, value: Map<String, Float>): FloatMapBuilder
{
	return FloatMapBuilder(resetButtonKey, fieldNameKey, value)
}

/** See [startSpecField]. Builds a `String`-keyed map of `Double` entries. */
fun ConfigEntryBuilder.startDoubleMap(fieldNameKey: Component, value: Map<String, Double>): DoubleMapBuilder
{
	return DoubleMapBuilder(resetButtonKey, fieldNameKey, value)
}

/** See [startSpecField]. Builds a `String`-keyed map of `String` entries. */
fun ConfigEntryBuilder.startStrMap(fieldNameKey: Component, value: Map<String, String>): StringMapBuilder
{
	return StringMapBuilder(resetButtonKey, fieldNameKey, value)
}