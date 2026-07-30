package net.kernelpanicsoft.archie.serialization.serializers

import io.netty.buffer.Unpooled
import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ByteArraySerializer
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.modules.SerializersModule
import net.kernelpanicsoft.archie.serialization.CodecSerializer
import net.minecraft.core.*
import net.minecraft.core.registries.Registries
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3

/* ─────────────────────── Type aliases ─────────────────────── */

/**
 * Contextual type-alias for [FriendlyByteBuf] that uses [FriendlyByteBufSerializer]
 * when the field is annotated with `@Contextual`.
 */
typealias SFriendlyByteBuf = @Contextual FriendlyByteBuf

/**
 * Contextual type-alias for [ResourceLocation] that uses [ResourceLocationSerializer] when
 * the field is annotated with `@Contextual`.
 */
typealias SResourceLocation = @Contextual ResourceLocation

/**
 * Contextual type-alias for [Vec3i] that uses [Vec3iSerializer] when the field is
 * annotated with `@Contextual`.
 */
typealias SVec3i = @Contextual Vec3i

/**
 * Contextual type-alias for [Vec3] that uses [Vec3Serializer] when the field is
 * annotated with `@Contextual`.
 */
typealias SVec3 = @Contextual Vec3

/**
 * Contextual type-alias for [BlockPos] that uses [BlockPosSerializer] when the field
 * is annotated with `@Contextual`.
 */
typealias SBlockPos = @Contextual BlockPos

/**
 * Contextual type-alias for [ChunkPos] that uses [ChunkPosSerializer] when the field
 * is annotated with `@Contextual`.
 */
typealias SChunkPos = @Contextual ChunkPos

/**
 * Contextual type-alias for [GlobalPos] that uses [GlobalPosSerializer] when the field
 * is annotated with `@Contextual`.
 */
typealias SGlobalPos = @Contextual GlobalPos

/**
 * Contextual type-alias for [BlockHitResult] that uses [BlockHitResultSerializer] when
 * the field is annotated with `@Contextual`.
 */
typealias SBlockHitResult = @Contextual BlockHitResult

/**
 * Contextual type-alias for [ItemStack] that uses a [net.kernelpanicsoft.archie.serialization.CodecSerializer]
 * over [ItemStack.CODEC] when the field is annotated with `@Contextual`.
 */
typealias SItemStack = @Contextual ItemStack

/* ─────────────────────── Serializers ─────────────────────── */

/**
 * A [KSerializer] for [FriendlyByteBuf] that encodes/decodes the buffer contents as a
 * raw byte array.
 *
 * The reader index is preserved after serialization so the buffer can be reused.
 */
object FriendlyByteBufSerializer : KSerializer<FriendlyByteBuf> {
    override val descriptor: SerialDescriptor =
        SerialDescriptor("FriendlyByteBuf", ByteArraySerializer().descriptor)

    override fun serialize(encoder: Encoder, value: FriendlyByteBuf) {
        val index = value.readerIndex()
        val bytes = ByteArray(value.readableBytes())
        value.readBytes(bytes)
        value.readerIndex(index)
        encoder.encodeSerializableValue(ByteArraySerializer(), bytes)
    }

    override fun deserialize(decoder: Decoder): FriendlyByteBuf =
        FriendlyByteBuf(Unpooled.buffer()).apply {
            writeBytes(decoder.decodeSerializableValue(ByteArraySerializer()))
        }
}

/**
 * A [KSerializer] for [ResourceLocation] that encodes/decodes its `namespace:path` string form.
 */
object ResourceLocationSerializer : KSerializer<ResourceLocation>
{
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ResourceLocation", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): ResourceLocation
    {
        return ResourceLocation.parse(decoder.decodeString())
    }

    override fun serialize(encoder: Encoder, value: ResourceLocation)
    {
        encoder.encodeString(value.toString())
    }

}

/**
 * A [KSerializer] for [Vec3i] (and its subclass [BlockPos]) that encodes/decodes the
 * three integer components.
 */
