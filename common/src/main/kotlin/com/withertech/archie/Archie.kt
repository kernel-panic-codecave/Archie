package com.withertech.archie

import com.mojang.logging.LogUtils
import com.withertech.archie.config.CategorySpec
import com.withertech.archie.config.ConfigSpec
import com.withertech.archie.config.builder.alphaMode
import com.withertech.archie.data.ArchieDataGeneratorPlatform
import com.withertech.archie.data.common.conditions.BuiltinConditions
import com.withertech.archie.data.common.crafting.ingredients.BuiltinIngredients
import com.withertech.archie.data.common.tags.CommonTags
import com.withertech.archie.data.internal.ArchieDatagen
import com.withertech.archie.events.ArchieEvents
import com.withertech.archie.gametest.ArchieGameTestPlatform
import com.withertech.archie.gametest.internal.ArchieGameTest
import com.withertech.archie.test.BlockRegistry
import com.withertech.archie.test.GuiRegistry
import com.withertech.archie.test.ItemRegistry
import com.withertech.archie.test.TileRegistry
import com.withertech.archie.util.buildArray
import dev.architectury.platform.Mod
import dev.architectury.platform.Platform
import kotlinx.serialization.Serializable
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.entity.BlockEntityType
import org.slf4j.Logger

object Archie
{
	const val MOD_ID = "archie"

	@JvmField
	val MOD: Mod = Platform.getMod(MOD_ID)

	@JvmField
	val LOGGER: Logger = LogUtils.getLogger()

	@JvmStatic
	operator fun get(loc: String): ResourceLocation = ResourceLocation(MOD_ID, loc)

	@JvmStatic
	fun init()
	{
		Archie
		if (Platform.isMinecraftForge())
			error("LexForge is not supported. Switch to NeoForge, or don't use my mods.")
		ArchieEvents += MOD

		BuiltinIngredients.init()
		BuiltinConditions.init()
		CommonTags.init()
		Config.init()
		if (Platform.isDevelopmentEnvironment())
		{
			BlockRegistry.init()
			ItemRegistry.init()
			TileRegistry.init()
			GuiRegistry.init()
		}
		if (ArchieGameTestPlatform.isGameTest)
			ArchieGameTest.init()
		if (ArchieDataGeneratorPlatform.isDataGen)
			ArchieDatagen.init()
	}

	@JvmStatic
	fun initClient()
	{
		GuiRegistry.initClient()
	}

	@JvmStatic
	fun initCommon()
	{
	}

	object Config : ConfigSpec(MOD, Component.literal("Config"))
	{
		override val categories: List<CategorySpec> = buildList {
			add(General)
			add(Test)
		}

		object General : CategorySpec(Component.literal("General"), "general")
		{
			val tests by boolean(
				title = Component.literal("Tests"),
				default = false
			)
		}

		@Suppress("unused")
		object Test : CategorySpec(Component.literal("Test Category"), "test")
		{
			override val subcategories: List<CategorySpec> = buildList {
				add(TestSub)
			}

			override val isEnabled: Boolean
				get() = General.tests

			val testBoolean by boolean(
				title = Component.literal("Test Boolean"),
				comment = comment(Component.literal("Test Comment"))
			)

			val testInt by int(
				title = Component.literal("Test Int"),
			)

			val testLong by long(
				title = Component.literal("Test Long"),
			)

			val testIntSlider by intSlider(
				title = Component.literal("Test Int Slider"),
				min = Int.MIN_VALUE / 2 + 1,
				max = Int.MAX_VALUE / 2
			)

			val testLongSlider by longSlider(
				title = Component.literal("Test Long Slider"),
				min = Long.MIN_VALUE / 2 + 1,
				max = Long.MAX_VALUE / 2,
			)

			val testFloat by float(
				title = Component.literal("Test Float"),
			)

			val testDouble by double(
				title = Component.literal("Test Double"),
			)

			val testString by string(
				title = Component.literal("Test String"),
			)

