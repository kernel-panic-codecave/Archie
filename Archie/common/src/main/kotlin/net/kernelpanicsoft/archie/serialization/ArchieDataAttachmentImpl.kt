package net.kernelpanicsoft.archie.serialization

import earth.terrarium.common_storage_lib.data.DataManager

/**
 * Thin wrapper around a single Common Storage Lib [DataManager], backing [AttachmentRegistry.attachment].
 * See [ArchieDataAttachment] for the full contract this implements.
 */
internal class ArchieDataAttachmentImpl<T>(private val manager: DataManager<T>) : ArchieDataAttachment<T>
{
	override fun get(holder: Any): T = manager.get(holder)
	override fun getOrThrow(holder: Any): T = manager.getOrThrow(holder)
	override fun getOrCreate(holder: Any, default: T): T = manager.getOrCreate(holder, default)

	override fun set(holder: Any, value: T): T
	{
		manager.set(holder, value)
		return value
	}

	override fun remove(holder: Any): T = manager.remove(holder)
	override fun has(holder: Any): Boolean = manager.has(holder)
	override fun modify(holder: Any, block: (T) -> T): T = manager.modify(holder, block)
}
