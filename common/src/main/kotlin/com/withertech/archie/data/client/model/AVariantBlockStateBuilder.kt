package com.withertech.archie.data.client.model

import com.google.common.base.Preconditions
import com.google.common.collect.ImmutableMap
import com.google.common.collect.Lists
import com.google.common.collect.Maps
import com.google.gson.JsonObject
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.Property
import java.util.*
import java.util.function.Function
import java.util.function.Predicate

class AVariantBlockStateBuilder internal constructor(val owner: Block) : IAGeneratedBlockState
{
	private val models: MutableMap<PartialBlockstate, ABlockStateProvider.ConfiguredModelList> =
		LinkedHashMap<PartialBlockstate, ABlockStateProvider.ConfiguredModelList>()
	private val coveredStates: MutableSet<BlockState> = HashSet()

	fun getModels(): Map<PartialBlockstate, ABlockStateProvider.ConfiguredModelList>
	{
		return models
	}

	override fun toJson(): JsonObject
	{
		val missingStates: MutableList<BlockState> = Lists.newArrayList(
			owner.stateDefinition.possibleStates
		)
		missingStates.removeAll(coveredStates)
		Preconditions.checkState(
			missingStates.isEmpty(), "Blockstate for block %s does not cover all states. Missing: %s",
			owner, missingStates
		)
		val variants = JsonObject()
		getModels().entries.stream()
			.sorted(java.util.Map.Entry.comparingByKey(PartialBlockstate.comparingByProperties()))
			.forEach { entry: Map.Entry<PartialBlockstate, ABlockStateProvider.ConfiguredModelList> ->
				variants.add(
					entry.key.toString(),
					entry.value.toJSON()
				)
			}
		val main = JsonObject()
		main.add("variants", variants)
		return main
	}

	fun addModels(
		state: PartialBlockstate,
		vararg models: AConfiguredModel
	): AVariantBlockStateBuilder
	{
		Preconditions.checkNotNull(state, "state must not be null")
		Preconditions.checkArgument(models.isNotEmpty(), "Cannot set models to empty array")
		Preconditions.checkArgument(
			state.owner === owner, "Cannot set models for a different block. Found: %s, Current: %s",
			state.owner, owner
		)
		if (!this.models.containsKey(state))
		{
			Preconditions.checkArgument(
				disjointToAll(state),
				"Cannot set models for a state for which a partial match has already been configured"
			)
			this.models[state] = ABlockStateProvider.ConfiguredModelList(*models)
			for (fullState in owner.stateDefinition.possibleStates)
			{
				if (state.test(fullState))
				{
					coveredStates.add(fullState)
				}
			}
		} else
		{
			this.models.compute(state
			) { _: PartialBlockstate, cml: ABlockStateProvider.ConfiguredModelList? ->
				cml?.append(
					*models
				)
			}
		}
		return this
	}

	fun setModels(
		state: PartialBlockstate,
		vararg model: AConfiguredModel
	): AVariantBlockStateBuilder
	{
		Preconditions.checkArgument(
			!models.containsKey(state),
			"Cannot set models for a state that has already been configured: %s",
			state
		)
		addModels(state, *model)
		return this
	}

	private fun disjointToAll(newState: PartialBlockstate): Boolean
	{
		return coveredStates.stream().noneMatch(newState)
	}

	fun partialState(): PartialBlockstate
	{
		return PartialBlockstate(owner, this)
	}

	fun forAllStates(mapper: Function<BlockState, Array<AConfiguredModel>>): AVariantBlockStateBuilder
	{
		return forAllStatesExcept(mapper)
	}

	fun forAllStatesExcept(
		mapper: Function<BlockState, Array<AConfiguredModel>>,
		vararg ignored: Property<*>
	): AVariantBlockStateBuilder
	{
		val seen: MutableSet<PartialBlockstate> = HashSet()
		for (fullState in owner.stateDefinition.possibleStates)
		{
			val propertyValues: MutableMap<Property<*>, Comparable<*>> = Maps.newLinkedHashMap(fullState.values)
			for (p in ignored)
			{
				propertyValues.remove(p)
			}
			val partialState = PartialBlockstate(
				owner, propertyValues, this
			)
			if (seen.add(partialState))
			{
				setModels(partialState, *mapper.apply(fullState))
			}
		}
		return this
	}

