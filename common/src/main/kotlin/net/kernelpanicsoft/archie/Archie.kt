package net.kernelpanicsoft.archie

import com.mojang.logging.LogUtils
import dev.architectury.platform.Mod
import dev.architectury.platform.Platform
import dev.architectury.registry.ReloadListenerRegistry
import net.kernelpanicsoft.archie.config.CommonKeyCode
import net.kernelpanicsoft.archie.config.v2.builder.ConfigCatObject
import net.kernelpanicsoft.archie.config.v2.builder.ConfigDocObject
import net.kernelpanicsoft.archie.config.v2.runtime.ConfigEngine
import net.kernelpanicsoft.archie.config.v2.ui.ConfigUiAdapter
import net.kernelpanicsoft.archie.config.v2.ui.cloth.ClothConfigUiAdapter
import net.kernelpanicsoft.archie.config.v2.ui.yacl.YaclConfigUiAdapter
import net.kernelpanicsoft.archie.data.ADataGeneratorPlatform
import net.kernelpanicsoft.archie.data.common.conditions.ABuiltinConditions
import net.kernelpanicsoft.archie.data.common.crafting.ingredients.ABuiltinIngredients
import net.kernelpanicsoft.archie.data.common.tags.ACommonTags
import net.kernelpanicsoft.archie.data.internal.ArchieDatagen
import net.kernelpanicsoft.archie.events.AEvents
import net.kernelpanicsoft.archie.gametest.AGameTestPlatform
import net.kernelpanicsoft.archie.gametest.internal.ArchieGameTest
import net.kernelpanicsoft.archie.gui.blockentity.BlockEntityStateManager
import net.kernelpanicsoft.archie.gui.theme.ThemeManifestResourceListener
import net.kernelpanicsoft.archie.gui.theme.ThemeResourceListener
import net.kernelpanicsoft.archie.gui.util.KColor
import net.kernelpanicsoft.archie.networking.ArchieNetworkChannel
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.PackType
import org.slf4j.Logger

/**
 * The root singleton for the Archie library mod.
 *
 * Provides the mod ID, the Architectury [Mod] descriptor, a shared [Logger], and the
 * `get(path)` operator for constructing [ResourceLocation]s in the `archie` namespace.
 * Also hosts Archie's own [ConfigSpec] as a nested [Config] object for testing purposes.
 *
 * ### Initialization
 * Call [init] once during common mod initialization and [initClient] once on the client side:
 * ```kotlin
 * // In your mod entrypoint:
 * Archie.init()       // common
 * Archie.initClient() // client-only
 * ```
 */
object Archie
{
	const val MOD_ID = "archie"

	@Volatile
	private var initialized: Boolean = false

	@JvmField
	val MOD: Mod = Platform.getMod(MOD_ID)

	@JvmField
	val LOGGER: Logger = LogUtils.getLogger()

	@JvmStatic
	fun init()
	{
		if (initialized) return
		synchronized(this)
		{
			if (initialized) return
			if (Platform.isMinecraftForge())
				error("LexForge is not supported. Switch to NeoForge, or don't use my mods.")
			// Register mod-scoped hooks first so downstream modules can subscribe during init.
			AEvents += MOD
			// Register packet handlers before any features try to send packets.
			// Skip in gametest mode where Architectury networking has issues.
			if (!AGameTestPlatform.isGameTest)
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
			initialized = true
		}
	}

	@JvmStatic
	fun initClient()
	{
		Config.initClient()
		ReloadListenerRegistry.register(
			PackType.CLIENT_RESOURCES,
			ThemeManifestResourceListener()
		)
		ReloadListenerRegistry.register(
			PackType.CLIENT_RESOURCES,
			ThemeResourceListener()
		)
	}

	@JvmStatic
	fun initCommon()
	{
	}
	/**
	 * Archie's v2 config using the delegate-based DSL.
	 *
	 * Access config values from gameplay code via delegates:
	 * ```kotlin
	 * if (ArchieV2Config.general.enabled) { ... }
	 * val mode = ArchieV2Config.general.mode
	 * ```
	 *
	 * Wired up once during [init] and bound to state automatically.
	 */
	object Config : ConfigDocObject(MOD,"archie", "Archie")
	{

		override val adapters: List<ConfigUiAdapter> = buildList {
			add(YaclConfigUiAdapter)
			add(ClothConfigUiAdapter)
		}
		val general = category(GeneralCategory())
		val test = category(TestCategory())

		class GeneralCategory : ConfigCatObject("general", "General") {
			val enabled by boolean(
				title = "Enabled",
				description = "Master enable/disable for Archie features",
				default = true
			)

			val logLevel by choice(
				title = "Log Level",
				options = listOf("DEBUG", "INFO", "WARN", "ERROR"),
				description = "Logging verbosity",
				default = "INFO"
			)
		}

		class TestCategory : ConfigCatObject("test", "Test Category") {
			val testBoolean by boolean(title = "Test Boolean", default = false)

			val testInt by int(title = "Test Int", default = 0)

