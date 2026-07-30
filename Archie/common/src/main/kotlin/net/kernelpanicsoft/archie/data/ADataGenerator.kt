package net.kernelpanicsoft.archie.data

import net.kernelpanicsoft.archie.data.client.ALanguageProvider
import net.kernelpanicsoft.archie.data.client.model.ABlockModelProvider
import net.kernelpanicsoft.archie.data.client.model.ABlockStateProvider
import net.kernelpanicsoft.archie.data.client.model.AItemModelProvider
import net.kernelpanicsoft.archie.data.common.crafting.ARecipeProvider
import net.kernelpanicsoft.archie.data.common.tags.ATagsProvider
import dev.architectury.platform.Mod
import net.minecraft.core.HolderLookup
import net.minecraft.data.DataProvider
import net.minecraft.data.PackOutput
import net.minecraft.data.recipes.RecipeOutput
import java.util.concurrent.CompletableFuture

/**
 * Base class for a platform's datagen entrypoint, providing a small DSL for registering
 * [DataProvider]s without needing to interact with architectury's `DataGeneratorPlugin`
 * directly.
 *
 * Providers are grouped by [client] and [common] since client-only providers (e.g. models,
 * languages) must be skipped on a dedicated server datagen run and vice versa; [isClient] and
 * [isServer] gate whether a provider actually runs based on the `archie.datagen.client`/
 * `archie.datagen.server` system properties set by the datagen run configuration.
 *
 * Loader modules implement [addProvider] on top of their platform's data generator and then
 * invoke this generator, typically as `ArchieDatagen(mod) { client { ... }; common { ... } }`.
 */
@Suppress("MemberVisibilityCanBePrivate", "unused")
abstract class ADataGenerator
{
	/** Whether client-only providers should run, from the `archie.datagen.client` system property. */
	val isClient: Boolean
		get() = System.getProperty("archie.datagen.client").toBoolean()

	/** Whether server-only providers should run, from the `archie.datagen.server` system property. */
	val isServer: Boolean
		get() = System.getProperty("archie.datagen.server").toBoolean()

	abstract val mod: Mod

	/**
	 * Registers [factory] with the underlying platform data generator, running it only when
	 * [run] is `true`, and returns the constructed provider so it can be reused (e.g. an item
	 * tags provider depending on a previously created block tags provider).
	 */
	abstract fun <T : DataProvider> addProvider(
		run: Boolean = true,
		factory: ARegistryAwareDataProviderFactory<T>
	): T

	/** [addProvider] overload for providers that don't need access to [HolderLookup.Provider]. */
	fun <T : DataProvider> addProvider(run: Boolean = true, factory: ADataProviderFactory<T>): T
	{
		return addProvider(run) { output, _ ->
			factory(output)
		}
	}

	/** Registers client-only providers (models, languages) declared in [block] via [Client]. */
	fun client(block: Client.() -> Unit)
	{
		Client().apply(block)
	}

	/** Registers server-only providers (tags, recipes) declared in [block] via [Common]. */
	fun common(block: Common.() -> Unit)
	{
		Common().apply(block)
	}

	operator fun invoke(block: ADataGenerator.() -> Unit) = apply(block)

	/** Factory for a [DataProvider] that only needs a [PackOutput] to be constructed. */
	fun interface ADataProviderFactory<T : DataProvider>
	{
		operator fun invoke(output: PackOutput): T
	}

	/** Factory for a [DataProvider] that also needs the registry [HolderLookup.Provider] future. */
	fun interface ARegistryAwareDataProviderFactory<T : DataProvider>
	{
		operator fun invoke(output: PackOutput, registries: CompletableFuture<HolderLookup.Provider>): T
	}

	/** Factory for an [ATagsProvider.ItemTagsProvider] that depends on an existing block tags provider. */
	fun interface ItemTagsDataProviderFactory
	{
		operator fun invoke(
			output: PackOutput,
			registries: CompletableFuture<HolderLookup.Provider>,
			blockTagsProvider: ATagsProvider.BlockTagsProvider
		): ATagsProvider.ItemTagsProvider
	}

	/** DSL scope for registering client-side providers; see [ADataGenerator.client]. */
	inner class Client
	{
		/** Registers an [ALanguageProvider] for [locale] that generates translations in [block]. */
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

		/** Registers an [ABlockModelProvider] that generates block models in [block]. */
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

		/** Registers an [AItemModelProvider] that generates item models in [block]. */
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

		/** Registers an [ABlockStateProvider] that generates blockstate JSONs in [block]. */
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

	/** DSL scope for registering server-side providers; see [ADataGenerator.common]. */
	inner class Common
	{
		/** The block tags provider registered via [blockTags], if any; used by [itemTags] to derive item tags from block tags. */
		lateinit var blockTagsProvider: ATagsProvider.BlockTagsProvider

		/** Registers an [ATagsProvider.BlockTagsProvider] that declares block tags in [block]. */
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

		/**
		 * Registers an [ATagsProvider.ItemTagsProvider] that declares item tags in [block].
		 * Constructs it with [blockTagsProvider] when a block tags provider was already
		 * registered via [blockTags], enabling `copy(blockTag, itemTag)`.
		 */
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

		/** Registers an [ATagsProvider.BiomeTagsProvider] that declares biome tags in [block]. */
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

		/** Registers an [ATagsProvider.EntityTypeTagsProvider] that declares entity type tags in [block]. */
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

		/** Registers an [ATagsProvider.FluidTagsProvider] that declares fluid tags in [block]. */
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

		/** Registers an [ARecipeProvider] that declares recipes via [block]. */
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