			val testSpec by spec(
				title = Component.literal("Test Spec"),
				default = TestSpec()
			)

			val testRegistry: BlockItem by registry(
				title = Component.literal("Test Registry"),
				default = Items.COBBLESTONE,
				subclass = BlockItem::class,
				registry = BuiltInRegistries.ITEM
			)

			val testKeycode by keycode(
				title = Component.literal("Test Keycode"),
			)

			val testColor by color(
				title = Component.literal("Test Color"),
			) {
				alphaMode = true
			}

			val testEnumSelector by enumSelector(
				title = Component.literal("Test Enum Selector"),
				kclass = TestEnum::class,
				default = TestEnum.Foo
			)

			val testSelector by selector(
				title = Component.literal("Test Selector"),
				kclass = String::class,
				default = "foo",
				entries = buildArray {
					add("foo")
					add("bar")
				}
			) {
			}

			val testIntList by intList(
				title = Component.literal("Test Int List"),
			)

			val testLongList by longList(
				title = Component.literal("Test Long List"),
			)

			val testFloatList by floatList(
				title = Component.literal("Test Float List"),
			)

			val testDoubleList by doubleList(
				title = Component.literal("Test Double List"),
			)

			val testStringList by stringList(
				title = Component.literal("Test String List"),
			)

			val testSpecList by specList(
				title = Component.literal("Test Spec List"),
				factory = ::TestSpec
			)

			val testRegistryList: List<BlockItem> by registryList(
				title = Component.literal("Test Registry List"),
				factory = Items::COBBLESTONE,
				subclass = BlockItem::class,
				registry = BuiltInRegistries.ITEM
			)

			val testKeycodeList by keycodeList(
				title = Component.literal("Test Keycode List"),
			)

			val testColorList by colorList(
				title = Component.literal("Test Color List"),
			) {
				alphaMode = true
			}

			val testIntMap by intMap(
				title = Component.literal("Test Int Map"),
			)

			val testLongMap by longMap(
				title = Component.literal("Test Long Map"),
			)

			val testFloatMap by floatMap(
				title = Component.literal("Test Float Map"),
			)

			val testDoubleMap by doubleMap(
				title = Component.literal("Test Double Map"),
			)

			val testStringMap by stringMap(
				title = Component.literal("Test String Map"),
			)

			val testSpecMap by specMap(
				title = Component.literal("Test Spec Map"),
				factory = ::TestSpec
			)

			val testRegistryMap: Map<String, BlockItem> by registryMap(
				title = Component.literal("Test Registry Map"),
				factory = Items::COBBLESTONE,
				subclass = BlockItem::class,
				registry = BuiltInRegistries.ITEM
			)

			val testKeycodeMap by keycodeMap(
				title = Component.literal("Test Keycode Map"),
			)

			val testColorMap by colorMap(
				title = Component.literal("Test Color Map")
			)

			val testNestedSpec by spec(
				title = Component.literal("Test Nested Spec"),
				default = TestNestedSpec()
			)

			@Serializable
			enum class TestEnum
			{
				Foo,
				Bar
			}

			class TestSpec : CategorySpec(Component.literal("Test Spec"))
			{
				val test by boolean(
					title = Component.literal("Test"),
				)
			}

			class TestNestedSpec : CategorySpec(Component.literal("Test Nested Spec"))
			{
				val childrenList by specList(
					title = Component.literal("Children List"),
					factory = ::TestNestedSpec
				)

				val childrenMap by specMap(
					title = Component.literal("Children Map"),
					factory = ::TestNestedSpec
				)
			}

			object TestSub : CategorySpec(Component.literal("Test Subcategory"), "test_sub")
			{
				val test by boolean(
					title = Component.literal("Test"),
				)

				val testRegistry by registry(
					title = Component.literal("Test Registry"),
					default = BlockEntityType.CHEST,
					registry = BuiltInRegistries.BLOCK_ENTITY_TYPE
				)
			}
		}
	}
}