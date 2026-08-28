package net.kernelpanicsoft.archie.config

import dev.architectury.platform.Mod
import dev.architectury.platform.Platform
import net.kernelpanicsoft.archie.util.isClothConfigLoaded
import net.kernelpanicsoft.archie.util.onClient
import net.minecraft.network.chat.Component
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

/**
 * The single per-mod root of the config system, declared as a singleton `object` holding one or
 * more nested [ConfigNode]s - [ConfigSpec]s, [ConfigGroup]s (folders), and [ConfigSpecCollection]s
 * (dynamic, directory-backed collections):
 * ```kotlin
 * object Config : ConfigContainer(MyMod.MOD) {
 *     object Common : ConfigSpec.Common(MyMod.MOD) { ... }
 *     object Client : ConfigSpec.Client(MyMod.MOD) { ... }
 * }
 * ```
 * Call [init] once during common mod init, on both physical sides; it initializes every nested
 * [ConfigNode] (loading/creating each [ConfigSpec]'s file per its [ConfigSpec.predicate] timing,
 * scanning each [ConfigSpecCollection]'s directory) and, on the client, builds and registers the
 * merged Cloth Config UI screen via [ClientConfigContainer].
 *
 * @param mod The owning mod, used to derive each nested [ConfigSpec]'s default filename.
 * @param title Display title used for the container's screen when it holds more than one
 * [ConfigNode]. Defaults to [mod]'s name.
 */
abstract class ConfigContainer(val mod: Mod, val title: Component = Component.literal(mod.name))
{
	/** Client-side mirror of this container, used to build the merged Cloth Config UI screen. */
	internal val client by lazy { ClientConfigContainer(this) }

	/** Nested [ConfigNode]s declared inside this container. */
	val children: List<ConfigNode>
		get() = this::class.nestedClasses
			.filterIsInstance<KClass<out ConfigNode>>()
			.filter { it.isSubclassOf(ConfigNode::class) }
			.mapNotNull { klass -> klass.objectInstance }

	/** Every [ConfigSpec] reachable from [children], however deep under nested [ConfigGroup]s. */
	internal val configs: List<ConfigSpec>
		get() = children.flatMap(::specsOf)

	private fun specsOf(node: ConfigNode): List<ConfigSpec> = when (node)
	{
		is ConfigSpec -> listOf(node)
		is ConfigGroup -> node.children.flatMap(::specsOf)
		is ConfigSpecCollection<*> -> emptyList()
	}

	/** Initializes every reachable [ConfigNode] and, on the client, registers the config UI screen. */
	fun init()
	{
		children.forEach { attach(it, emptyList()) }
		onClient { initClient() }
	}

	private fun attach(node: ConfigNode, segments: List<String>)
	{
		when (node)
		{
			is ConfigSpec ->
			{
				node.pathSegments = segments
				node.init()
			}
			is ConfigGroup -> node.children.forEach { attach(it, segments + node.id) }
			is ConfigSpecCollection<*> ->
			{
				node.registerNetwork(mod, (segments + node.id).joinToString("_"))
				node.baseFolder = { (listOf(mod.modId) + segments).fold(Platform.getConfigFolder()) { acc, seg -> acc.resolve(seg) } }
				if (node.synchronized)
					node.deferScanToServerStart(mod, segments)
				else
					node.init()
			}
		}
	}

	internal fun initClient()
	{
		if (isClothConfigLoaded)
			client.initClient()
	}
}
