package net.kernelpanicsoft.archie.config.v2.runtime

import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import java.util.concurrent.ConcurrentHashMap

/**
 * Runtime helpers for resolving registry ids and option lists for v2 registry-backed fields.
 */
object ConfigV2RegistryOptions {
    private val allRegistries: List<Registry<*>> by lazy {
        BuiltInRegistries.REGISTRY.toList()
    }

    private val registriesById: Map<ResourceLocation, Registry<*>> by lazy {
        allRegistries.associateBy { it.key().location() }
    }

    // Cache full option snapshots per registry id; per-call limits are applied on top.
    private val optionSnapshotByRegistryId = ConcurrentHashMap<ResourceLocation, List<String>>()
    private val inferredRegistryIdByValue = ConcurrentHashMap<ResourceLocation, String>()

    fun optionsFor(registryId: String?, limit: Int = 4096): List<String> {
        if (registryId.isNullOrBlank() || limit <= 0) {
            return emptyList()
        }
        val rl = runCatching { ResourceLocation.parse(registryId.trim()) }.getOrNull() ?: return emptyList()
        val registry = registriesById[rl] ?: return emptyList()
        val options = optionSnapshotByRegistryId.computeIfAbsent(rl) {
            registry.keySet().asSequence().map { key -> key.toString() }.toList()
        }
        return if (limit >= options.size) options else options.subList(0, limit)
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
