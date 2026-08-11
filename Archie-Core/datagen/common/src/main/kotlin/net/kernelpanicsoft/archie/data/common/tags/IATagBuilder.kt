package net.kernelpanicsoft.archie.data.common.tags

import net.minecraft.data.tags.TagsProvider
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.BiomeTags
import net.minecraft.tags.BlockTags
import net.minecraft.tags.EntityTypeTags
import net.minecraft.tags.FluidTags
import net.minecraft.tags.ItemTags
import net.minecraft.tags.TagKey



/**
 * An extension to [TagsProvider.TagAppender] that provides additional functionality.
 */
interface IATagBuilder<T : Any>
{
	/**
	 * Set the value of the `replace` flag in a Tag.
	 *
	 *
	 * When set to true the tag will replace any existing tag entries.
	 *
	 * @return the [IATagBuilder] instance
	 */
	fun setReplace(replace: Boolean): IATagBuilder<T>

	/**
	 * Set the value of the `replace` flag to true in a Tag.
	 *
	 *
	 * The tag will replace any existing tag entries.
	 *
	 * @return the [IATagBuilder] instance
	 */
	fun replace(): IATagBuilder<T>

	/**
	 * Add an element to the tag.
	 *
	 * @return the [IATagBuilder] instance
	 */
	fun add(element: T): IATagBuilder<T>

	/**
	 * Add multiple elements to the tag.
	 *
	 * @return the [IATagBuilder] instance
	 */
	@SafeVarargs
	fun add(vararg elements: T): IATagBuilder<T>

	/**
	 * Add an element to the tag.
	 *
	 * @return the [IATagBuilder] instance
	 */
	fun add(registryKey: ResourceKey<T>): IATagBuilder<T>

	/**
	 * Add a single element to the tag.
	 *
	 * @return the [IATagBuilder] instance
	 */
	fun add(id: ResourceLocation): IATagBuilder<T>

	/**
	 * Add an optional [ResourceLocation] to the tag.
	 *
	 * @return the [IATagBuilder] instance
	 */
	fun addOptional(id: ResourceLocation): IATagBuilder<T>

	/**
	 * Add an optional [ResourceKey] to the tag.
	 *
	 * @return the [IATagBuilder] instance
	 */
	fun addOptional(registryKey: ResourceKey<T>): IATagBuilder<T>

	/** Add multiple optional [ResourceLocation]s to the tag. */
	fun addOptionals(vararg ids: ResourceLocation): IATagBuilder<T>

	/** Add multiple optional [ResourceKey]s to the tag. */
	fun addOptionals(vararg keys: ResourceKey<T>): IATagBuilder<T>

	/**
	 * Add all elements of [tag] to this tag, unconditionally (unlike [addTags], this does not
	 * require [tag] to be defined by a known builder or vanilla tag).
	 *
	 * @return the [IATagBuilder] instance
	 * @see BlockTags
	 *
	 * @see EntityTypeTags
	 *
	 * @see FluidTags
	 *
	 * @see BiomeTags
	 *
	 * @see ItemTags
	 */
	fun addTag(tag: TagKey<T>): IATagBuilder<T>

	/**
	 * Add another optional tag to this tag.
	 *
	 * @return the [IATagBuilder] instance
	 */
	fun addOptionalTag(id: ResourceLocation): IATagBuilder<T>

	/**
	 * Add another optional tag to this tag.
	 *
	 * @return the [IATagBuilder] instance
	 */
	fun addOptionalTag(tag: TagKey<T>): IATagBuilder<T>

	/** Add multiple optional tags, by id, to this tag. */
	fun addOptionalTags(vararg ids: ResourceLocation): IATagBuilder<T>

	/** Add multiple optional tags to this tag. */
	fun addOptionalTags(vararg tags: TagKey<T>): IATagBuilder<T>

	/**
	 * Add multiple elements to this tag.
	 *
	 * @return the [IATagBuilder] instance
	 */
	fun add(vararg ids: ResourceLocation): IATagBuilder<T>

	/**
	 * Add multiple elements to this tag.
	 *
	 * @return the [IATagBuilder] instance
	 */
	@SafeVarargs
	fun add(vararg registryKeys: ResourceKey<T>): IATagBuilder<T>

	/**
	 * Add multiple tags to this tag.
	 *
	 * @return the [IATagBuilder] instance
	 */
	fun addTags(vararg ids: ResourceLocation): IATagBuilder<T>

	/**
	 * Add multiple tags to this tag.
	 *
	 * @return the [IATagBuilder] instance
	 */
	@SafeVarargs
	fun addTags(vararg tagKeys: TagKey<T>): IATagBuilder<T>
}