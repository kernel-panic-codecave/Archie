package com.withertech.archie.data.client

import com.google.gson.JsonObject
import com.withertech.archie.Archie
import com.withertech.archie.data.IArchieDataProvider
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

@Suppress("unused")
abstract class ArchieLanguageProvider(
	override val output: PackOutput,
	override val mod: Mod,
	override val exitOnError: Boolean,
	private val locale: String = "en_us"
) :
	IArchieDataProvider
{
	private val data: MutableMap<String, String> = TreeMap()

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

	fun addBlock(name: String, key: Supplier<out Block>)
	{
		add(key.get(), name)
	}

	fun add(key: Block, name: String)
	{
		add(key.descriptionId, name)
	}

	fun addItem(name: String, key: Supplier<out Item>)
	{
		add(key.get(), name)
	}

	fun add(key: Item, name: String)
	{
		add(key.descriptionId, name)
	}

	fun addItemStack(name: String, key: Supplier<ItemStack>)
	{
		add(key.get(), name)
	}

	fun add(key: ItemStack, name: String)
	{
		add(key.descriptionId, name)
	}

	fun addEnchantment(name: String, key: Supplier<out Enchantment>)
	{
		add(key.get(), name)
	}

	fun add(key: Enchantment, name: String)
	{
		add(key.descriptionId, name)
	}

	fun addEffect(name: String, key: Supplier<out MobEffect>)
	{
		add(key.get(), name)
	}

	fun add(key: MobEffect, name: String)
	{
		add(key.descriptionId, name)
	}

	fun addEntityType(name: String, key: Supplier<out EntityType<*>>)
	{
		add(key.get(), name)
	}

	fun add(key: EntityType<*>, name: String)
	{
		add(key.descriptionId, name)
	}

	fun add(key: String, value: String)
	{
		check(data.put(key, value) == null) { "Duplicate translation key $key" }
	}
}