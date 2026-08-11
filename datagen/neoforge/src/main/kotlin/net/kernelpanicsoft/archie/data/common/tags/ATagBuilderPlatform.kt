package net.kernelpanicsoft.archie.data.common.tags

import net.minecraft.data.tags.TagsProvider
import net.minecraft.tags.TagBuilder

/** NeoForge implementation of [ATagBuilderPlatform]. */
actual object ATagBuilderPlatform
{
	/** Sets the tag's `replace` flag directly via vanilla's [TagBuilder.replace], which NeoForge doesn't restrict. */
	actual fun setTagReplace(builder: TagBuilder, replace: Boolean)
	{
		builder.replace(replace)
	}

	actual fun <T : Any> createTagBuilder(parent: TagsProvider.TagAppender<T>, provider: ATagsProvider<T>): IATagBuilder<T>
	{
		return ATagBuilder(parent, provider)
	}
}