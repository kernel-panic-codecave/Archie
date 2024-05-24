package com.withertech.archie.data.common.tags

import dev.architectury.injectables.annotations.ExpectPlatform
import dev.architectury.platform.Mod
import net.minecraft.data.tags.TagsProvider.TagAppender
import net.minecraft.tags.TagBuilder

object TagBuilderPlatform
{
	@ExpectPlatform
	@JvmStatic
	fun setTagReplace(builder: TagBuilder, replace: Boolean)
	{
		throw AssertionError()
	}

	@ExpectPlatform
	@JvmStatic
	fun <T : Any> createTagBuilder(parent: TagAppender<T>, provider: ArchieTagsProvider<T>): IArchieTagBuilder<T>
	{
		throw AssertionError()
	}
}