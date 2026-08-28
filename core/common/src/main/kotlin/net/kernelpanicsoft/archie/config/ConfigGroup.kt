package net.kernelpanicsoft.archie.config

import net.minecraft.network.chat.Component
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

/**
 * A folder grouping other [ConfigNode]s (config specs, nested groups, and collections), declared
 * as a nested singleton `object` inside a [ConfigContainer] or another [ConfigGroup]:
 * ```kotlin
 * object Config : ConfigContainer(MyMod.MOD) {
 *     object Gui : ConfigGroup(Component.literal("GUI")) {
 *         object Theme : ConfigSpec.Client(MyMod.MOD) { ... }   // -> config/mymod/gui/theme.json5
 *     }
 * }
 * ```
 * [children] are discovered the same way [ConfigContainer.children] discovers its own - by
 * reflecting over nested objects - so there's nothing to override or register manually. This
 * group's [id] becomes one path segment for every [ConfigSpec]/[ConfigSpecCollection] nested
 * under it (however deep), and one "Edit" row drilling into a subscreen in its parent's UI.
 */
@Suppress("unused")
abstract class ConfigGroup(override val title: Component, override val id: String = title.string.toSnakeCase()) : ConfigNode
{
	/** Client-side mirror of this group, used to build its subscreen. */
	internal val client by lazy { ClientConfigGroup(this) }

	/** Nested [ConfigNode]s declared inside this group. */
	val children: List<ConfigNode>
		get() = this::class.nestedClasses
			.filterIsInstance<KClass<out ConfigNode>>()
			.filter { it.isSubclassOf(ConfigNode::class) }
			.mapNotNull { klass -> klass.objectInstance }
}
