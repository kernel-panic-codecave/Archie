package com.withertech.archie.data.common.tags

import net.minecraft.data.tags.TagsProvider.TagAppender
import net.minecraft.tags.TagBuilder

expect object ATagBuilderPlatform
{
	fun setTagReplace(builder: TagBuilder, replace: Boolean)

	fun <T : Any> createTagBuilder(parent: TagAppender<T>, provider: ATagsProvider<T>): IArchieTagBuilder<T>
}