package net.kernelpanicsoft.archie.data.client.model

import com.google.common.base.Preconditions
import com.google.common.collect.Multimap
import com.google.common.collect.MultimapBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.properties.Property
import java.util.*

/**
 * Builds a `multipart`-style blockstate JSON for [owner], where each [PartBuilder] applies its
 * model(s) whenever its `when` conditions (block property values, optionally grouped with
 * AND/OR) match. Obtain via [ABlockStateProvider.getMultipartBuilder].
 */
class AMultiPartBlockStateBuilder(private val owner: Block) : IAGeneratedBlockState
{
	private val parts: MutableList<PartBuilder> = ArrayList()

	private var config: (PartBuilder.() -> Unit)? = null

	/** Starts an [AConfiguredModel.Builder] whose [AConfiguredModel.Builder.addModel] creates and adds a new unconditional [PartBuilder]. */
	fun part(): AConfiguredModel.Builder<PartBuilder>
	{
		return AConfiguredModel.builder(this)
	}

	/** Builds a new [PartBuilder] with its model(s) declared in [block], adds it, and applies any pending [configure] callback. */
	fun part(block: AConfiguredModel.Builder<PartBuilder>.() -> Unit): PartBuilder
	{
		return AConfiguredModel.builder(this)
			.apply(block)
			.addModel()
			.apply {
				config?.let { it() }
			}
	}

	/** Registers an already-built [part]. */
	fun addPart(part: PartBuilder): AMultiPartBlockStateBuilder
	{
		parts.add(part)
		return this
	}

	/** Sets a callback run against every part built by [part] after this call, e.g. to add shared conditions. */
	fun configure(block: (PartBuilder.() -> Unit)?)
	{
		config = block
	}

	/** Serializes to the `{"multipart": [...]}` blockstate JSON. */
	override fun toJson(): JsonObject
	{
		val variants = JsonArray()
		for (part in parts)
		{
			variants.add(part.toJson())
		}
		val main = JsonObject()
		main.add("multipart", variants)
		return main
	}