	class PartialBlockstate internal constructor(
		val owner: Block,
		setStates: Map<Property<*>, Comparable<*>>,
		private val outerBuilder: AVariantBlockStateBuilder
	) :
		Predicate<BlockState>
	{
		val setStates: SortedMap<Property<*>, Comparable<*>>

		internal constructor(owner: Block, outerBuilder: AVariantBlockStateBuilder) : this(
			owner,
			ImmutableMap.of<Property<*>, Comparable<*>>(),
			outerBuilder
		)

		init
		{
			for (entry in setStates.entries)
			{
				val prop = entry.key
				val value = entry.value
				Preconditions.checkArgument(
					owner.stateDefinition.properties.contains(prop), "Property %s not found on block %s", entry,
					this.owner
				)
				Preconditions.checkArgument(
					prop.possibleValues.contains(value),
					"%s is not a valid value for %s",
					value,
					prop
				)
			}
			this.setStates = Maps.newTreeMap(Comparator.comparing { obj: Property<*> -> obj.name })
			this.setStates.putAll(setStates)
		}

		fun <T : Comparable<T>> with(prop: Property<T>, value: T): PartialBlockstate
		{
			Preconditions.checkArgument(!setStates.containsKey(prop), "Property %s has already been set", prop)
			val newState: MutableMap<Property<*>, Comparable<*>> = HashMap(setStates)
			newState[prop] = value
			return PartialBlockstate(owner, newState, outerBuilder)
		}

		private fun checkValidOwner()
		{
			Preconditions.checkNotNull(
				outerBuilder,
				"Partial blockstate must have a valid owner to perform this action"
			)
		}

		fun modelForState(): AConfiguredModel.Builder<AVariantBlockStateBuilder>
		{
			checkValidOwner()
			return AConfiguredModel.builder(outerBuilder, this)
		}

		fun addModels(vararg models: AConfiguredModel): PartialBlockstate
		{
			checkValidOwner()
			outerBuilder!!.addModels(this, *models)
			return this
		}

		fun setModels(vararg models: AConfiguredModel): AVariantBlockStateBuilder
		{
			checkValidOwner()
			return outerBuilder!!.setModels(this, *models)
		}

		fun partialState(): PartialBlockstate
		{
			checkValidOwner()
			return outerBuilder!!.partialState()
		}

		override fun equals(o: Any?): Boolean
		{
			if (this === o) return true
			if (o == null || javaClass != o.javaClass) return false
			val that = o as PartialBlockstate
			return owner == that.owner && setStates == that.setStates
		}

		override fun hashCode(): Int
		{
			return Objects.hash(owner, setStates)
		}

		override fun test(blockState: BlockState): Boolean
		{
			if (blockState.block !== owner)
			{
				return false
			}
			for ((key, value) in setStates)
			{
				if (blockState.getValue(key) !== value)
				{
					return false
				}
			}
			return true
		}

		override fun toString(): String
		{
			val ret = StringBuilder()
			for ((key, value) in setStates)
			{
				if (ret.isNotEmpty())
				{
					ret.append(',')
				}
				@Suppress("UNCHECKED_CAST")
				ret.append(key.name)
					.append('=')
					.append(
						(key as Property<Comparable<Any>>).getName(
							value as Comparable<Any>
						)
					)
			}
			return ret.toString()
		}

		companion object
		{
			fun comparingByProperties(): Comparator<PartialBlockstate>
			{
				// Sort variants inversely by property values, to approximate vanilla style
				return Comparator { s1: PartialBlockstate, s2: PartialBlockstate ->
					val propUniverse: SortedSet<Property<*>> =
						TreeSet(
							s1.setStates.comparator().reversed()
						)
					propUniverse.addAll(s1.setStates.keys)
					propUniverse.addAll(s2.setStates.keys)
					for (prop in propUniverse)
					{
						val val1 = s1.setStates[prop]
						val val2 = s2.setStates[prop]
						if (val1 == null && val2 != null)
						{
							return@Comparator -1
						} else if (val2 == null && val1 != null)
						{
							return@Comparator 1
						} else if (val1 != null && val2 != null)
						{
							@Suppress("UNCHECKED_CAST") val cmp = (val1 as Comparable<Any>).compareTo(val2)
							if (cmp != 0)
							{
								return@Comparator cmp
							}
						}
					}
					0
				}
			}
		}
	}
}