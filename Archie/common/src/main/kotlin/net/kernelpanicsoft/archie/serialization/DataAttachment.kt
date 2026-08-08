package net.kernelpanicsoft.archie.serialization

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * A [DataManager][earth.terrarium.common_storage_lib.data.DataManager]-backed attachment. Unlike
 * [NBTHolder], which owns its own per-instance field storage, a single [ArchieDataAttachment]
 * instance is stateless and reusable as the delegate for a `var Holder.property by ...`
 * extension property on *any* number of holder instances - the holder passed to each method
 * (or, via [getValue]/[setValue], the property's receiver) is where the data actually lives.
 * Obtain instances via [AttachmentRegistry.attachment].
 *
 * ### Usage
 * ```kotlin
 * object MyAttachments : AttachmentRegistry(MyMod.MOD_ID) {
 *     val mana by intAttachment(sync = true, default = { 0 })
 * }
 * var Entity.mana by MyAttachments.mana
 *
 * // in mod init, after MyAttachments' properties above have already run:
 * MyAttachments.init()
 * ```
 *
 * ### Supported holder types
 * What's supported depends on the platform and on whether the attachment was declared with
 * `itemComponent = true`:
 * - **Entity / BlockEntity**: a NeoForge attachment / Fabric `AttachmentTarget`, on both platforms.
 * - **ServerLevel**: NeoForge only. Fabric's `updateTarget` dispatch has no `Level`/`ServerLevel`
 *   case at all, so even where the underlying `get`/`set`/`has` calls happen to succeed there,
 *   `sync = true` will silently never push an update. Don't rely on world-level attachments if
 *   you need Fabric parity.
 * - **ItemStack**: only if declared with `itemComponent = true` (backed by a vanilla
 *   [net.minecraft.core.component.DataComponentType] instead of an attachment). Calling any
 *   method here against an `ItemStack` for an attachment that *wasn't* declared with
 *   `itemComponent = true` throws [NullPointerException] (its backing `DataComponentType` is
 *   null) rather than [IllegalArgumentException] - `ItemStack` still passes CSL's holder-kind
 *   check either way, it just has nowhere to actually read/write.
 *
 * Any other object type throws [IllegalArgumentException] from every method here except
 * [getValue]/[setValue], which forward straight into [get]/[set].
 *
 * ### `sync` vs. `itemComponent`
 * These are two genuinely different mechanisms, not two flavors of one thing:
 * - Entity/BlockEntity/ServerLevel sync (`sync = true` on [AttachmentRegistry.attachment]) is a
 *   reactive push straight out of [set]/[remove] to tracking players, driven by CSL's own
 *   `DataManagerImpl`/packets.
 * - `itemComponent` attachments are **not** covered by that push at all - [set] on an `ItemStack`
 *   never sends anything itself. They ride vanilla's normal item/component replication instead
 *   (the same mechanism as vanilla's own `BundleContents`), which isn't reactive the same way.
 *
 * Declaring `itemComponent = true` without `sync = true` still needs a network codec under the
 * hood (vanilla's `DataComponentType` always carries one) - [AttachmentRegistry.attachment]
 * handles that for you regardless of what you pass for `sync`.
 *
 * ### `has()` after `get()` on Entity/BlockEntity/ServerLevel holders
 * Confirmed on real Fabric/NeoForge attachment internals, not documented by Common Storage Lib
 * itself: [get] on these holder kinds silently creates *and persists* the default value on first
 * read (Fabric's `AttachmentTarget.getAttachedOrCreate`, NeoForge's `AttachmentHolder.getData` -
 * both write-through on a miss, they don't just compute-and-discard). That means [has] can only
 * tell "never touched" apart from "read once" if you call it *before* the first [get] - calling
 * [get] first, then [has], will report `true` even though nothing was ever explicitly [set].
 * `ItemStack`/`itemComponent` holders don't have this quirk - `DataComponentHolder.getOrDefault`
 * genuinely doesn't persist on read.
 */
interface ArchieDataAttachment<T> : ReadWriteProperty<Any?, T>
{
	/**
	 * Reads [holder]'s current value, falling back to the attachment's default if unset. Never
	 * throws for an unset value - only for an unsupported [holder].
	 *
	 * On Entity/BlockEntity/ServerLevel holders, an unset read silently creates *and persists* the
	 * default - see the class-level "`has()` after `get()`" note before relying on [has] afterward.
	 */
	fun get(holder: Any): T

	/** Reads [holder]'s current value, throwing if it's never been explicitly [set]. The exact exception type (`NullPointerException` vs. `RuntimeException`) differs by platform for `ItemStack` holders - don't match on a specific type for that case. */
	fun getOrThrow(holder: Any): T

	/** Reads [holder]'s current value if [has] is true, otherwise [set]s it to [default] first. Returns the (possibly just-written) current value either way. */
	fun getOrCreate(holder: Any, default: T): T

	/** Writes [value] onto [holder], returning [value]. */
	fun set(holder: Any, value: T): T

	/**
	 * Removes [holder]'s explicitly-set value, if any, reverting subsequent [get] calls to the
	 * default. The return value mirrors the removed data 1:1 from the underlying Java API and can
	 * be a JVM-level null if nothing was set - prefer checking [has] first if you actually need it.
	 */
	fun remove(holder: Any): T

	/** True if [holder] has an explicitly-[set] value (as opposed to just reading back the default). */
	fun has(holder: Any): Boolean

	/** Reads [holder]'s current value, applies [block], writes the result back, and returns it. */
	fun modify(holder: Any, block: (T) -> T): T

	override operator fun getValue(thisRef: Any?, property: KProperty<*>): T =
		get(thisRef ?: throw IllegalStateException("${property.name} has no receiver to read a data attachment from - it can't be a top-level property"))

	override operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T)
	{
		set(thisRef ?: throw IllegalStateException("${property.name} has no receiver to write a data attachment to - it can't be a top-level property"), value)
	}
}
