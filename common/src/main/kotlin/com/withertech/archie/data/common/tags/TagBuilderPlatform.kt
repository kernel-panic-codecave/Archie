package com.withertech.archie.data.common.tags

import dev.architectury.platform.Mod
import net.minecraft.data.tags.TagsProvider.TagAppender
import net.minecraft.tags.TagBuilder

expect object TagBuilderPlatform
{
	fun setTagReplace(builder: TagBuilder, replace: Boolean)

	fun <T : Any> createTagBuilder(parent: TagAppender<T>, provider: ArchieTagsProvider<T>): IArchieTagBuilder<T>
}