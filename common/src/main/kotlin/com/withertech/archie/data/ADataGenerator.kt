package com.withertech.archie.data

import com.withertech.archie.data.client.ALanguageProvider
import com.withertech.archie.data.client.model.ABlockModelProvider
import com.withertech.archie.data.client.model.ABlockStateProvider
import com.withertech.archie.data.client.model.AItemModelProvider
import com.withertech.archie.data.common.crafting.ARecipeProvider
import com.withertech.archie.data.common.tags.ATagsProvider
import dev.architectury.platform.Mod
import net.minecraft.core.HolderLookup
import net.minecraft.data.DataProvider
import net.minecraft.data.PackOutput
import net.minecraft.data.recipes.RecipeOutput
import java.util.concurrent.CompletableFuture

@Suppress("MemberVisibilityCanBePrivate", "unused")
abstract class ADataGenerator
{
	val isClient: Boolean
		get() = System.getProperty("archie.datagen.client").toBoolean()
	val isServer: Boolean
		get() = System.getProperty("archie.datagen.server").toBoolean()

	abstract val mod: Mod

	abstract fun <T : DataProvider> addProvider(
		run: Boolean = true,
		factory: ARegistryAwareDataProviderFactory<T>
	): T

	fun <T : DataProvider> addProvider(run: Boolean = true, factory: ADataProviderFactory<T>): T
	{
		return addProvider(run) { output, _ ->
			factory(output)
		}
	}

	fun client(block: Client.() -> Unit)
	{
		Client().apply(block)
	}

	fun common(block: Common.() -> Unit)
	{
		Common().apply(block)
	}

	operator fun invoke(block: ADataGenerator.() -> Unit) = apply(block)

	fun interface ADataProviderFactory<T : DataProvider>
	{
		operator fun invoke(output: PackOutput): T
	}

	fun interface ARegistryAwareDataProviderFactory<T : DataProvider>
	{
		operator fun invoke(output: PackOutput, registries: CompletableFuture<HolderLookup.Provider>): T
	}

	fun interface ItemTagsDataProviderFactory
	{
		operator fun invoke(
			output: PackOutput,
			registries: CompletableFuture<HolderLookup.Provider>,
			blockTagsProvider: ATagsProvider.BlockTagsProvider
		): ATagsProvider.ItemTagsProvider
	}

	inner class Client
	{
		fun languages(locale: String = "en_us", block: ALanguageProvider.() -> Unit): ALanguageProvider
		{
			return languages { packOutput ->
				object : ALanguageProvider(packOutput, mod, false, locale)
				{
					override fun generate()
					{
						this.block()
					}
				}
			}
		}

		fun languages(constructor: ADataProviderFactory<ALanguageProvider>): ALanguageProvider
		{
			return addProvider(isClient, constructor)
		}

		fun blockModels(block: ABlockModelProvider.() -> Unit): ABlockModelProvider
		{
			return blockModels { packOutput ->
				object : ABlockModelProvider(packOutput, mod, false)
				{
					override fun generate()
					{
						this.block()
					}
				}
			}
		}

		fun blockModels(constructor: ADataProviderFactory<ABlockModelProvider>): ABlockModelProvider
		{
			return addProvider(isClient, constructor)
		}

		fun itemModels(block: AItemModelProvider.() -> Unit): AItemModelProvider
		{
			return itemModels { packOutput ->
				object : AItemModelProvider(packOutput, mod, false)
				{
					override fun generate()
					{
						this.block()
					}
				}
			}
		}

		fun itemModels(constructor: ADataProviderFactory<AItemModelProvider>): AItemModelProvider
		{
			return addProvider(isClient, constructor)
		}

		fun blockStates(block: ABlockStateProvider.() -> Unit): ABlockStateProvider
		{
			return blockStates { packOutput ->
				object : ABlockStateProvider(packOutput, mod, false)
				{
					override fun generate()
					{
						this.block()
					}
				}
			}
		}

		fun blockStates(constructor: ADataProviderFactory<ABlockStateProvider>): ABlockStateProvider
		{
			return addProvider(isClient, constructor)
		}
	}

