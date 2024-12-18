package com.withertech.archie.data.common.tags

import net.fabricmc.fabric.impl.datagen.FabricTagBuilder
import net.minecraft.data.tags.TagsProvider
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.*
import java.util.function.Consumer
import java.util.function.Predicate
import java.util.stream.Stream

actual object ATagBuilderPlatform
{
	@Suppress("UnstableApiUsage")
	actual fun setTagReplace(builder: TagBuilder, replace: Boolean)
	{
		(builder as FabricTagBuilder).fabric_setReplace(replace)
	}

	actual fun <T : Any> createTagBuilder(parent: TagsProvider.TagAppender<T>, provider: ATagsProvider<T>): IArchieTagBuilder<T>
	{
		return ArchieTagBuilder(parent, provider)
	}

	class ArchieTagBuilder<T : Any>(private val parent: TagsProvider.TagAppender<T>, private val provider: ATagsProvider<T>) :
		TagsProvider.TagAppender<T>(parent.builder), IArchieTagBuilder<T>
	{
		override fun setReplace(replace: Boolean): ArchieTagBuilder<T>
		{
			setTagReplace(builder, replace)
			return this
		}

		override fun replace(): ArchieTagBuilder<T>
		{
			return setReplace(true)
		}

		override fun add(element: T): ArchieTagBuilder<T>
		{
			add(provider.reverseLookup(element))
			return this
		}

		@SafeVarargs
		override fun add(vararg elements: T): ArchieTagBuilder<T>
		{
			Stream.of(*elements).map { element: T ->
				provider.reverseLookup(
					element
				)
			}.forEach { registryKey: ResourceKey<T> ->
				this.add(
					registryKey
				)
			}
			return this
		}

		override fun add(registryKey: ResourceKey<T>): ArchieTagBuilder<T>
		{
			parent.add(registryKey)
			return this
		}

		override fun add(id: ResourceLocation): ArchieTagBuilder<T>
		{
			builder.addElement(id)
			return this
		}

		override fun addOptional(id: ResourceLocation): ArchieTagBuilder<T>
		{
			parent.addOptional(id)
			return this
		}

		override fun addOptional(registryKey: ResourceKey<T>): ArchieTagBuilder<T>
		{
			return addOptional(registryKey.location())
		}

		override fun addOptionals(vararg ids: ResourceLocation): ArchieTagBuilder<T>
		{
			ids.forEach(this::addOptional)
			return this
		}

		override fun addOptionals(vararg keys: ResourceKey<T>): ArchieTagBuilder<T>
		{
			keys.forEach(this::addOptional)
			return this
		}

		override fun addTag(tag: TagKey<T>): ArchieTagBuilder<T>
		{
			builder.add(ForcedTagEntry(TagEntry.element(tag.location())))
			return this
		}

		override fun addOptionalTag(id: ResourceLocation): ArchieTagBuilder<T>
		{
			parent.addOptionalTag(id)
			return this
		}

		override fun addOptionalTag(tag: TagKey<T>): ArchieTagBuilder<T>
		{
			return addOptionalTag(tag.location())
		}

		override fun addOptionalTags(vararg ids: ResourceLocation): ArchieTagBuilder<T>
		{
			ids.forEach(this::addOptionalTag)
			return this
		}

		override fun addOptionalTags(vararg tags: TagKey<T>): ArchieTagBuilder<T>
		{
			tags.forEach(this::addOptionalTag)
			return this
		}

		override fun add(vararg ids: ResourceLocation): ArchieTagBuilder<T>
		{
			for (id in ids)
			{
				add(id)
			}

			return this
		}

		@SafeVarargs
		override fun add(vararg registryKeys: ResourceKey<T>): ArchieTagBuilder<T>
		{
			for (registryKey in registryKeys)
			{
				add(registryKey)
			}

			return this
		}

		override fun addTags(vararg ids: ResourceLocation): ArchieTagBuilder<T>
		{
			for (id in ids)
			{
				builder.addTag(id)
			}

			return this
		}

		@SafeVarargs
		override fun addTags(vararg tagKeys: TagKey<T>): ArchieTagBuilder<T>
		{
			for (tagKey in tagKeys)
			{
				addTag(tagKey)
			}

			return this
		}
	}

	class ForcedTagEntry(private val delegate: TagEntry) :
		TagEntry(delegate.id, true, delegate.required)
	{
		override fun <T> build(arg: Lookup<T>, consumer: Consumer<T>): Boolean
		{
			return delegate.build(arg, consumer)
		}

		override fun verifyIfPresent(
			objectExistsTest: Predicate<ResourceLocation>,
			tagExistsTest: Predicate<ResourceLocation>
		): Boolean
		{
			return true
		}
	}
}