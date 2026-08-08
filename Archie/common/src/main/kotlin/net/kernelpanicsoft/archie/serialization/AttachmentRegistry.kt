package net.kernelpanicsoft.archie.serialization

import earth.terrarium.common_storage_lib.data.DataManagerRegistry
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.serializer
import net.kernelpanicsoft.archie.config.toSnakeCase
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty

/**
 * Base class for declaring a mod's [ArchieDataAttachment]s, wrapping a single Common Storage Lib
 * `DataManagerRegistry`. Declare one `object` per mod extending this class, declare attachments
 * as delegated properties on it via [attachment] (or one of the primitive convenience wrappers),
 * then call [init] once at mod-init time - after those property initializers have already run,
 * same ordering as [net.kernelpanicsoft.archie.networking.NetworkChannel]/`Config.init()`.
 *
 * ```kotlin
 * object MyAttachments : AttachmentRegistry(MyMod.MOD_ID) {
 *     val mana by intAttachment(sync = true, default = { 0 })
 * }
 *
 * // mod init:
 * MyAttachments.init()
 * ```
 */
abstract class AttachmentRegistry(modId: String)
{
	@PublishedApi
	internal val registry = DataManagerRegistry(modId)

	/**
	 * Declares an [ArchieDataAttachment] backed by [serializer], keyed by the delegated property's
	 * snake_case name. See [ArchieDataAttachment] for the full contract, including exactly what
	 * [sync] and [itemComponent] each do and don't cover.
	 *
	 * @param sync Reactively push updates to tracking players on every [ArchieDataAttachment.set]/
	 *   [ArchieDataAttachment.remove] - Entity/BlockEntity on both loaders, ServerLevel on NeoForge
	 *   only.
	 * @param copyOnDeath Preserve the value across a player's death/respawn. Entity/BlockEntity
	 *   holders only; meaningless (and untested by Archie) for `itemComponent`-only attachments.
	 * @param itemComponent Additionally back this attachment with a vanilla `DataComponentType`,
	 *   making it usable on `ItemStack` holders too.
	 * @param default Supplies the value used before a holder has anything explicitly set.
	 */
	fun <T : Any> attachment(
		serializer: KSerializer<T>,
		sync: Boolean = false,
		copyOnDeath: Boolean = false,
		itemComponent: Boolean = false,
		default: () -> T,
	): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, ArchieDataAttachment<T>>> = PropertyDelegateProvider { _, property ->
		val builder = registry.builder(default).serialize(serializer.codec)
		// itemComponent needs a client codec regardless of `sync`: CSL's own builder unconditionally
		// calls `.networkSynchronized(clientCodec)` when building the DataComponentType, and leaves
		// clientCodec null unless networkSerializer(...) was called - passing null there breaks at
		// registration time. Always supplying our own explicit StreamCodec here (rather than CSL's
		// no-arg networkSerializer(), which derives one from the Codec instead) keeps this consistent
		// with the rest of Archie's serialization, which encodes over the network via kotlinx CBOR
		// directly rather than round-tripping through a Codec.
		if (sync || itemComponent) builder.networkSerializer(serializer.streamCodec)
		if (copyOnDeath) builder.copyOnDeath()
		if (itemComponent) builder.withDataComponent()
		// ArchieDataAttachment itself implements ReadWriteProperty (so it can *also* back a `var
		// Holder.x by MyAttachments.x` extension property once resolved) - if `attachment(...)`
		// returned it directly as the PropertyDelegateProvider's own delegate type, `by attachment(...)`
		// here would unwrap straight through to ArchieDataAttachment's getValue() and bind `mana`'s
		// type to T, not to ArchieDataAttachment<T> itself. Wrapping it in a plain ReadOnlyProperty
		// stops that second unwrap, the same way NBTHolderImpl.itemField/fluidField/energyField wrap
		// their storage objects for the exact same reason.
		val attachment = ArchieDataAttachmentImpl(builder.buildAndRegister(property.name.toSnakeCase()))
		ReadOnlyProperty { _, _ -> attachment }
	}

	fun booleanAttachment(sync: Boolean = false, copyOnDeath: Boolean = false, itemComponent: Boolean = false, default: () -> Boolean = { false }) =
		attachment(Boolean.serializer(), sync, copyOnDeath, itemComponent, default)
	fun byteAttachment(sync: Boolean = false, copyOnDeath: Boolean = false, itemComponent: Boolean = false, default: () -> Byte = { 0 }) =
		attachment(Byte.serializer(), sync, copyOnDeath, itemComponent, default)
	fun shortAttachment(sync: Boolean = false, copyOnDeath: Boolean = false, itemComponent: Boolean = false, default: () -> Short = { 0 }) =
		attachment(Short.serializer(), sync, copyOnDeath, itemComponent, default)
	fun intAttachment(sync: Boolean = false, copyOnDeath: Boolean = false, itemComponent: Boolean = false, default: () -> Int = { 0 }) =
		attachment(Int.serializer(), sync, copyOnDeath, itemComponent, default)
	fun longAttachment(sync: Boolean = false, copyOnDeath: Boolean = false, itemComponent: Boolean = false, default: () -> Long = { 0 }) =
		attachment(Long.serializer(), sync, copyOnDeath, itemComponent, default)
	fun floatAttachment(sync: Boolean = false, copyOnDeath: Boolean = false, itemComponent: Boolean = false, default: () -> Float = { 0.0f }) =
		attachment(Float.serializer(), sync, copyOnDeath, itemComponent, default)
	fun doubleAttachment(sync: Boolean = false, copyOnDeath: Boolean = false, itemComponent: Boolean = false, default: () -> Double = { 0.0 }) =
		attachment(Double.serializer(), sync, copyOnDeath, itemComponent, default)
	fun stringAttachment(sync: Boolean = false, copyOnDeath: Boolean = false, itemComponent: Boolean = false, default: () -> String = { "" }) =
		attachment(String.serializer(), sync, copyOnDeath, itemComponent, default)

	/** Registers every attachment declared through this registry against the mod event bus. Call once, at mod-init time, after all of this object's `by attachment(...)` properties have already run. */
	fun init() = registry.init()
}

/** Reified variant of [AttachmentRegistry.attachment] that resolves the [KSerializer] for [T] automatically. */
inline fun <reified T : Any> AttachmentRegistry.attachment(
	sync: Boolean = false,
	copyOnDeath: Boolean = false,
	itemComponent: Boolean = false,
	noinline default: () -> T,
): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, ArchieDataAttachment<T>>> = attachment(serializer<T>(), sync, copyOnDeath, itemComponent, default)
