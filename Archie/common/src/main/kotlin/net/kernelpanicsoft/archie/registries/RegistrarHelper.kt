package net.kernelpanicsoft.archie.registries

import dev.architectury.registry.registries.Registrar
import dev.architectury.registry.registries.RegistrarBuilder
import dev.architectury.registry.registries.RegistrarManager
import net.kernelpanicsoft.archie.util.rem

abstract class RegistrarHelper(private val modId: String)
{
	private val manager = RegistrarManager.get(modId)
	private val lazies = mutableListOf<Lazy<Registrar<*>>>()

	fun <T : Any> registry(id: String, block: RegistrarBuilder<T>.() -> Unit = {}): Lazy<Registrar<T>>
	{
		return lazy {
			manager.builder<T>(modId % id).apply(block).build()
		}
	}

	fun init()
	{
		lazies.forEach { it.value }
	}
}