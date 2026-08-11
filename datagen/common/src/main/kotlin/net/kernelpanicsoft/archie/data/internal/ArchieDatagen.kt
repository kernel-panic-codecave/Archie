package net.kernelpanicsoft.archie.data.internal

import net.kernelpanicsoft.archie.Archie
import net.kernelpanicsoft.archie.data.ADataGenerator
import net.kernelpanicsoft.archie.data.ADatagenEventObject
import net.kernelpanicsoft.archie.data.common.conditions.withCondition
import net.kernelpanicsoft.archie.data.common.crafting.ingredients.AComponentsIngredient
import net.kernelpanicsoft.archie.data.common.tags.ACommonTags
import net.kernelpanicsoft.archie.data.internal.common.tags.*
import net.minecraft.core.component.DataComponents
import net.minecraft.data.recipes.RecipeCategory
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.level.block.Blocks

/**
 * Archie's own datagen registration, used both to populate the vanilla-derived common ("c") tags
 * ([AInternalBlockTagsProvider] and friends) that ship with the library and as a smoke test for
 * the datagen DSL itself (e.g. the emerald-from-diamond shapeless recipe below).
 */
internal object ArchieDatagen : ADatagenEventObject(Archie.MOD)
{
	override fun ADataGenerator.handler()
	{
		client {
			languages {
				add("archie.networking.config.no_permissions", "You do not have the required permissions to edit the server config")
			}
		}
		common {
			blockTags(::AInternalBlockTagsProvider)
			itemTags(::AInternalItemTagsProvider)
			biomeTags(::AInternalBiomeTagsProvider)
			entityTags(::AInternalEntityTypeTagsProvider)
			fluidTags(::AInternalFluidTagsProvider)
		}
	}
}