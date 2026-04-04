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

internal object ArchieDatagen : ADatagenEventObject(Archie.MOD)
{
	override fun ADataGenerator.handler()
	{
		client {
			blockStates {
				simpleBlock(Blocks.COBBLESTONE)
			}
		}
		common {
			blockTags(::AInternalBlockTagsProvider)
			itemTags(::AInternalItemTagsProvider)
			biomeTags(::AInternalBiomeTagsProvider)
			entityTags(::AInternalEntityTypeTagsProvider)
			fluidTags(::AInternalFluidTagsProvider)

			recipes {
				shapeless {
					ingredients {
						1 of AComponentsIngredient.of(
							Ingredient.of(ACommonTags.Items.GEMS_DIAMOND)
						) {
							set(DataComponents.CUSTOM_NAME, Component.literal("Emerald"))
						}
					}

					result = Items.EMERALD
					category = RecipeCategory.MISC
					unlockedBy(ACommonTags.Items.GEMS_DIAMOND)
					save(it.withCondition {
						platform(FABRIC) or TRUE
					})
				}
			}
		}
	}
}