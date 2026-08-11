package net.kernelpanicsoft.archie.serialization

import net.kernelpanicsoft.archie.util.toMutableEntry
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.internal.AbstractPolymorphicSerializer
import kotlinx.serialization.serializer
import net.benwoodworth.knbt.*
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.nbt.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.experimental.ExperimentalTypeInference

/** A [Nbt] (knbt) instance pre-configured for Minecraft's Java-edition NBT format, uncompressed. */
val NBT = Nbt {
	variant = NbtVariant.Java
	compression = NbtCompression.None
}

/**
 * Like [Nbt.encodeToNbtTag], but for class/polymorphic types unwraps the single top-level
 * compound entry keyed by the serial name, returning its value directly instead of a
 * one-entry [NbtCompound] wrapper.
 */
@OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
fun <T> Nbt.encodeToNbtTagRootless(serializer: SerializationStrategy<T>, value: T): NbtTag
{
	return if (serializer.descriptor.kind == StructureKind.CLASS ||
		serializer is AbstractPolymorphicSerializer
	)
		encodeToNbtTag(serializer, value).nbtCompound[serializer.descriptor.serialName]!!
	else
		encodeToNbtTag(serializer, value)
}

/**
 * The inverse of [encodeToNbtTagRootless]: decodes [tag] as [T], re-wrapping it in a one-entry
 * compound keyed by the serial name first if [T] is a class/polymorphic type.
 */
@OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
fun <T> Nbt.decodeFromNbtTagRootless(deserializer: DeserializationStrategy<T>, tag: NbtTag): T
{
	return if (deserializer.descriptor.kind == StructureKind.CLASS ||
		deserializer is AbstractPolymorphicSerializer
	)
		decodeFromNbtTag(deserializer, buildNbtCompound {
			put(deserializer.descriptor.serialName, tag)
		})
	else
		decodeFromNbtTag(deserializer, tag)
}

/** Reified variant of [encodeToNbtTagRootless] that resolves [T]'s serializer automatically. */
@OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
inline fun <reified T> Nbt.encodeToNbtTagRootless(value: T): NbtTag =
	encodeToNbtTagRootless(serializersModule.serializer(), value)

/** Reified variant of [decodeFromNbtTagRootless] that resolves [T]'s serializer automatically. */
@OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
inline fun <reified T> Nbt.decodeFromNbtTagRootless(tag: NbtTag): T =
	decodeFromNbtTagRootless(serializersModule.serializer(), tag)


/** Builds a Minecraft [ListTag] using knbt's [NbtListBuilder] DSL via [builderAction]. */
@OptIn(ExperimentalTypeInference::class, ExperimentalContracts::class)
inline fun <T : NbtTag> buildListTag(
	@BuilderInference builderAction: NbtListBuilder<T>.() -> Unit,
): ListTag
{
	contract { callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE) }
	return buildNbtList(builderAction).toMinecraft
}

/** Builds a Minecraft [CompoundTag] using knbt's [NbtCompoundBuilder] DSL via [builderAction]. */
@OptIn(ExperimentalContracts::class)
inline fun buildCompoundTag(builderAction: NbtCompoundBuilder.() -> Unit): CompoundTag
{
	contract { callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE) }
	return buildNbtCompound(builderAction).toMinecraft
}

/** Builds entries via knbt's [NbtCompoundBuilder] DSL and puts each of them into the existing [compoundTag]. */
@OptIn(ExperimentalContracts::class)
inline fun mergeToCompoundTag(compoundTag: CompoundTag, builderAction: NbtCompoundBuilder.() -> Unit)
{
	contract { callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE) }
	buildNbtCompound(builderAction).forEach { (key, value) ->
		compoundTag.put(key, value.toMinecraft)
	}
}

/** Runs [action] for each element of [listTag], converted to a knbt [NbtTag]. No-op for an empty/untyped list. */
@OptIn(ExperimentalContracts::class)
inline fun forEachTag(listTag: ListTag, action: (NbtTag) -> Unit)
{
	contract { callsInPlace(action, InvocationKind.UNKNOWN) }
	listTag.fromMinecraft?.forEach { value ->
		action(value)
	}
}

/** Runs [action] for each key/value entry of [compoundTag], with the value converted to a knbt [NbtTag]. */
@OptIn(ExperimentalContracts::class)
inline fun forEachTag(compoundTag: CompoundTag, action: (Map.Entry<String, NbtTag>) -> Unit)
{
	contract { callsInPlace(action, InvocationKind.UNKNOWN) }
	compoundTag.fromMinecraft.forEach { (key, value) ->
		action((key to value).toMutableEntry())
	}
}

/** Builds a [DataComponentPatch] using Minecraft's [DataComponentPatch.Builder] DSL via [builderAction]. */
@OptIn(ExperimentalContracts::class)
inline fun buildComponentPatch(builderAction: DataComponentPatch.Builder.() -> Unit): DataComponentPatch
{
	contract { callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE) }
	return DataComponentPatch.builder().apply(builderAction).build()
}

