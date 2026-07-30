package net.kernelpanicsoft.archie.data.common.crafting

import net.kernelpanicsoft.archie.Archie
import net.kernelpanicsoft.archie.data.IADataProvider
import net.kernelpanicsoft.archie.data.common.conditions.AConditionsPlatform
import net.kernelpanicsoft.archie.data.common.crafting.recipies.ArchieCookingRecipeBuilder
import net.kernelpanicsoft.archie.data.common.crafting.recipies.ArchieShapedRecipeBuilder
import net.kernelpanicsoft.archie.data.common.crafting.recipies.ArchieShapelessRecipeBuilder
import dev.architectury.platform.Mod
import net.minecraft.core.HolderLookup
import net.minecraft.data.CachedOutput
import net.minecraft.data.PackOutput
import net.minecraft.data.recipes.*
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item
import net.minecraft.world.item.crafting.BlastingRecipe
import net.minecraft.world.item.crafting.CampfireCookingRecipe
import net.minecraft.world.item.crafting.SmeltingRecipe
import net.minecraft.world.item.crafting.SmokingRecipe
import net.minecraft.world.level.ItemLike
import java.util.concurrent.CompletableFuture
import kotlin.system.exitProcess

/**
 * Datagen provider that builds recipe JSONs on top of vanilla's [RecipeProvider]. Implement
 * [generate] and use the `shaped`/`shapeless`/`smelting`/`blasting`/`smoking`/`cooking` DSL
 * helpers, or vanilla's [RecipeBuilder]s directly, to register recipes into the given
 * [RecipeOutput]. Use via [net.kernelpanicsoft.archie.data.ADataGenerator.Common.recipes].
 */
@Suppress("unused")
abstract class ARecipeProvider(
	override val output: PackOutput,
	override val mod: Mod,
	registries: CompletableFuture<HolderLookup.Provider>,
	override val exitOnError: Boolean
) : RecipeProvider(output, registries),
	IADataProvider
{

	/** On Fabric, a wrapping [RecipeProvider] needed for [AConditionsPlatform.withCondition] support; `null` elsewhere. */
	private val fabricParent: RecipeProvider? = AConditionsPlatform.fabricRecipeProvider(this, registries)

	override fun run(cachedOutput: CachedOutput): CompletableFuture<*>
	{
		return if (fabricParent != null)
			fabricParent.run(cachedOutput)
		else
			super.run(cachedOutput)
	}

	final override fun buildRecipes(recipeOutput: RecipeOutput)
	{
		runCatching {
			generate(recipeOutput)
		}.onFailure {
			Archie.LOGGER.error(
				"Data Provider $name failed with exception: ${it.message}\n" +
						"Stacktrace: ${it.stackTraceToString()}"
			)
			if (exitOnError) exitProcess(-1)
		}
	}

	/** Called once during [buildRecipes] to register recipes into [recipeOutput]. */
	abstract fun generate(recipeOutput: RecipeOutput)

	override fun getName(): String = format("Recipes")

	/** Builds a shaped crafting recipe declared in [block]. */
	fun shaped(
		block: ArchieShapedRecipeBuilder.() -> Unit
	): ArchieShapedRecipeBuilder = ArchieShapedRecipeBuilder.shaped(block)

	/** Builds a shapeless crafting recipe declared in [block]. */
	fun shapeless(
		block: ArchieShapelessRecipeBuilder.() -> Unit
	): ArchieShapelessRecipeBuilder = ArchieShapelessRecipeBuilder.shapeless(block)

	/** Builds a furnace smelting recipe declared in [block]. */
	fun smelting(
		block: ArchieCookingRecipeBuilder<SmeltingRecipe>.() -> Unit
	): ArchieCookingRecipeBuilder<SmeltingRecipe> = ArchieCookingRecipeBuilder.smelting(block)

	/** Builds a blast furnace recipe declared in [block]. */
	fun blasting(
		block: ArchieCookingRecipeBuilder<BlastingRecipe>.() -> Unit
	): ArchieCookingRecipeBuilder<BlastingRecipe> = ArchieCookingRecipeBuilder.blasting(block)

	/** Builds a smoker recipe declared in [block]. */
	fun smoking(
		block: ArchieCookingRecipeBuilder<SmokingRecipe>.() -> Unit
	): ArchieCookingRecipeBuilder<SmokingRecipe> = ArchieCookingRecipeBuilder.smoking(block)

	/** Builds a campfire cooking recipe declared in [block]. */
	fun cooking(
		block: ArchieCookingRecipeBuilder<CampfireCookingRecipe>.() -> Unit
	): ArchieCookingRecipeBuilder<CampfireCookingRecipe> = ArchieCookingRecipeBuilder.cooking(block)

	/** Adds an unlock criterion requiring [ingredient] to have been obtained, named after it. */
	@Suppress("UNCHECKED_CAST")
	fun <T : RecipeBuilder> T.unlockedBy(ingredient: ItemLike): T =
		unlockedBy(getHasName(ingredient), has(ingredient)) as T

	/** Adds an unlock criterion requiring any item in [tag] to have been obtained, named `has_<tag path>`. */
	@Suppress("UNCHECKED_CAST")
	fun <T : RecipeBuilder> T.unlockedBy(tag: TagKey<Item>): T =
		unlockedBy("has_${tag.location.path}", has(tag)) as T

}