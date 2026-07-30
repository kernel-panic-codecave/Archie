package net.kernelpanicsoft.archie

import com.mojang.logging.LogUtils
import dev.architectury.platform.Mod
import dev.architectury.platform.Platform
import dev.architectury.registry.ReloadListenerRegistry
import kotlinx.serialization.Serializable
import net.kernelpanicsoft.archie.config.CategorySpec
import net.kernelpanicsoft.archie.config.ConfigSpec
import net.kernelpanicsoft.archie.data.ADataGeneratorPlatform
import net.kernelpanicsoft.archie.data.common.conditions.ABuiltinConditions
import net.kernelpanicsoft.archie.data.common.crafting.ingredients.ABuiltinIngredients
import net.kernelpanicsoft.archie.data.common.tags.ACommonTags
import net.kernelpanicsoft.archie.data.internal.ArchieDatagen
import net.kernelpanicsoft.archie.events.AEvents
import net.kernelpanicsoft.archie.gametest.AGameTestPlatform
import net.kernelpanicsoft.archie.gametest.AGameTestSide
import net.kernelpanicsoft.archie.gametest.internal.ArchieGameTest
import net.kernelpanicsoft.archie.gui.blockentity.BlockEntityStateManager
import net.kernelpanicsoft.archie.gui.theme.ThemeManifestResourceListener
import net.kernelpanicsoft.archie.gui.theme.ThemeResourceListener
import net.kernelpanicsoft.archie.networking.ArchieNetworkChannel
import net.kernelpanicsoft.archie.util.buildArray
import net.kernelpanicsoft.archie.util.onClient
import net.kernelpanicsoft.archie.util.rem
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.server.packs.PackType
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.entity.BlockEntityType
import org.slf4j.Logger

/**
 * Archie's mod object and library entrypoint.
 */
object Archie
{
	/** Archie's own mod id, used as the namespace for its resources and network channel. */
	const val MOD_ID = "archie"


	/** The Architectury [Mod] descriptor for Archie itself. */
	@JvmField
	val MOD: Mod = Platform.getMod(MOD_ID)

	/** Shared SLF4J logger for Archie's own internal logging. */
	@JvmField
	val LOGGER: Logger = LogUtils.getLogger()

	/**
	 * Initializes Archie's shared (loader-independent) systems.
	 *
	 * Registers Archie with [AEvents], wires up networking (skipped only for a server-only
	 * gametest run, since Architectury's networking registration touches client-only classes),
	 * initializes block entity state syncing, built-in data providers, and Archie's own config,
	 * and activates the datagen/gametest code paths when running under those tasks.
	 *
	 * @throws IllegalStateException if running on LexForge, which is not supported.
	 */
	@JvmStatic
	fun init()
	{

		if (Platform.isMinecraftForge())
			error("LexForge is not supported. Switch to NeoForge, or don't use my mods.")
		AEvents += MOD
		if (!AGameTestPlatform.isGameTest || AGameTestPlatform.side == AGameTestSide.CLIENT)
		{
			ArchieNetworkChannel.init()
		}
		BlockEntityStateManager.init()

		ABuiltinIngredients.init()
		ABuiltinConditions.init()
		ACommonTags.init()
		Config.init()


		// Datagen and GameTest code paths are only activated in dedicated run configs.
		if (AGameTestPlatform.isGameTest)
			ArchieGameTest.init()
		if (ADataGeneratorPlatform.isDataGen)
			ArchieDatagen.init()
		onClient {
			ReloadListenerRegistry.register(PackType.CLIENT_RESOURCES, ThemeManifestResourceListener(), Archie % "theme_manifest")
			ReloadListenerRegistry.register(PackType.CLIENT_RESOURCES, ThemeResourceListener(), Archie % "theme")
		}
	}

	/**
	 * Reserved for client-only initialization that must run after [init], from a client
	 * entrypoint. Currently a no-op: Archie's own config screen already registers synchronously
	 * inside [init], since deferring it to a client entrypoint would race Catalogue's config
	 * screen discovery (see [ConfigSpec.init]).
	 */
	@JvmStatic
	fun initClient()
	{
	}

	/**
	 * Reserved for common-side initialization that must run after both [init] and platform
	 * bootstrap. Currently a no-op.
	 */
	@JvmStatic
	fun initCommon()
	{
	}

	/**
	 * Archie's own config, registered under the "Config" title. `General` holds Archie's real
	 * settings; `Test` is a self-test fixture exercising every [CategorySpec] value type
	 * supported by the config system and is not meant to be user-facing.
	 */
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
				comment = Component.literal("Test Comment")
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
				default = TestSpec(),
				factory = ::TestSpec
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
				alpha = true
			)

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
			)

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
			)

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
				default = TestNestedSpec(),
				factory = ::TestNestedSpec
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
