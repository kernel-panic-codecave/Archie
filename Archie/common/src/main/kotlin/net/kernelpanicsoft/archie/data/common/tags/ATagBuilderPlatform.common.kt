package net.kernelpanicsoft.archie.data.common.tags

import net.minecraft.data.tags.TagsProvider.TagAppender
import net.minecraft.tags.TagBuilder

/** Cross-loader hooks into vanilla/loader-specific tag builder internals not otherwise exposed uniformly. */
expect object ATagBuilderPlatform
{
	/** Sets whether a tag file [replace]s (rather than merges with) tags from lower-priority datapacks. */
	fun setTagReplace(builder: TagBuilder, replace: Boolean)

	/** Creates an [IATagBuilder] wrapping [parent], the loader's native tag appender for [provider]. */
	fun <T : Any> createTagBuilder(parent: TagAppender<T>, provider: ATagsProvider<T>): IATagBuilder<T>
}