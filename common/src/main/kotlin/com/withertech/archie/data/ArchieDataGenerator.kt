package com.withertech.archie.data

import com.withertech.archie.data.client.ArchieLanguageProvider
import com.withertech.archie.data.client.model.ArchieBlockModelProvider
import com.withertech.archie.data.client.model.ArchieBlockStateProvider
import com.withertech.archie.data.client.model.ArchieItemModelProvider
import com.withertech.archie.data.common.crafting.ArchieRecipeProvider
import com.withertech.archie.data.common.tags.ArchieTagsProvider
import com.withertech.archie.events.ArchieEvents
import dev.architectury.platform.Mod
import net.minecraft.core.HolderLookup
import net.minecraft.data.DataProvider
import net.minecraft.data.PackOutput
import net.minecraft.data.recipes.RecipeOutput
import java.util.concurrent.CompletableFuture

@Suppress("MemberVisibilityCanBePrivate", "unused")
abstract class ArchieDataGenerator
{
	val isClient: Boolean
		get() = System.getProperty("archie.datagen.client").toBoolean()
	val isServer: Boolean
		get() = System.getProperty("archie.datagen.server").toBoolean()

	abstract val mod: Mod

	abstract fun <T : DataProvider> addProvider(
		run: Boolean = true,
		factory: RegistryAwareArchieDataProviderFactory<T>
	): T

	fun <T : DataProvider> addProvider(run: Boolean = true, factory: ArchieDataProviderFactory<T>): T
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

	operator fun invoke(block: ArchieDataGenerator.() -> Unit) = apply(block)

	fun interface ArchieDataProviderFactory<T : DataProvider>
	{
		operator fun invoke(output: PackOutput): T
	}

	fun interface RegistryAwareArchieDataProviderFactory<T : DataProvider>
	{
		operator fun invoke(output: PackOutput, registries: CompletableFuture<HolderLookup.Provider>): T
	}

	fun interface ItemTagsDataProviderFactory
	{
		operator fun invoke(
			output: PackOutput,
			registries: CompletableFuture<HolderLookup.Provider>,
			blockTagsProvider: ArchieTagsProvider.BlockTagsProvider
		): ArchieTagsProvider.ItemTagsProvider
	}

	inner class Client
	{
		fun languages(locale: String = "en_us", block: ArchieLanguageProvider.() -> Unit): ArchieLanguageProvider
		{
			return languages { packOutput ->
				object : ArchieLanguageProvider(packOutput, mod, false, locale)
				{
					override fun generate()
					{
						this.block()
					}
				}
			}
		}

		fun languages(constructor: ArchieDataProviderFactory<ArchieLanguageProvider>): ArchieLanguageProvider
		{
			return addProvider(isClient, constructor)
		}

		fun blockModels(block: ArchieBlockModelProvider.() -> Unit): ArchieBlockModelProvider
		{
			return blockModels { packOutput ->
				object : ArchieBlockModelProvider(packOutput, mod, false)
				{
					override fun generate()
					{
						this.block()
					}
				}
			}
		}

		fun blockModels(constructor: ArchieDataProviderFactory<ArchieBlockModelProvider>): ArchieBlockModelProvider
		{
			return addProvider(isClient, constructor)
		}

		fun itemModels(block: ArchieItemModelProvider.() -> Unit): ArchieItemModelProvider
		{
			return itemModels { packOutput ->
				object : ArchieItemModelProvider(packOutput, mod, false)
				{
					override fun generate()
					{
						this.block()
					}
				}
			}
		}

		fun itemModels(constructor: ArchieDataProviderFactory<ArchieItemModelProvider>): ArchieItemModelProvider
		{
			return addProvider(isClient, constructor)
		}

		fun blockStates(block: ArchieBlockStateProvider.() -> Unit): ArchieBlockStateProvider
		{
			return blockStates { packOutput ->
				object : ArchieBlockStateProvider(packOutput, mod, false)
				{
					override fun generate()
					{
						this.block()
					}
				}
			}
		}

		fun blockStates(constructor: ArchieDataProviderFactory<ArchieBlockStateProvider>): ArchieBlockStateProvider
		{
			return addProvider(isClient, constructor)
		}
	}

