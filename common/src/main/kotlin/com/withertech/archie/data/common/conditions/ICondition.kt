package com.withertech.archie.data.common.conditions

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import kotlinx.serialization.Serializable
import net.minecraft.core.Holder
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import kotlin.reflect.KClass

interface ICondition
{
	fun test(context: IContext): Boolean

	val codec: MapCodec<out ICondition>

	val identifier: ResourceLocation

	interface IContext
	{
		/**
		 * Return the requested tag if available, or an empty tag otherwise.
		 */
		fun <T> getTag(key: TagKey<T>): Collection<Holder<T>>
		{
			return getAllTags(key.registry()).getOrDefault(key.location(), setOf<Holder<T>>())
		}

		/**
		 * Return all the loaded tags for the passed registry, or an empty map if none is available.
		 * Note that the map and the tags are unmodifiable.
		 */
		fun <T> getAllTags(registry: ResourceKey<out Registry<T>>): Map<ResourceLocation, Collection<Holder<T>>>

		/**
		 * Return the registry entry for the specified [ResourceKey]
		 */
		fun <T> getRegistryEntry(key: ResourceKey<T>) : T?
		{
			return getRegistry<T>(ResourceKey.createRegistryKey(key.registry()))[key]
		}

		/**
		 * Return the registry entry for the given [ResourceLocation] in the [Registry] specified by the registry key
		 */
		fun <T> getRegistryEntry(registry: ResourceKey<out Registry<T>>, key: ResourceLocation): T?
		{
			return getRegistryEntry(ResourceKey.create(registry, key))
		}

		/**
		 * Return the [Registry] for the given registry key
		 */
		fun <T> getRegistry(registry: ResourceKey<out Registry<T>>): Registry<T>
	}

	companion object
	{
		inline fun <reified T : ICondition> register(identifier: ResourceLocation, codec: MapCodec<T>)
		{
			ConditionsPlatform.register(identifier, codec)
		}

		val CODEC: Codec<ICondition>
			get() = ConditionsPlatform.codec()
	}
}