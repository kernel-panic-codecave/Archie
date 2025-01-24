package net.kernelpanicsoft.archie.data.common.tags

import net.fabricmc.fabric.impl.datagen.FabricTagBuilder
import net.minecraft.data.tags.TagsProvider
import net.minecraft.tags.*

actual object ATagBuilderPlatform
{
	@Suppress("UnstableApiUsage")
	actual fun setTagReplace(builder: TagBuilder, replace: Boolean)
	{
		(builder as FabricTagBuilder).fabric_setReplace(replace)
	}

	actual fun <T : Any> createTagBuilder(parent: TagsProvider.TagAppender<T>, provider: ATagsProvider<T>): IATagBuilder<T>
	{
		return ATagBuilder(parent, provider)
	}


}