	inner class Common
	{
		lateinit var blockTagsProvider: ArchieTagsProvider.BlockTagsProvider

		fun blockTags(block: ArchieTagsProvider.BlockTagsProvider.(registries: HolderLookup.Provider) -> Unit): ArchieTagsProvider.BlockTagsProvider
		{
			return blockTags { packOutput, registries ->
				object : ArchieTagsProvider.BlockTagsProvider(packOutput, mod, registries, false)
				{
					override fun generate(registries: HolderLookup.Provider)
					{
						this.block(registries)
					}
				}
			}
		}

		fun blockTags(constructor: RegistryAwareArchieDataProviderFactory<ArchieTagsProvider.BlockTagsProvider>): ArchieTagsProvider.BlockTagsProvider
		{
			return addProvider(isServer, constructor).also {
				blockTagsProvider = it
			}
		}

		fun itemTags(block: ArchieTagsProvider.ItemTagsProvider.(registries: HolderLookup.Provider) -> Unit): ArchieTagsProvider.ItemTagsProvider
		{
			return if (::blockTagsProvider.isInitialized)
				itemTags { packOutput, registries, blockTagsProvider ->
					object : ArchieTagsProvider.ItemTagsProvider(packOutput, mod, registries, blockTagsProvider, false)
					{
						override fun generate(registries: HolderLookup.Provider)
						{
							this.block(registries)
						}
					}
				}
			else
				itemTags { packOutput, registries ->
					object : ArchieTagsProvider.ItemTagsProvider(packOutput, mod, registries, false)
					{
						override fun generate(registries: HolderLookup.Provider)
						{
							this.block(registries)
						}
					}
				}
		}

		fun itemTags(constructor: RegistryAwareArchieDataProviderFactory<ArchieTagsProvider.ItemTagsProvider>): ArchieTagsProvider.ItemTagsProvider
		{
			return addProvider(isServer, constructor)
		}

		fun itemTags(constructor: ItemTagsDataProviderFactory): ArchieTagsProvider.ItemTagsProvider
		{
			if (!::blockTagsProvider.isInitialized)
				throw IllegalStateException("You did not register a block tags provider. you must do that to use this overload")
			return addProvider(isServer) { packOutput, registries ->
				constructor(packOutput, registries, blockTagsProvider)
			}
		}

		fun biomeTags(block: ArchieTagsProvider.BiomeTagsProvider.(registries: HolderLookup.Provider) -> Unit): ArchieTagsProvider.BiomeTagsProvider
		{
			return biomeTags { packOutput, registries ->
				object : ArchieTagsProvider.BiomeTagsProvider(packOutput, mod, registries, false)
				{
					override fun generate(registries: HolderLookup.Provider)
					{
						this.block(registries)
					}
				}
			}
		}

		fun biomeTags(constructor: RegistryAwareArchieDataProviderFactory<ArchieTagsProvider.BiomeTagsProvider>): ArchieTagsProvider.BiomeTagsProvider
		{
			return addProvider(isServer, constructor)
		}

		fun entityTags(block: ArchieTagsProvider.EntityTypeTagsProvider.(registries: HolderLookup.Provider) -> Unit): ArchieTagsProvider.EntityTypeTagsProvider
		{
			return entityTags { packOutput, registries ->
				object : ArchieTagsProvider.EntityTypeTagsProvider(packOutput, mod, registries, false)
				{
					override fun generate(registries: HolderLookup.Provider)
					{
						this.block(registries)
					}
				}
			}
		}

		fun entityTags(constructor: RegistryAwareArchieDataProviderFactory<ArchieTagsProvider.EntityTypeTagsProvider>): ArchieTagsProvider.EntityTypeTagsProvider
		{
			return addProvider(isServer, constructor)
		}

		fun fluidTags(block: ArchieTagsProvider.FluidTagsProvider.(registries: HolderLookup.Provider) -> Unit): ArchieTagsProvider.FluidTagsProvider
		{
			return fluidTags { packOutput, registries ->
				object : ArchieTagsProvider.FluidTagsProvider(packOutput, mod, registries, false)
				{
					override fun generate(registries: HolderLookup.Provider)
					{
						this.block(registries)
					}
				}
			}
		}

		fun fluidTags(constructor: RegistryAwareArchieDataProviderFactory<ArchieTagsProvider.FluidTagsProvider>): ArchieTagsProvider.FluidTagsProvider
		{
			return addProvider(isServer, constructor)
		}

		fun recipes(block: ArchieRecipeProvider.(recipeOutput: RecipeOutput) -> Unit): ArchieRecipeProvider
		{
			return addProvider(isServer) { packOutput, registries ->
				return@addProvider object : ArchieRecipeProvider(packOutput, mod, registries, false)
				{
					override fun generate(recipeOutput: RecipeOutput)
					{
						this.block(recipeOutput)
					}
				}
			}
		}

		fun recipes(constructor: RegistryAwareArchieDataProviderFactory<ArchieRecipeProvider>): ArchieRecipeProvider
		{
			return addProvider(isServer, constructor)
		}
	}
}