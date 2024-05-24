package com.withertech.archie.data.internal

import com.withertech.archie.Archie
import com.withertech.archie.data.common.crafting.ingredients.ComponentsIngredient
import com.withertech.archie.data.common.tags.CommonTags
import com.withertech.archie.data.internal.common.tags.*
import com.withertech.archie.events.ArchieEvents
import net.minecraft.core.component.DataComponents
import net.minecraft.data.recipes.RecipeCategory
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.level.block.Blocks

internal object ArchieDatagen
{
	fun init()
	{
		ArchieEvents.GATHER_DATA.register(ArchieEvents.GatherDataHandler.create(Archie.MOD) {
			client {
				blockStates {
					simpleBlock(Blocks.COBBLESTONE)
				}
			}
			common {
				blockTags(::ArchieInternalBlockTagsProvider)
				itemTags(::ArchieInternalItemTagsProvider)
				biomeTags(::ArchieInternalBiomeTagsProvider)
				entityTags(::ArchieInternalEntityTypeTagsProvider)
				fluidTags(::ArchieInternalFluidTagsProvider)

				recipes {
					shapeless {
						ingredients {
							1 of ComponentsIngredient.of(
								Ingredient.of(CommonTags.Items.GEMS_DIAMOND)
							) {
								set(DataComponents.CUSTOM_NAME, Component.literal("Emerald"))
							}
						}

						result = Items.EMERALD
						category = RecipeCategory.MISC
						unlockedBy(CommonTags.Items.GEMS_DIAMOND)
						save(it)
					}
				}
			}
		})
	}
}