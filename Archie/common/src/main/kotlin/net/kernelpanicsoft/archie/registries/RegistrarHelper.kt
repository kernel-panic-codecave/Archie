package net.kernelpanicsoft.archie.registries

import dev.architectury.registry.registries.Registrar
import dev.architectury.registry.registries.RegistrarBuilder
import dev.architectury.registry.registries.RegistrarManager
import net.kernelpanicsoft.archie.util.rem

/**
 * A base for declaring **custom** Architectury registries (i.e. a whole new registry, the way
 * [net.minecraft.core.registries.Registries.ITEM] is a registry) via lazily-built [Registrar]s.
 *
 * This is unrelated to registering *entries* into an existing registry - for that, use
 * [RegistryHelper]/[ADeferredRegistryHolder] with a [dev.architectury.registry.registries.DeferredRegister].
 * Once a custom registry declared here exists, populating it with entries still requires its
 * own [dev.architectury.registry.registries.DeferredRegister] targeting the [Registrar]'s
 * registry key, created separately from this helper.
 *
 * @param modId The mod id passed to [RegistrarManager.get] and used as the namespace for each [registry].
 */
abstract class RegistrarHelper(private val modId: String)
{
	private val manager = RegistrarManager.get(modId)
	private val lazies = mutableListOf<Lazy<Registrar<*>>>()

	/**
	 * Declares a lazily-built custom [Registrar] (registry) for [id], configured via [block].
	 *
	 * @param id The unqualified registry id, namespaced under [modId].
	 */
	fun <T : Any> registry(id: String, block: RegistrarBuilder<T>.() -> Unit = {}): Lazy<Registrar<T>>
	{
		return lazy {
			manager.builder<T>(modId % id).apply(block).build()
		}.also { lazies += it }
	}

	/** Forces every custom registry declared via [registry] to be built. */
	fun init()
	{
		lazies.forEach { it.value }
	}
}