	inner class Common
	{
		lateinit var blockTagsProvider: ATagsProvider.BlockTagsProvider

		fun blockTags(block: ATagsProvider.BlockTagsProvider.(registries: HolderLookup.Provider) -> Unit): ATagsProvider.BlockTagsProvider
		{
			return blockTags { packOutput, registries ->
				object : ATagsProvider.BlockTagsProvider(packOutput, mod, registries, false)
				{
					override fun generate(registries: HolderLookup.Provider)
					{
						this.block(registries)
					}
				}
			}
		}

		fun blockTags(constructor: ARegistryAwareDataProviderFactory<ATagsProvider.BlockTagsProvider>): ATagsProvider.BlockTagsProvider
		{
			return addProvider(isServer, constructor).also {
				blockTagsProvider = it
			}
		}

		fun itemTags(block: ATagsProvider.ItemTagsProvider.(registries: HolderLookup.Provider) -> Unit): ATagsProvider.ItemTagsProvider
		{
			return if (::blockTagsProvider.isInitialized)
				itemTags { packOutput, registries, blockTagsProvider ->
					object : ATagsProvider.ItemTagsProvider(packOutput, mod, registries, blockTagsProvider, false)
					{
						override fun generate(registries: HolderLookup.Provider)
						{
							this.block(registries)
						}
					}
				}
			else
				itemTags { packOutput, registries ->
					object : ATagsProvider.ItemTagsProvider(packOutput, mod, registries, false)
					{
						override fun generate(registries: HolderLookup.Provider)
						{
							this.block(registries)
						}
					}
				}
		}

		fun itemTags(constructor: ARegistryAwareDataProviderFactory<ATagsProvider.ItemTagsProvider>): ATagsProvider.ItemTagsProvider
		{
			return addProvider(isServer, constructor)
		}

		fun itemTags(constructor: ItemTagsDataProviderFactory): ATagsProvider.ItemTagsProvider
		{
			if (!::blockTagsProvider.isInitialized)
				throw IllegalStateException("You did not register a block tags provider. you must do that to use this overload")
			return addProvider(isServer) { packOutput, registries ->
				constructor(packOutput, registries, blockTagsProvider)
			}
		}

		fun biomeTags(block: ATagsProvider.BiomeTagsProvider.(registries: HolderLookup.Provider) -> Unit): ATagsProvider.BiomeTagsProvider
		{
			return biomeTags { packOutput, registries ->
				object : ATagsProvider.BiomeTagsProvider(packOutput, mod, registries, false)
				{
					override fun generate(registries: HolderLookup.Provider)
					{
						this.block(registries)
					}
				}
			}
		}

		fun biomeTags(constructor: ARegistryAwareDataProviderFactory<ATagsProvider.BiomeTagsProvider>): ATagsProvider.BiomeTagsProvider
		{
			return addProvider(isServer, constructor)
		}

		fun entityTags(block: ATagsProvider.EntityTypeTagsProvider.(registries: HolderLookup.Provider) -> Unit): ATagsProvider.EntityTypeTagsProvider
		{
			return entityTags { packOutput, registries ->
				object : ATagsProvider.EntityTypeTagsProvider(packOutput, mod, registries, false)
				{
					override fun generate(registries: HolderLookup.Provider)
					{
						this.block(registries)
					}
				}
			}
		}

		fun entityTags(constructor: ARegistryAwareDataProviderFactory<ATagsProvider.EntityTypeTagsProvider>): ATagsProvider.EntityTypeTagsProvider
		{
			return addProvider(isServer, constructor)
		}

		fun fluidTags(block: ATagsProvider.FluidTagsProvider.(registries: HolderLookup.Provider) -> Unit): ATagsProvider.FluidTagsProvider
		{
			return fluidTags { packOutput, registries ->
				object : ATagsProvider.FluidTagsProvider(packOutput, mod, registries, false)
				{
					override fun generate(registries: HolderLookup.Provider)
					{
						this.block(registries)
					}
				}
			}
		}

		fun fluidTags(constructor: ARegistryAwareDataProviderFactory<ATagsProvider.FluidTagsProvider>): ATagsProvider.FluidTagsProvider
		{
			return addProvider(isServer, constructor)
		}

		fun recipes(block: ARecipeProvider.(recipeOutput: RecipeOutput) -> Unit): ARecipeProvider
		{
			return addProvider(isServer) { packOutput, registries ->
				return@addProvider object : ARecipeProvider(packOutput, mod, registries, false)
				{
					override fun generate(recipeOutput: RecipeOutput)
					{
						this.block(recipeOutput)
					}
				}
			}
		}

		fun recipes(constructor: ARegistryAwareDataProviderFactory<ARecipeProvider>): ARecipeProvider
		{
			return addProvider(isServer, constructor)
		}
	}
}