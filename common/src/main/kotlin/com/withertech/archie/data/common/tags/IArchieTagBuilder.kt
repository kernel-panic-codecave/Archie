package com.withertech.archie.data.common.tags

import net.minecraft.data.tags.TagsProvider
import net.minecraft.data.tags.TagsProvider.TagAppender
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
interface IArchieTagBuilder<T>
{
	/**
	 * Set the value of the `replace` flag in a Tag.
	 *
	 *
	 * When set to true the tag will replace any existing tag entries.
	 *
	 * @return the [IArchieTagBuilder] instance
	 */
	fun setReplace(replace: Boolean): IArchieTagBuilder<T>

	/**
	 * Set the value of the `replace` flag to true in a Tag.
	 *
	 *
	 * The tag will replace any existing tag entries.
	 *
	 * @return the [IArchieTagBuilder] instance
	 */
	fun replace(): IArchieTagBuilder<T>

	/**
	 * Add an element to the tag.
	 *
	 * @return the [IArchieTagBuilder] instance
	 */
	fun add(element: T): IArchieTagBuilder<T>

	/**
	 * Add multiple elements to the tag.
	 *
	 * @return the [IArchieTagBuilder] instance
	 */
	@SafeVarargs
	fun add(vararg elements: T): IArchieTagBuilder<T>

	/**
	 * Add an element to the tag.
	 *
	 * @return the [IArchieTagBuilder] instance
	 * @see .add
	 */
	fun add(registryKey: ResourceKey<T>): IArchieTagBuilder<T>

	/**
	 * Add a single element to the tag.
	 *
	 * @return the [IArchieTagBuilder] instance
	 */
	fun add(id: ResourceLocation): IArchieTagBuilder<T>

	/**
	 * Add an optional [ResourceLocation] to the tag.
	 *
	 * @return the [IArchieTagBuilder] instance
	 */
	fun addOptional(id: ResourceLocation): IArchieTagBuilder<T>

	/**
	 * Add an optional [ResourceKey] to the tag.
	 *
	 * @return the [IArchieTagBuilder] instance
	 */
	fun addOptional(registryKey: ResourceKey<T>): IArchieTagBuilder<T>

	fun addOptionals(vararg ids: ResourceLocation): IArchieTagBuilder<T>

	fun addOptionals(vararg keys: ResourceKey<T>): IArchieTagBuilder<T>

	/**
	 * Add another tag to this tag.
	 *
	 *
	 * **Note:** any vanilla tags can be added to the builder,
	 * but other tags can only be added if it has a builder registered in the same provider.
	 *
	 *
	 * Use [.forceAddTag] to force add any tag.
	 *
	 * @return the [IArchieTagBuilder] instance
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
	fun addTag(tag: TagKey<T>): IArchieTagBuilder<T>

	/**
	 * Add another optional tag to this tag.
	 *
	 * @return the [IArchieTagBuilder] instance
	 */
	fun addOptionalTag(id: ResourceLocation): IArchieTagBuilder<T>

	/**
	 * Add another optional tag to this tag.
	 *
	 * @return the [IArchieTagBuilder] instance
	 */
	fun addOptionalTag(tag: TagKey<T>): IArchieTagBuilder<T>

	fun addOptionalTags(vararg ids: ResourceLocation): IArchieTagBuilder<T>

	fun addOptionalTags(vararg tags: TagKey<T>): IArchieTagBuilder<T>

	/**
	 * Add multiple elements to this tag.
	 *
	 * @return the [IArchieTagBuilder] instance
	 */
	fun add(vararg ids: ResourceLocation): IArchieTagBuilder<T>

	/**
	 * Add multiple elements to this tag.
	 *
	 * @return the [IArchieTagBuilder] instance
	 */
	@SafeVarargs
	fun add(vararg registryKeys: ResourceKey<T>): IArchieTagBuilder<T>

	/**
	 * Add multiple tags to this tag.
	 *
	 * @return the [IArchieTagBuilder] instance
	 */
	fun addTags(vararg ids: ResourceLocation): IArchieTagBuilder<T>

	/**
	 * Add multiple tags to this tag.
	 *
	 * @return the [IArchieTagBuilder] instance
	 */
	@SafeVarargs
	fun addTags(vararg tagKeys: TagKey<T>): IArchieTagBuilder<T>
}