package net.kernelpanicsoft.archie.config

import net.minecraft.network.chat.Component
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

/**
 * A top-level section of a [ConfigSpec], or a nested subsection of another [CategorySpec].
 * Declared as a nested singleton `object` inside a [ConfigSpec] or a parent [CategorySpec] -
 * [ConfigSpec.categories] and [subcategories] both discover their members by reflecting over
 * nested objects, so there's nothing to override or register manually.
 */
abstract class CategorySpec(title: Component, id: String = title.string.toSnakeCase()) : DataSpec(title, id)
{
	/** Nested [CategorySpec] objects declared inside this one, for grouping in the UI. */
	val subcategories: List<CategorySpec>
		get() = this::class.nestedClasses
			.filterIsInstance<KClass<out CategorySpec>>()
			.filter { it.isSubclassOf(CategorySpec::class) }
			.mapNotNull { klass -> klass.objectInstance }

	override fun init()
	{
		super.init()
		subcategories.forEach { cat ->
			types[cat.id] = FieldType.Category(cat)
			if (cat.subcategories.isNotEmpty())
				cat.init()
		}
	}
}