object Vec3iSerializer : KSerializer<Vec3i> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Vec3i") {
        element<Int>("x")
        element<Int>("y")
        element<Int>("z")
    }

    override fun serialize(encoder: Encoder, value: Vec3i) {
        encoder.encodeStructure(descriptor) {
            encodeIntElement(descriptor, 0, value.x)
            encodeIntElement(descriptor, 1, value.y)
            encodeIntElement(descriptor, 2, value.z)
        }
    }

    override fun deserialize(decoder: Decoder): Vec3i =
        decoder.decodeStructure(descriptor) {
            var x: Int? = null; var y: Int? = null; var z: Int? = null
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> x = decodeIntElement(descriptor, 0)
                    1 -> y = decodeIntElement(descriptor, 1)
                    2 -> z = decodeIntElement(descriptor, 2)
                    CompositeDecoder.DECODE_DONE -> break
                    else -> throw SerializationException("Unexpected index: $index")
                }
            }
            Vec3i(x ?: throw SerializationException("Missing x"), y ?: throw SerializationException("Missing y"), z ?: throw SerializationException("Missing z"))
        }
}

/**
 * A [KSerializer] for [Vec3] that encodes/decodes the three double-precision components.
 */
object Vec3Serializer : KSerializer<Vec3> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Vec3") {
        element<Double>("x")
        element<Double>("y")
        element<Double>("z")
    }

    override fun serialize(encoder: Encoder, value: Vec3) {
        encoder.encodeStructure(descriptor) {
            encodeDoubleElement(descriptor, 0, value.x)
            encodeDoubleElement(descriptor, 1, value.y)
            encodeDoubleElement(descriptor, 2, value.z)
        }
    }

    override fun deserialize(decoder: Decoder): Vec3 =
        decoder.decodeStructure(descriptor) {
            var x: Double? = null; var y: Double? = null; var z: Double? = null
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> x = decodeDoubleElement(descriptor, 0)
                    1 -> y = decodeDoubleElement(descriptor, 1)
                    2 -> z = decodeDoubleElement(descriptor, 2)
                    CompositeDecoder.DECODE_DONE -> break
                    else -> throw SerializationException("Unexpected index: $index")
                }
            }
            Vec3(x ?: throw SerializationException("Missing x"), y ?: throw SerializationException("Missing y"), z ?: throw SerializationException("Missing z"))
        }
}

/**
 * A [KSerializer] for [BlockPos] that encodes/decodes the three integer components.
 */
object BlockPosSerializer : KSerializer<BlockPos> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("BlockPos") {
        element<Int>("x")
        element<Int>("y")
        element<Int>("z")
    }

    override fun serialize(encoder: Encoder, value: BlockPos) {
        encoder.encodeStructure(descriptor) {
            encodeIntElement(descriptor, 0, value.x)
            encodeIntElement(descriptor, 1, value.y)
            encodeIntElement(descriptor, 2, value.z)
        }
    }

    override fun deserialize(decoder: Decoder): BlockPos =
        decoder.decodeStructure(descriptor) {
            var x: Int? = null; var y: Int? = null; var z: Int? = null
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> x = decodeIntElement(descriptor, 0)
                    1 -> y = decodeIntElement(descriptor, 1)
                    2 -> z = decodeIntElement(descriptor, 2)
                    CompositeDecoder.DECODE_DONE -> break
                    else -> throw SerializationException("Unexpected index: $index")
                }
            }
            BlockPos(x ?: throw SerializationException("Missing x"), y ?: throw SerializationException("Missing y"), z ?: throw SerializationException("Missing z"))
        }
}

/**
 * A [KSerializer] for [ChunkPos] that encodes/decodes the value as a single packed [Long].
 */
object ChunkPosSerializer : KSerializer<ChunkPos> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ChunkPos", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: ChunkPos) = encoder.encodeLong(value.toLong())
    override fun deserialize(decoder: Decoder): ChunkPos = ChunkPos(decoder.decodeLong())
}

/**
 * A [KSerializer] for [ResourceKey] of a specific registry.
 *
 * The serialized form is a [ResourceLocation] string (the key's location).
 *
 * ### Example
 * ```kotlin
 * val DIMENSION_KEY_SERIALIZER = ResourceKeySerializer(Registries.DIMENSION)
 * ```
 *
 * @param registry The [ResourceKey] of the registry this serializer is scoped to.
 */
class ResourceKeySerializer<T : Any>(val registry: ResourceKey<out Registry<T>>) :
    KSerializer<ResourceKey<*>> {
    companion object {
        /** Pre-built serializer for dimension [ResourceKey]s. */
        val DIMENSION = ResourceKeySerializer(Registries.DIMENSION)
    }

    override val descriptor: SerialDescriptor = ResourceLocationSerializer.descriptor

    override fun serialize(encoder: Encoder, value: ResourceKey<*>) =
        encoder.encodeSerializableValue(ResourceLocationSerializer, value.location())

    override fun deserialize(decoder: Decoder): ResourceKey<*> =
        ResourceKey.create<T>(registry, decoder.decodeSerializableValue(ResourceLocationSerializer))
}

