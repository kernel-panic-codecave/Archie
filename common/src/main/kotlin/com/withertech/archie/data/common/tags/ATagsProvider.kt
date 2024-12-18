package com.withertech.archie.data.common.tags

import com.withertech.archie.Archie
import com.withertech.archie.data.IADataProvider
import com.withertech.archie.registries.holder
import dev.architectury.platform.Mod
import net.minecraft.core.Holder
import net.minecraft.core.HolderLookup
import net.minecraft.core.Registry
import net.minecraft.core.RegistryAccess
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.data.PackOutput
import net.minecraft.data.tags.TagsProvider
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.*
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.Item
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.material.Fluid
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer
import java.util.function.Function
import kotlin.jvm.optionals.getOrNull
import kotlin.system.exitProcess

abstract class ATagsProvider<T : Any>(
	override val output: PackOutput, override val mod: Mod, registryKey: ResourceKey<out Registry<T>>,
	registries: CompletableFuture<HolderLookup.Provider>, override val exitOnError: Boolean
) : TagsProvider<T>(output, registryKey, registries), IADataProvider
{
	final override fun addTags(registries: HolderLookup.Provider)
	{
		runCatching {
			generate(registries)
		}.onFailure {
			Archie.LOGGER.error(
				"Data Provider $name failed with exception: ${it.message}\n" +
						"Stacktrace: ${it.stackTraceToString()}"
			)
			if (exitOnError) exitProcess(-1)
		}
	}

	/**
	 * Implement this method and then use [invoke] to get and register new tag builders.
	 */
	abstract fun generate(registries: HolderLookup.Provider)

	/**
	 * Looks up a registry entry for a specific [ResourceKey].
	 * Only works if the resource key corresponds to the provider's [registryKey]
	 * @param registries The [HolderLookup.Provider] received from [addTags]
	 * @param key The [ResourceKey] to look up
	 * @return The looked up registry entry
	 * @throws IllegalStateException If either the registry or the entry cannot be looked up
	 */
	protected open fun lookup(registries: HolderLookup.Provider, key: ResourceKey<T>): T
	{
		val registryLookup = registries.lookupOrThrow(registryKey)
		return registryLookup.getOrThrow(key).value()
	}

	/**
	 * Looks up a registry entry for a specific [ResourceLocation].
	 * Uses the provider's [registryKey] to create a [ResourceKey] and delegates
	 * to the overload that takes a [ResourceKey]
	 * @param registries The [HolderLookup.Provider] received from [addTags]
	 * @param key The [ResourceKey] to look up
	 * @return The looked up registry entry
	 * @throws IllegalStateException If either the registry or the entry cannot be looked up
	 */
	protected open fun lookup(registries: HolderLookup.Provider, key: ResourceLocation): T
	{
		return lookup(registries, ResourceKey.create(registryKey, key))
	}

	/**
	 * Override to enable adding objects to the tag builder directly.
	 */
	open fun reverseLookup(element: T): ResourceKey<T>
	{
		val registry =
			RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY).registry(registryKey).getOrNull()

		if (registry != null)
		{
			val key: Optional<ResourceKey<T>> = registry.getResourceKey(element)

			if (key.isPresent)
			{
				return key.get()
			}
		}

		throw UnsupportedOperationException("Adding objects is not supported by $javaClass")
	}

	/**
	 * Creates a new instance of [IArchieTagBuilder] for the given [TagKey].
	 *
	 * @receiver The [TagKey] tag to create the builder for
	 * @return The [IArchieTagBuilder] instance
	 */
	operator fun TagKey<T>.invoke(): IArchieTagBuilder<T>
	{
		return ATagBuilderPlatform.createTagBuilder(super.tag(this), this@ATagsProvider)
	}

	/**
	 * Creates a new instance of [IArchieTagBuilder] for the given [TagKey]
	 * and applies a lambda over it.
	 *
	 * @receiver The [TagKey] tag to create the builder for
	 * @param block the lamda to apply over the [TagKey]
	 */
	operator fun TagKey<T>.invoke(block: IArchieTagBuilder<T>.() -> Unit)
	{
		this().apply(block)
	}

	/**
	 * Creates a new instance of [IArchieTagBuilder] for the given [TagKey]
	 * and adds an element of type [T] to it.
	 *
	 * @receiver The [TagKey] tag to create the builder for
	 * @param tag the element to add to the [TagKey]
	 */
	operator fun TagKey<T>.plusAssign(tag: T)
	{
		this().add(tag)
	}

	/**
	 * Creates a new instance of [IArchieTagBuilder] for the given [TagKey]
	 * and adds an element of type [ResourceKey] to it.
	 *
	 * @receiver The [TagKey] tag to create the builder for
	 * @param tag the element to add to the [TagKey]
	 */
	operator fun TagKey<T>.plusAssign(tag: ResourceKey<T>)
	{
		this().add(tag)
	}

	/**
	 * Creates a new instance of [IArchieTagBuilder] for the given [TagKey]
	 * and adds an element of type [ResourceLocation] to it.
	 *
	 * @receiver The [TagKey] tag to create the builder for
	 * @param tag the element to add to the [TagKey]
	 */
	operator fun TagKey<T>.plusAssign(tag: ResourceLocation)
	{
		this().add(tag)
	}

	/**
	 * Creates a new instance of [IArchieTagBuilder] for the given [TagKey]
	 * and adds a tag of type [TagKey] to it.
	 *
	 * @receiver The [TagKey] tag to create the builder for
	 * @param tag the element to add to the [TagKey]
	 */
	operator fun TagKey<T>.plusAssign(tag: TagKey<T>)
	{
		this().addTag(tag)
	}

	/**
	 * Creates a new instance of [IArchieTagBuilder] for the given [TagKey]
	 * and adds a list of elements of type [T] to it.
	 *
	 * @receiver The [TagKey] tag to create the builder for
	 * @param tags the list of elements to add to the [TagKey]
	 */
	operator fun TagKey<T>.plusAssign(tags: List<T>)
	{
		this().apply { tags.forEach(this::add) }
	}

	/**
	 * Creates a new instance of [IArchieTagBuilder] for the given [TagKey]
	 * and adds a list of elements of type [ResourceKey] to it.
	 *
	 * @receiver The [TagKey] tag to create the builder for
	 * @param tags the list of elements to add to the [TagKey]
	 */
	@JvmName("plusAssignKeyList")
	operator fun TagKey<T>.plusAssign(tags: List<ResourceKey<T>>)
	{
		this().apply { tags.forEach(this::add) }
	}

	/**
	 * Creates a new instance of [IArchieTagBuilder] for the given [TagKey]
	 * and adds a list of elements of type [ResourceLocation] to it.
	 *
	 * @receiver The [TagKey] tag to create the builder for
	 * @param tags the list of elements to add to the [TagKey]
	 */
	@JvmName("plusAssignLocList")
	operator fun TagKey<T>.plusAssign(tags: List<ResourceLocation>)
	{
		this().apply { tags.forEach(this::add) }
	}

	/**
	 * Creates a new instance of [IArchieTagBuilder] for the given [TagKey]
	 * and adds a list of tags of type [TagKey] to it.
	 *
	 * @receiver The [TagKey] tag to create the builder for
	 * @param tags the list of elements to add to the [TagKey]
	 */
	@JvmName("plusAssignTagList")
	operator fun TagKey<T>.plusAssign(tags: List<TagKey<T>>)
	{
		this().apply { tags.forEach(this::addTag) }
	}

	operator fun TagKey<T>.timesAssign(tag: ResourceKey<T>)
	{
		this().addOptional(tag)
	}

	operator fun TagKey<T>.timesAssign(tag: ResourceLocation)
	{
		this().addOptional(tag)
	}

	operator fun TagKey<T>.timesAssign(tag: TagKey<T>)
	{
		this().addOptionalTag(tag)
	}

	@JvmName("timesAssignKeyList")
	operator fun TagKey<T>.timesAssign(tags: List<ResourceKey<T>>)
	{
		this().apply { tags.forEach(this::addOptional) }
	}

	@JvmName("timesAssignLocList")
	operator fun TagKey<T>.timesAssign(tags: List<ResourceLocation>)
	{
		this().apply { tags.forEach(this::addOptional) }
	}

	@JvmName("timesAssignTagList")
	operator fun TagKey<T>.timesAssign(tags: List<TagKey<T>>)
	{
		this().apply { tags.forEach(this::addOptionalTag) }

	}

	@Deprecated("Don't use this, use the platform agnostic version", ReplaceWith("tag()"))
	override fun tag(tag: TagKey<T>): TagAppender<T>
	{
		throw IllegalStateException("Usage of vanilla \"tag\" method in an ArchieTagsProvider is prohibited. use the platform agnostic \"invoke\" operator instead")
	}

	override fun getName(): String = format("${
		registryKey.location().path.split("/").last().split("_")
			.joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
	} Tags")

	/**
	 * Extend this class to create [Block] tags in the "/blocks" tag directory.
	 */
	abstract class BlockTagsProvider(
		output: PackOutput,
		mod: Mod,
		registriesFuture: CompletableFuture<HolderLookup.Provider>,
		exitOnError: Boolean
	) :
		ATagsProvider<Block>(output, mod, Registries.BLOCK, registriesFuture, exitOnError)
	{
		override fun reverseLookup(element: Block): ResourceKey<Block>
		{
			return (element.holder as Holder.Reference<Block>).key()
		}
	}

	/**
	 * Extend this class to create [Item] tags in the "/items" tag directory.
	 */
	abstract class ItemTagsProvider :
		ATagsProvider<Item>
	{
		/**
		 * Construct an [ItemTagsProvider] tag provider **with** an associated [BlockTagsProvider] tag provider.
		 *
		 * @param output The [PackOutput] instance
		 * @param mod The architectury [Mod] instance
		 * @param registriesFuture The [HolderLookup.Provider] future
		 * @param blockTagsProvider The parent [BlockTagsProvider]
		 */
		constructor(
			output: PackOutput,
			mod: Mod,
			registriesFuture: CompletableFuture<HolderLookup.Provider>,
			blockTagsProvider: BlockTagsProvider?,
			exitOnError: Boolean

		) : super(output, mod, Registries.ITEM, registriesFuture, exitOnError)
		{
			this.blockTagBuilderProvider =
				if (blockTagsProvider == null) null else Function<TagKey<Block>, TagBuilder> { tag: TagKey<Block> ->
					blockTagsProvider.getOrCreateRawBuilder(
						tag
					)
				}
		}

		/**
		 * Construct an [ItemTagsProvider] tag provider **without** an associated [BlockTagsProvider] tag provider.
		 *
		 * @param output The [PackOutput] instance
		 * @param mod The architectury [Mod] instance
		 * @param registriesFuture The [HolderLookup.Provider] future
		 */
		constructor(
			output: PackOutput,
			mod: Mod,
			registriesFuture: CompletableFuture<HolderLookup.Provider>,
			exitOnError: Boolean

		) : this(output, mod, registriesFuture, null, exitOnError)


		private val blockTagBuilderProvider: Function<TagKey<Block>, TagBuilder>?

		/**
		 * Copy the entries from a tag with the [Block] type into this item tag.
		 *
		 *
		 * The [ItemTagsProvider] tag provider must be constructed with an associated [BlockTagsProvider] tag provider to use this method.
		 *
		 * @param blockTag The block tag to copy from.
		 * @param itemTag  The item tag to copy to.
		 */
		fun copy(blockTag: TagKey<Block>, itemTag: TagKey<Item>)
		{
			val blockTagBuilder = Objects.requireNonNull(
				this.blockTagBuilderProvider,
				"Pass Block tag provider via constructor to use copy"
			)!!.apply(blockTag)
			val itemTagBuilder: TagBuilder = this.getOrCreateRawBuilder(itemTag)
			blockTagBuilder.build().forEach(Consumer { entry: TagEntry ->
				itemTagBuilder.add(
					entry
				)
			})
		}

		override fun reverseLookup(element: Item): ResourceKey<Item>
		{
			return (element.holder as Holder.Reference<Item>).key()
		}
	}

	/**
	 * Extend this class to create [Fluid] tags in the "/fluids" tag directory.
	 */
	abstract class FluidTagsProvider(
		output: PackOutput,
		mod: Mod,
		registriesFuture: CompletableFuture<HolderLookup.Provider>,
		exitOnError: Boolean
	) :
		ATagsProvider<Fluid>(output, mod, Registries.FLUID, registriesFuture, exitOnError)
	{
		override fun reverseLookup(element: Fluid): ResourceKey<Fluid>
		{
			return (element.holder as Holder.Reference<Fluid>).key()
		}
	}

	/**
	 * Extend this class to create [EntityType] tags in the "/entity_types" tag directory.
	 */
	abstract class EntityTypeTagsProvider(
		output: PackOutput,
		mod: Mod,
		registriesFuture: CompletableFuture<HolderLookup.Provider>,
		exitOnError: Boolean
	) :
		ATagsProvider<EntityType<*>>(output, mod, Registries.ENTITY_TYPE, registriesFuture, exitOnError)
	{
		override fun reverseLookup(element: EntityType<*>): ResourceKey<EntityType<*>>
		{
			return (element.holder as Holder.Reference<EntityType<*>>).key()
		}
	}

	/**
	 * Extend this class to create [Biome] tags in the "/worldgen/biome" tag directory.
	 *
	 * **Note:** Minecraft does not have a biome registry, ao only [ResourceKey] and [TagKey] are allowed as tag entries
	 */
	abstract class BiomeTagsProvider(
		output: PackOutput,
		mod: Mod,
		registriesFuture: CompletableFuture<HolderLookup.Provider>, exitOnError: Boolean
	) :
		ATagsProvider<Biome>(output, mod, Registries.BIOME, registriesFuture, exitOnError)
	{
		override fun reverseLookup(element: Biome): ResourceKey<Biome>
		{
			throw UnsupportedOperationException("You can't look up a biome in the registry since Minecraft doesn't have a biome registry")
		}
	}

}