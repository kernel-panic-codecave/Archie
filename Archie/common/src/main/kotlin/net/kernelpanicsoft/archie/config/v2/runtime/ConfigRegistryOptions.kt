package net.kernelpanicsoft.archie.config.v2.runtime

import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import java.util.concurrent.ConcurrentHashMap

/**
 * Runtime helpers for resolving registry ids and option lists for v2 registry-backed fields.
 */
object ConfigRegistryOptions {
    private val legacyAliases: Map<String, ResourceKey<out Registry<*>>> = mapOf(
        "minecraft:core/item" to Registries.ITEM,
        "minecraft:core/block" to Registries.BLOCK,
        "minecraft:core/fluid" to Registries.FLUID,
        "minecraft:core/entity_type" to Registries.ENTITY_TYPE,
        "minecraft:core/block_entity_type" to Registries.BLOCK_ENTITY_TYPE,
        "minecraft:core/block_item" to Registries.ITEM,
    )

    private val allRegistries: List<Registry<*>> by lazy {
        BuiltInRegistries.REGISTRY.toList()
    }

    private val registriesById: Map<ResourceLocation, Registry<*>> by lazy {
        allRegistries.associateBy { it.key().location() }
    }

    // Cache full option snapshots per registry id; per-call limits are applied on top.
    private val optionSnapshotByRegistryId = ConcurrentHashMap<ResourceLocation, List<String>>()
    private val inferredRegistryIdByValue = ConcurrentHashMap<ResourceLocation, String>()

    private fun normalizeRegistryId(registryId: String?): String? {
        if (registryId.isNullOrBlank()) return null
        val trimmed = registryId.trim()
        return legacyAliases[trimmed]?.location()?.toString() ?: trimmed
    }

    private fun optionsForResolvedRegistry(resolvedRegistryId: String?, limit: Int): List<String> {
        if (resolvedRegistryId.isNullOrBlank() || limit <= 0) {
            return emptyList()
        }
        val rl = runCatching { ResourceLocation.parse(resolvedRegistryId) }.getOrNull() ?: return emptyList()
        val registry = registriesById[rl] ?: return emptyList()
        val options = optionSnapshotByRegistryId.computeIfAbsent(rl) {
            registry.keySet().asSequence().map { key -> key.toString() }.toList()
        }
        return if (limit >= options.size) options else options.subList(0, limit)
    }

    fun optionsFor(registryId: String?, limit: Int = 4096): List<String> {
        return optionsForResolvedRegistry(normalizeRegistryId(registryId), limit)
    }

    fun optionsFor(registryId: String?, candidates: Iterable<String>, limit: Int = 4096): List<String> {
        optionsFor(registryId, limit).takeIf { it.isNotEmpty() }?.let { return it }

        for (candidate in candidates) {
            val inferredRegistryId = inferRegistryId(candidate) ?: continue
            optionsForResolvedRegistry(inferredRegistryId, limit).takeIf { it.isNotEmpty() }?.let { return it }
        }

        return emptyList()
    }

    fun inferRegistryId(value: String?): String? {
        if (value.isNullOrBlank()) {
            return null
        }
        val rl = runCatching { ResourceLocation.parse(value.trim()) }.getOrNull() ?: return null
        return inferRegistryId(rl)
    }

    fun inferRegistryId(value: ResourceLocation?): String? {
        if (value == null) {
            return null
        }
        inferredRegistryIdByValue[value]?.let { return it }

        val registryId = allRegistries.firstOrNull { it.containsKey(value) }
            ?.key()
            ?.location()
            ?.toString()
            ?: return null

        inferredRegistryIdByValue[value] = registryId
        return registryId
    }
}
