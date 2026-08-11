package net.kernelpanicsoft.archie.data.client

import com.google.gson.JsonObject
import net.kernelpanicsoft.archie.Archie
import net.kernelpanicsoft.archie.data.IADataProvider
import dev.architectury.platform.Mod
import net.minecraft.data.CachedOutput
import net.minecraft.data.DataProvider
import net.minecraft.data.PackOutput
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.Enchantment
import net.minecraft.world.level.block.Block
import java.nio.file.Path
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.function.Supplier
import kotlin.system.exitProcess

/**
 * Datagen provider that builds a `assets/<mod_id>/lang/<locale>.json` translation file.
 * Implement [generate] and call the `add*` helpers to register translation keys; use via
 * [net.kernelpanicsoft.archie.data.ADataGenerator.Client.languages].
 */
@Suppress("unused")
abstract class ALanguageProvider(
	override val output: PackOutput,
	override val mod: Mod,
	override val exitOnError: Boolean,
	private val locale: String = "en_us"
) :
	IADataProvider
{
	private val data: MutableMap<String, String> = TreeMap()

	/** Called once during [run] to register translations via the `add*` helpers. */
	protected abstract fun generate()

	override fun run(cache: CachedOutput): CompletableFuture<*>
	{
		runCatching {
			generate()
		}.onFailure {
			Archie.LOGGER.error(
				"Data Provider $name failed with exception: ${it.message}\n" +
						"Stacktrace: ${it.stackTraceToString()}"
			)
			if (exitOnError) exitProcess(-1)
		}
		if (data.isNotEmpty()) return save(
			cache,
			output.getOutputFolder(PackOutput.Target.RESOURCE_PACK).resolve(this.mod.modId).resolve("lang").resolve(
				this.locale + ".json"
			)
		)


		return CompletableFuture.allOf()
	}

	override fun getName(): String = format("Languages - $locale")

	private fun save(cache: CachedOutput, target: Path): CompletableFuture<*>
	{
		// TODO: DataProvider.saveStable handles the caching and hashing already, but creating the JSON Object this way seems unreliable. -C
		val json = JsonObject()
		data.forEach { (property: String?, value: String?) ->
			json.addProperty(
				property,
				value
			)
		}

		return DataProvider.saveStable(cache, json, target)
	}

	/** Translates a deferred [Block] to [name]; see [add]. */
	fun addBlock(name: String, key: Supplier<out Block>)
	{
		add(key.get(), name)
	}

	/** Translates [key]'s `descriptionId` to [name]; see [add]. */
	fun add(key: Block, name: String)
	{
		add(key.descriptionId, name)
	}

	/** Translates a deferred [Item] to [name]; see [add]. */
	fun addItem(name: String, key: Supplier<out Item>)
	{
		add(key.get(), name)
	}

	/** Translates [key]'s `descriptionId` to [name]; see [add]. */
	fun add(key: Item, name: String)
	{
		add(key.descriptionId, name)
	}

	/** Translates a deferred [ItemStack] to [name]; see [add]. */
	fun addItemStack(name: String, key: Supplier<ItemStack>)
	{
		add(key.get(), name)
	}

	/** Translates [key]'s `descriptionId` to [name]; see [add]. */
	fun add(key: ItemStack, name: String)
	{
		add(key.descriptionId, name)
	}

//	fun addEnchantment(name: String, key: Supplier<out Enchantment>)
//	{
//		add(key.get(), name)
//	}
//
//	fun add(key: Enchantment, name: String)
//	{
//		add(key.descriptionId, name)
//	}

	/** Translates a deferred [MobEffect] to [name]; see [add]. */
	fun addEffect(name: String, key: Supplier<out MobEffect>)
	{
		add(key.get(), name)
	}

	/** Translates [key]'s `descriptionId` to [name]; see [add]. */
	fun add(key: MobEffect, name: String)
	{
		add(key.descriptionId, name)
	}

	/** Translates a deferred [EntityType] to [name]; see [add]. */
	fun addEntityType(name: String, key: Supplier<out EntityType<*>>)
	{
		add(key.get(), name)
	}

	/** Translates [key]'s `descriptionId` to [name]; see [add]. */
	fun add(key: EntityType<*>, name: String)
	{
		add(key.descriptionId, name)
	}

	/** Registers a raw translation [key] to [value]. Throws if [key] is already registered. */
	fun add(key: String, value: String)
	{
		check(data.put(key, value) == null) { "Duplicate translation key $key" }
	}
}