/** Converts a knbt tag to the equivalent Minecraft [Tag] (a `null` receiver becomes [EndTag]). */
val NbtTag?.toMinecraft: Tag
	get() = when (this)
	{
		null -> EndTag.INSTANCE
		is NbtByte -> ByteTag.valueOf(value)
		is NbtByteArray -> ByteArrayTag(this)
		is NbtCompound -> toMinecraft
		is NbtDouble -> DoubleTag.valueOf(value)
		is NbtFloat -> FloatTag.valueOf(value)
		is NbtInt -> IntTag.valueOf(value)
		is NbtIntArray -> IntArrayTag(this)
		is NbtList<*> -> toMinecraft
		is NbtLong -> LongTag.valueOf(value)
		is NbtLongArray -> LongArrayTag(this)
		is NbtShort -> ShortTag.valueOf(value)
		is NbtString -> StringTag.valueOf(value)
	}

/** Converts a knbt [NbtCompound] to the equivalent Minecraft [CompoundTag]. */
val NbtCompound.toMinecraft: CompoundTag
	get() = CompoundTag().apply {
		mapValues { it.value.toMinecraft }.forEach { (key, value) ->
			put(key, value)
		}
	}

/** Converts a knbt [NbtList] to the equivalent Minecraft [ListTag]. */
val NbtList<*>.toMinecraft: ListTag
	get() = ListTag().apply {
		this@toMinecraft.map {
			it.toMinecraft
		}.forEach {
			add(it)
		}
	}

/** Converts a Minecraft [Tag] to the equivalent knbt tag, or `null` for an [EndTag]. */
val Tag.fromMinecraft: NbtTag?
	get() = when (id.toInt())
	{
		0 -> null
		1 -> NbtByte((this as NumericTag).asByte)
		2 -> NbtShort((this as NumericTag).asShort)
		3 -> NbtInt((this as NumericTag).asInt)
		4 -> NbtLong((this as NumericTag).asLong)
		5 -> NbtFloat((this as NumericTag).asFloat)
		6 -> NbtDouble((this as NumericTag).asDouble)
		7 -> NbtByteArray((this as ByteArrayTag).asByteArray)
		8 -> NbtString(this.asString)
		9 -> (this as ListTag).fromMinecraft
		10 -> (this as CompoundTag).fromMinecraft
		11 -> NbtIntArray((this as IntArrayTag).asIntArray)
		12 -> NbtLongArray((this as LongArrayTag).asLongArray)
		else -> throw IllegalStateException("Unknown tag type: $this")
	}

/** Converts a Minecraft [CompoundTag] to the equivalent knbt [NbtCompound]. */
val CompoundTag.fromMinecraft: NbtCompound
	get() = buildNbtCompound {
		this@fromMinecraft.allKeys.associateWith {
			this@fromMinecraft[it]?.fromMinecraft
		}.forEach { (key, value) ->
			if (value != null)
				put(key, value)
		}
	}
/** Converts a Minecraft [ListTag] to the equivalent knbt [NbtList], or `null` for an untyped (empty) list. */
val ListTag.fromMinecraft: NbtList<*>?
	get() = when (elementType.toInt())
	{
		0 -> null
		1 -> buildNbtList {
			forEach {
				add(it.fromMinecraft as NbtByte)
			}
		}

		2 -> buildNbtList {
			forEach {
				add(it.fromMinecraft as NbtShort)
			}
		}

		3 -> buildNbtList {
			forEach {
				add(it.fromMinecraft as NbtInt)
			}
		}

		4 -> buildNbtList {
			forEach {
				add(it.fromMinecraft as NbtLong)
			}
		}

		5 -> buildNbtList {
			forEach {
				add(it.fromMinecraft as NbtFloat)
			}
		}

		6 -> buildNbtList {
			forEach {
				add(it.fromMinecraft as NbtDouble)
			}
		}

		7 -> buildNbtList {
			forEach {
				add(it.fromMinecraft as NbtByteArray)
			}
		}

		8 -> buildNbtList {
			forEach {
				add(it.fromMinecraft as NbtString)
			}
		}

		9 -> buildNbtList<NbtList<*>> {
			forEach {
				add(it.fromMinecraft as NbtList<*>)
			}
		}

		10 -> buildNbtList {
			forEach {
				add(it.fromMinecraft as NbtCompound)
			}
		}

		11 -> buildNbtList {
			forEach {
				add(it.fromMinecraft as NbtIntArray)
			}
		}

		12 -> buildNbtList {
			forEach {
				add(it.fromMinecraft as NbtLongArray)
			}
		}

		else -> throw IllegalStateException("Unknown tag type: $this")
	}