			val testLong by long(title = "Test Long", default = 0L)

			val testIntSlider by intSlider(
				title = "Test Int Slider",
				default = 0,
				min = Int.MIN_VALUE / 2 + 1,
				max = Int.MAX_VALUE / 2
			)

			val testLongSlider by longSlider(
				title = "Test Long Slider",
				default = 0L,
				min = Long.MIN_VALUE / 2 + 1,
				max = Long.MAX_VALUE / 2
			)

			val testFloat by float(title = "Test Float", default = 0.0f)

			val testDouble by double(title = "Test Double", default = 0.0)

			val testString by string(title = "Test String", default = "")

			val testRegistry by registry(
				title = "Test Registry",
				description = "Registry item (string ID)",
				default = "minecraft:cobblestone",
				registryId = "minecraft:core/block_item"
			)

			val testKeycode by keyCode(title = "Test Keycode", default = CommonKeyCode.unknown)

			val testColor by color(title = "Test Color", default = KColor.WHITE, supportsAlpha = true)

			val testEnumSelector by choice(
				title = "Test Enum Selector",
				options = listOf("Foo", "Bar"),
				default = "Foo"
			)

			val testSelector by choice(
				title = "Test Selector",
				options = listOf("foo", "bar"),
				default = "foo"
			)

			val testIntList by intList(title = "Test Int List", default = listOf())

			val testLongList by longList(title = "Test Long List", default = listOf())

			val testFloatList by floatList(title = "Test Float List", default = listOf())

			val testDoubleList by doubleList(title = "Test Double List", default = listOf())

			val testStringList by stringList(title = "Test String List", default = listOf())

			val testRegistryList by registryList(
				title = "Test Registry List",
				description = "Registry list (string IDs)",
				default = listOf(),
				registryId = "minecraft:core/block_item"
			)

			val testKeycodeList by keyCodeList(title = "Test Keycode List", default = listOf())

			val testColorList by colorList(title = "Test Color List", default = listOf(), supportsAlpha = true)

			val testIntMap by intMap(title = "Test Int Map", default = mapOf())

			val testLongMap by longMap(title = "Test Long Map", default = mapOf())

			val testFloatMap by floatMap(title = "Test Float Map", default = mapOf())

			val testDoubleMap by doubleMap(title = "Test Double Map", default = mapOf())

			val testStringMap by stringMap(title = "Test String Map", default = mapOf())

			val testRegistryMap by registryMap(
				title = "Test Registry Map",
				description = "Registry map (string IDs)",
				default = mapOf(),
				registryId = "minecraft:core/block_item"
			)

			val testKeycodeMap by keyCodeMap(title = "Test Keycode Map", default = mapOf())

			// Type-safe nested category collections
			val testSpecList by categoryListOf { TestSpecCategory() }

			val testSpecMap by categoryMapOf { id -> TestSpecMapCategory(id) }

			// Nested categories (specs)
			val testSpecCategory = nestedCategory(TestSpecCategory())

			val testNestedSpec = nestedCategory(TestNestedSpecCategory())

			val testSub = nestedCategory(TestSubCategory())

			class TestSpecCategory : ConfigCatObject("test_spec_category", "Test Spec Category") {
				val test by boolean(title = "Test", default = false)
			}

			class TestSpecMapCategory(id: String) : ConfigCatObject("test_spec_map_$id", "Test Spec Map ($id)") {
				val mapValue by string(title = "Map Value", default = id)
				val mapEnabled by boolean(title = "Enabled", default = true)
			}

			class TestNestedSpecCategory : ConfigCatObject("test_nested_spec", "Test Nested Spec") {
				val childrenList = nestedCategory(TestNestedSpecChildListCategory())

				val childrenMap = nestedCategory(TestNestedSpecChildMapCategory())

				class TestNestedSpecChildListCategory : ConfigCatObject("children_list", "Children List") {
					val firstChild = nestedCategory(TestNestedSpecChildCategory("first_child", "First Child"))
					val secondChild = nestedCategory(TestNestedSpecChildCategory("second_child", "Second Child"))
				}

				class TestNestedSpecChildMapCategory : ConfigCatObject("children_map", "Children Map") {
					val keyAChild = nestedCategory(TestNestedSpecChildCategory("key_a_child", "Key A Child"))
					val keyBChild = nestedCategory(TestNestedSpecChildCategory("key_b_child", "Key B Child"))
				}
			}

			class TestNestedSpecChildCategory(id: String, title: String) : ConfigCatObject(id, title) {
				val childValue by boolean(title = "Child Value", default = false)
				val childName by string(title = "Child Name", default = "unnamed")
			}

			class TestSubCategory : ConfigCatObject("test_sub", "Test Subcategory") {
				val test by boolean(title = "Test", default = false)

				val testRegistry by registry(
					title = "Test Registry",
					description = "Registry block entity type",
					default = "minecraft:chest",
					registryId = "minecraft:core/block_entity_type"
				)
			}
		}
	}
}