package com.withertech.archie.serialization

import com.withertech.archie.util.toMutableEntry
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.internal.AbstractPolymorphicSerializer
import net.benwoodworth.knbt.*
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.nbt.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.experimental.ExperimentalTypeInference

val NBT = Nbt {
	variant = NbtVariant.Java
	compression = NbtCompression.None
}

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

@OptIn(ExperimentalTypeInference::class, ExperimentalContracts::class)
inline fun <T : NbtTag> buildListTag(
	@BuilderInference builderAction: NbtListBuilder<T>.() -> Unit,
): ListTag
{
	contract { callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE) }
	return buildNbtList(builderAction).toMinecraft
}

@OptIn(ExperimentalContracts::class)
inline fun buildCompoundTag(builderAction: NbtCompoundBuilder.() -> Unit): CompoundTag
{
	contract { callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE) }
	return buildNbtCompound(builderAction).toMinecraft
}

@OptIn(ExperimentalContracts::class)
inline fun mergeToCompoundTag(compoundTag: CompoundTag, builderAction: NbtCompoundBuilder.() -> Unit)
{
	contract { callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE) }
	buildNbtCompound(builderAction).forEach { (key, value) ->
		compoundTag.put(key, value.toMinecraft)
	}
}

@OptIn(ExperimentalContracts::class)
inline fun forEachTag(listTag: ListTag, action: (NbtTag) -> Unit)
{
	contract { callsInPlace(action, InvocationKind.UNKNOWN) }
	listTag.fromMinecraft?.forEach { value ->
		action(value)
	}
}

@OptIn(ExperimentalContracts::class)
inline fun forEachTag(compoundTag: CompoundTag, action: (Map.Entry<String, NbtTag>) -> Unit)
{
	contract { callsInPlace(action, InvocationKind.UNKNOWN) }
	compoundTag.fromMinecraft.forEach { (key, value) ->
		action((key to value).toMutableEntry())
	}
}

@OptIn(ExperimentalContracts::class)
inline fun buildComponentPatch(builderAction: DataComponentPatch.Builder.() -> Unit): DataComponentPatch
{
	contract { callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE) }
	return DataComponentPatch.builder().apply(builderAction).build()
}

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

val NbtCompound.toMinecraft: CompoundTag
	get() = CompoundTag().apply {
		mapValues { it.value.toMinecraft }.forEach { (key, value) ->
			put(key, value)
		}
	}

val NbtList<*>.toMinecraft: ListTag
	get() = ListTag().apply {
		this@toMinecraft.map {
			it.toMinecraft
		}.forEach {
			add(it)
		}
	}

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

val CompoundTag.fromMinecraft: NbtCompound
	get() = buildNbtCompound {
		this@fromMinecraft.allKeys.associateWith {
			this@fromMinecraft[it]?.fromMinecraft
		}.forEach { (key, value) ->
			if (value != null)
				put(key, value)
		}
	}
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