/**
 * A [KSerializer] for [GlobalPos] that encodes the dimension [ResourceKey] and [BlockPos].
 */
object GlobalPosSerializer : KSerializer<GlobalPos> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("GlobalPos") {
        element("dimension", ResourceKeySerializer.DIMENSION.descriptor)
        element("pos", BlockPosSerializer.descriptor)
    }

    override fun serialize(encoder: Encoder, value: GlobalPos) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, ResourceKeySerializer.DIMENSION, value.dimension())
            encodeSerializableElement(descriptor, 1, BlockPosSerializer, value.pos())
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun deserialize(decoder: Decoder): GlobalPos =
        decoder.decodeStructure(descriptor) {
            var dimension: ResourceKey<Level>? = null
            var pos: BlockPos? = null
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> dimension = decodeSerializableElement(descriptor, 0, ResourceKeySerializer.DIMENSION) as ResourceKey<Level>
                    1 -> pos = decodeSerializableElement(descriptor, 1, BlockPosSerializer)
                    CompositeDecoder.DECODE_DONE -> break
                    else -> throw SerializationException("Unexpected index: $index")
                }
            }
            GlobalPos(dimension ?: throw SerializationException("Missing dimension"), pos ?: throw SerializationException("Missing pos"))
        }
}

/**
 * A [KSerializer] for [BlockHitResult] that encodes the hit location, face direction,
 * block position, whether the hit is inside the block, and whether it was a miss.
 */
object BlockHitResultSerializer : KSerializer<BlockHitResult> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("BlockHitResult") {
        element("location", Vec3Serializer.descriptor)
        element<String>("side")
        element("blockPos", BlockPosSerializer.descriptor)
        element<Boolean>("insideBlock")
        element<Boolean>("missed")
    }

    override fun serialize(encoder: Encoder, value: BlockHitResult) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, Vec3Serializer, value.location)
            encodeStringElement(descriptor, 1, value.direction.name)
            encodeSerializableElement(descriptor, 2, BlockPosSerializer, value.blockPos)
            encodeBooleanElement(descriptor, 3, value.isInside)
            encodeBooleanElement(descriptor, 4, value.type == HitResult.Type.MISS)
        }
    }

    override fun deserialize(decoder: Decoder): BlockHitResult {
        var location: Vec3? = null; var side: Direction? = null
        var pos: BlockPos? = null; var inside: Boolean? = null; var missed: Boolean? = null
        decoder.decodeStructure(descriptor) {
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> location = decodeSerializableElement(descriptor, 0, Vec3Serializer)
                    1 -> side = Direction.byName(decodeStringElement(descriptor, 1))
                    2 -> pos = decodeSerializableElement(descriptor, 2, BlockPosSerializer)
                    3 -> inside = decodeBooleanElement(descriptor, 3)
                    4 -> missed = decodeBooleanElement(descriptor, 4)
                    CompositeDecoder.DECODE_DONE -> break
                    else -> throw SerializationException("Unexpected index: $index")
                }
            }
        }
        if (location == null || side == null || pos == null || inside == null || missed == null)
            throw SerializationException("Properties missing when decoding BlockHitResult")
        return if (missed == true) BlockHitResult.miss(location, side!!, pos)
        else BlockHitResult(location, side!!, pos, inside)
    }
}

/**
 * A [SerializersModule] that registers all built-in Minecraft type serializers as contextual
 * serializers.
 *
 * Include this module in your serialization format instances to enable `@Contextual` on
 * Minecraft types.
 */
val MinecraftSerializersModule = SerializersModule {
    contextual(FriendlyByteBuf::class, FriendlyByteBufSerializer)
    contextual(ResourceLocation::class, ResourceLocationSerializer)
    contextual(Vec3i::class, Vec3iSerializer)
    contextual(Vec3::class, Vec3Serializer)
    contextual(BlockPos::class, BlockPosSerializer)
    contextual(ChunkPos::class, ChunkPosSerializer)
    contextual(GlobalPos::class, GlobalPosSerializer)
    contextual(BlockHitResult::class, BlockHitResultSerializer)
    contextual(ItemStack::class, CodecSerializer(ItemStack.CODEC))
}
