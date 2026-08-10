package net.kernelpanicsoft.archie.data

import dev.architectury.platform.Mod
import net.minecraft.core.HolderLookup
import net.minecraft.data.DataProvider
import net.minecraft.data.PackOutput
import java.util.concurrent.CompletableFuture

/**
 * Base class for a platform's datagen entrypoint. Trimmed to the minimal surface `archie-core`
 * needs (just enough for [net.kernelpanicsoft.archie.events.AEvents]'s `GatherDataHandler` to
 * reference it as a type) - the full `client { }`/`common { }` provider DSL (models, languages,
 * tags, recipes) lives in `archie-datagen` as extension functions/classes on this type, since it
 * pulls in the whole datagen provider graph. See `archie-datagen`'s `ADataGenerator` extensions.
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
}
