package net.kernelpanicsoft.archie.data.common.tags

import net.minecraft.data.tags.TagsProvider
import net.minecraft.tags.TagBuilder

actual object ATagBuilderPlatform
{
	actual fun setTagReplace(builder: TagBuilder, replace: Boolean)
	{
		builder.replace(replace)
	}

	actual fun <T : Any> createTagBuilder(parent: TagsProvider.TagAppender<T>, provider: ATagsProvider<T>): IATagBuilder<T>
	{
		return ATagBuilder(parent, provider)
	}
}