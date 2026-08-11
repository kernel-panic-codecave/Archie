package net.kernelpanicsoft.archie.config

import dev.architectury.platform.Mod
import dev.architectury.platform.Platform
import net.kernelpanicsoft.archie.APlatform
import net.kernelpanicsoft.archie.util.onClient
import net.minecraft.network.chat.Component
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

/**
 * The single per-mod root of the config system, declared as a singleton `object` holding one or
 * more nested [ConfigSpec] objects:
 * ```kotlin
 * object Config : ConfigContainer(MyMod.MOD) {
 *     object Common : ConfigSpec.Common(MyMod.MOD) { ... }
 *     object Client : ConfigSpec.Client(MyMod.MOD) { ... }
 * }
 * ```
 * Call [init] once during common mod init, on both physical sides; it initializes every nested
 * [ConfigSpec] (loading/creating its file per its [ConfigSpec.predicate] timing) and, on the
 * client, builds and registers the merged Cloth Config UI screen via [ClientConfigContainer].
 *
 * @param mod The owning mod, used to derive each nested [ConfigSpec]'s default filename.
 * @param title Display title used for the container's screen when it holds more than one
 * [ConfigSpec]. Defaults to [mod]'s name.
 */
abstract class ConfigContainer(val mod: Mod, val title: Component = Component.literal(mod.name))
{
	/** Client-side mirror of this container, used to build the merged Cloth Config UI screen. */
	internal val client by lazy { ClientConfigContainer(this) }

	/** Nested [ConfigSpec] objects declared inside this container. */
	val configs: List<ConfigSpec>
		get() = this::class.nestedClasses
			.filterIsInstance<KClass<out ConfigSpec>>()
			.filter { it.isSubclassOf(ConfigSpec::class) }
			.mapNotNull { klass -> klass.objectInstance }

	/** Initializes every nested [ConfigSpec] and, on the client, registers the config UI screen. */
	fun init()
	{
		configs.forEach(ConfigSpec::init)
		onClient { initClient() }
	}

	internal fun initClient()
	{
		// Cloth Config's mod id differs by loader: Fabric allows hyphens ("cloth-config"), while
		// NeoForge's mod id charset doesn't, so its variant registers as "cloth_config" instead.
		val clothConfigModId = when (val platform = APlatform.platform)
		{
			"fabric" -> "cloth-config"
			"neoforge" -> "cloth_config"
			else -> throw UnsupportedOperationException("Unsupported platform: $platform")
		}
		if (Platform.isModLoaded(clothConfigModId))
			client.initClient()
	}
}