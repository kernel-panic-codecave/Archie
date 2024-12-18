package com.withertech.archie.data.client.model

import com.google.common.base.Preconditions
import com.google.common.collect.Multimap
import com.google.common.collect.MultimapBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.properties.Property
import java.util.*

class AMultiPartBlockStateBuilder(private val owner: Block) : IAGeneratedBlockState
{
	private val parts: MutableList<PartBuilder> = ArrayList()

	private var config: (PartBuilder.() -> Unit)? = null
	fun part(): AConfiguredModel.Builder<PartBuilder>
	{
		return AConfiguredModel.builder(this)
	}

	fun part(block: AConfiguredModel.Builder<PartBuilder>.() -> Unit): PartBuilder
	{
		return AConfiguredModel.builder(this)
			.apply(block)
			.addModel()
			.apply {
				config?.let { it() }
			}
	}

	fun addPart(part: PartBuilder): AMultiPartBlockStateBuilder
	{
		parts.add(part)
		return this
	}

	fun configure(block: (PartBuilder.() -> Unit)?)
	{
		config = block
	}

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

	inner class PartBuilder internal constructor(models: ABlockStateProvider.ConfiguredModelList)
	{
		var models: ABlockStateProvider.ConfiguredModelList = models
		var useOr: Boolean = false
		val conditions: Multimap<Property<*>, Comparable<*>> =
			MultimapBuilder.linkedHashKeys().arrayListValues().build()
		val nestedConditionGroups: MutableList<ConditionGroup> = ArrayList()

		fun useOr(): PartBuilder
		{
			this.useOr = true
			return this
		}

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

		fun end(): AMultiPartBlockStateBuilder
		{
			return this@AMultiPartBlockStateBuilder
		}

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

		fun canApplyTo(b: Block): Boolean
		{
			return b.stateDefinition.properties.containsAll(conditions.keySet())
		}

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
					activeString.append((key as Property<Comparable<Any?>>).getName(`val` as Comparable<Any?>))
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