	/** A single `multipart` entry: applies [models] when its (possibly nested) conditions match. */
	inner class PartBuilder internal constructor(models: ABlockStateProvider.ConfiguredModelList)
	{
		var models: ABlockStateProvider.ConfiguredModelList = models
		var useOr: Boolean = false
		val conditions: Multimap<Property<*>, Comparable<*>> =
			MultimapBuilder.linkedHashKeys().arrayListValues().build()
		val nestedConditionGroups: MutableList<ConditionGroup> = ArrayList()

		/** Combines this part's [conditions] with OR instead of the default AND. */
		fun useOr(): PartBuilder
		{
			this.useOr = true
			return this
		}

		/** Requires [prop] to equal one of [values] (OR'd together) for this part to apply. Cannot be mixed with [nestedGroup]. */
		@SafeVarargs
		fun <T : Comparable<T>> condition(prop: Property<T>, vararg values: T): PartBuilder
		{
			Preconditions.checkNotNull(prop, "Property must not be null")
			Preconditions.checkNotNull(values, "Value list must not be null")
			Preconditions.checkArgument(values.isNotEmpty(), "Value list must not be empty")
			Preconditions.checkArgument(
				!conditions.containsKey(prop),
				"Cannot set condition for property \"%s\" more than once",
				prop.name
			)
			Preconditions.checkArgument(
				canApplyTo(owner), "IProperty %s is not valid for the block %s", prop,
				owner
			)
			Preconditions.checkState(
				nestedConditionGroups.isEmpty(),
				"Can't have normal conditions if there are already nested condition groups"
			)
			conditions.putAll(prop, listOf(*values))
			return this
		}

		/** Starts a nested [ConditionGroup] under this part. Cannot be mixed with [condition]. */
		fun nestedGroup(): ConditionGroup
		{
			Preconditions.checkState(
				conditions.isEmpty,
				"Can't have nested condition groups if there are already normal conditions"
			)
			val group = ConditionGroup()
			nestedConditionGroups.add(group)
			return group
		}

		/** Returns to the enclosing [AMultiPartBlockStateBuilder]. */
		fun end(): AMultiPartBlockStateBuilder
		{
			return this@AMultiPartBlockStateBuilder
		}

		/** Serializes this part's `when`/`apply` entry. */
		fun toJson(): JsonObject
		{
			val out = JsonObject()
			if (!conditions.isEmpty)
			{
				out.add("when", toJson(this.conditions, this.useOr))
			} else if (nestedConditionGroups.isNotEmpty())
			{
				out.add("when", toJson(this.nestedConditionGroups, this.useOr))
			}
			out.add("apply", models.toJSON())
			return out
		}

		/** Whether every property referenced by this part's conditions exists on [b]. */
		fun canApplyTo(b: Block): Boolean
		{
			return b.stateDefinition.properties.containsAll(conditions.keySet())
		}

		/** A nested AND/OR group of conditions within a [PartBuilder]'s `when` clause. */
		inner class ConditionGroup
		{
			val conditions: Multimap<Property<*>, Comparable<*>> =
				MultimapBuilder.linkedHashKeys().arrayListValues().build()
			val nestedConditionGroups: MutableList<ConditionGroup> = ArrayList()
			private var parent: ConditionGroup? = null
			var useOr: Boolean = false

			@SafeVarargs
			fun <T : Comparable<T>?> condition(prop: Property<T>, vararg values: T): ConditionGroup
			{
				Preconditions.checkNotNull(prop, "Property must not be null")
				Preconditions.checkNotNull(values, "Value list must not be null")
				Preconditions.checkArgument(values.isNotEmpty(), "Value list must not be empty")
				Preconditions.checkArgument(
					!conditions.containsKey(prop),
					"Cannot set condition for property \"%s\" more than once",
					prop.name
				)
				Preconditions.checkArgument(
					canApplyTo(owner), "IProperty %s is not valid for the block %s", prop,
					owner
				)
				Preconditions.checkState(
					nestedConditionGroups.isEmpty(),
					"Can't have normal conditions if there are already nested condition groups"
				)
				this.conditions.putAll(prop, listOf(*values))
				return this
			}

			fun nestedGroup(): ConditionGroup
			{
				Preconditions.checkState(
					conditions.isEmpty,
					"Can't have nested condition groups if there are already normal conditions"
				)
				val group = ConditionGroup()
				group.parent = this
				this.nestedConditionGroups.add(group)
				return group
			}

			fun endNestedGroup(): ConditionGroup
			{
				checkNotNull(parent) { "This condition group is not nested, use end() instead" }
				return parent!!
			}

			fun end(): PartBuilder
			{
				check(this.parent == null) { "This is a nested condition group, use endNestedGroup() instead" }
				return this@PartBuilder
			}

			fun useOr(): ConditionGroup
			{
				this.useOr = true
				return this
			}

			fun toJson(): JsonObject
			{
				if (!this.conditions.isEmpty)
				{
					return toJson(this.conditions, this.useOr)
				} else if (this.nestedConditionGroups.isNotEmpty())
				{
					return toJson(this.nestedConditionGroups, this.useOr)
				}
				return JsonObject()
			}
		}
	}

	companion object
	{
		@Suppress("UNCHECKED_CAST")
		private fun propertyValueName(key: Property<*>, value: Comparable<*>): String
		{
			val typedKey = key as Property<Comparable<Any?>>
			val typedValue = value as Comparable<Any?>
			return typedKey.getName(typedValue)
		}

		private fun toJson(conditions: List<PartBuilder.ConditionGroup>, useOr: Boolean): JsonObject
		{
			val groupJson = JsonObject()
			val innerGroupJson = JsonArray()
			groupJson.add(if (useOr) "OR" else "AND", innerGroupJson)
			for (group in conditions)
			{
				innerGroupJson.add(group.toJson())
			}
			return groupJson
		}

		private fun toJson(conditions: Multimap<Property<*>, Comparable<*>>, useOr: Boolean): JsonObject
		{
			var groupJson = JsonObject()
			for ((key, value) in conditions.asMap())
			{
				val activeString = StringBuilder()
				for (`val` in value)
				{
					if (activeString.isNotEmpty()) activeString.append("|")
					activeString.append(propertyValueName(key, `val`))
				}
				groupJson.addProperty(key.name, activeString.toString())
			}
			if (useOr)
			{
				val innerWhen = JsonArray()
				for ((key, value) in groupJson.entrySet())
				{
					val obj = JsonObject()
					obj.add(key, value)
					innerWhen.add(obj)
				}
				groupJson = JsonObject()
				groupJson.add("OR", innerWhen)
			}
			return groupJson
		}
	}
}