package net.kernelpanicsoft.archie.data.common.tags

import net.minecraft.data.tags.TagsProvider
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagEntry
import net.minecraft.tags.TagKey
import java.util.function.Consumer
import java.util.function.Predicate
import java.util.stream.Stream

class ATagBuilder<T : Any>(private val parent: TagsProvider.TagAppender<T>, private val provider: ATagsProvider<T>) :
	TagsProvider.TagAppender<T>(parent.builder), IATagBuilder<T>
{
	override fun setReplace(replace: Boolean): ATagBuilder<T>
	{
		ATagBuilderPlatform.setTagReplace(builder, replace)
		return this
	}

	override fun replace(): ATagBuilder<T>
	{
		return setReplace(true)
	}

	override fun add(element: T): ATagBuilder<T>
	{
		add(provider.reverseLookup(element))
		return this
	}

	@SafeVarargs
	override fun add(vararg elements: T): ATagBuilder<T>
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

	override fun add(registryKey: ResourceKey<T>): ATagBuilder<T>
	{
		parent.add(registryKey)
		return this
	}

	override fun add(id: ResourceLocation): ATagBuilder<T>
	{
		builder.addElement(id)
		return this
	}

	override fun addOptional(id: ResourceLocation): ATagBuilder<T>
	{
		parent.addOptional(id)
		return this
	}

	override fun addOptional(registryKey: ResourceKey<T>): ATagBuilder<T>
	{
		return addOptional(registryKey.location())
	}

	override fun addOptionals(vararg ids: ResourceLocation): ATagBuilder<T>
	{
		ids.forEach(this::addOptional)
		return this
	}

	override fun addOptionals(vararg keys: ResourceKey<T>): ATagBuilder<T>
	{
		keys.forEach(this::addOptional)
		return this
	}

	override fun addTag(tag: TagKey<T>): ATagBuilder<T>
	{
		builder.add(ForcedTagEntry(TagEntry.element(tag.location())))
		return this
	}

	override fun addOptionalTag(id: ResourceLocation): ATagBuilder<T>
	{
		parent.addOptionalTag(id)
		return this
	}

	override fun addOptionalTag(tag: TagKey<T>): ATagBuilder<T>
	{
		return addOptionalTag(tag.location())
	}

	override fun addOptionalTags(vararg ids: ResourceLocation): ATagBuilder<T>
	{
		ids.forEach(this::addOptionalTag)
		return this
	}

	override fun addOptionalTags(vararg tags: TagKey<T>): ATagBuilder<T>
	{
		tags.forEach(this::addOptionalTag)
		return this
	}

	override fun add(vararg ids: ResourceLocation): ATagBuilder<T>
	{
		for (id in ids)
		{
			add(id)
		}

		return this
	}

	@SafeVarargs
	override fun add(vararg registryKeys: ResourceKey<T>): ATagBuilder<T>
	{
		for (registryKey in registryKeys)
		{
			add(registryKey)
		}

		return this
	}

	override fun addTags(vararg ids: ResourceLocation): ATagBuilder<T>
	{
		for (id in ids)
		{
			builder.addTag(id)
		}

		return this
	}

	@SafeVarargs
	override fun addTags(vararg tagKeys: TagKey<T>): ATagBuilder<T>
	{
		for (tagKey in tagKeys)
		{
			addTag(tagKey)
		}

		return this
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