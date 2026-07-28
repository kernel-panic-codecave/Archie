@file:Suppress("FunctionName", "unused")
@file:OptIn(ExperimentalSerializationApi::class)

package net.kernelpanicsoft.archie.serialization

import com.google.gson.JsonParser
import com.mojang.datafixers.util.Pair
import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import com.mojang.serialization.DynamicOps
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SealedSerializationApi
import kotlinx.serialization.builtins.ArraySerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.serializer
import net.kernelpanicsoft.archie.Archie
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import kotlin.reflect.KClass
import com.google.gson.JsonElement as GsonElement

/**
 * Gets data from a [RegistryFriendlyByteBuf] using the provided [KSerializer]
 */
fun <T : Any> RegistryFriendlyByteBuf.read(serializer: KSerializer<T>): T =
	SerializationManager.cbor.decodeFromByteArray(serializer, readByteArray())

/**
 * Writes data into a [RegistryFriendlyByteBuf] using the [KSerializer] using the class of the data
 */
@OptIn(InternalSerializationApi::class)
fun <T : Any> RegistryFriendlyByteBuf.write(data: T) = write(data::class.serializer() as KSerializer<T>, data)

/**
 * Writes data into a [RegistryFriendlyByteBuf] using a [KSerializer]
 */
fun <T : Any> RegistryFriendlyByteBuf.write(serializer: KSerializer<T>, data: T) =
	writeBytes(SerializationManager.cbor.encodeToByteArray(serializer, data))

/**
 * Converts a [Codec] into a [KSerializer].
 *
 * **Note:** By default `JsonOps` and `NbtOps` are supported. Check [SerializationManager.registerOp] to register another [DynamicOps].
 * Trying to use unregistered [DynamicOps] implementations will result in an [UnsupportedOperationException].
 */
val <T : Any> Codec<T>.kSerializer: KSerializer<T>
	get() = CodecSerializer(this)

/**
 * Converts a [KSerializer] into a [Codec].
 *
 * **Note:** By default `JsonOps` and `NbtOps` are supported. Check [SerializationManager.registerOp] to register another [DynamicOps].
 * Trying to use unregistered [DynamicOps] implementations will result in an [UnsupportedOperationException].
 *
 * ### Example
 * ```kotlin
 * @Serializable
 * data class TestData(
 *     val str: String,
 *     val int: Int,
 *     val float: Float,
 *     val double: Double,
 *     val boolean: Boolean
 * )
 *
 * // Using the toCodec extension:
 * val TestCodec = TestData.serializer().toCodec()
 *
 * // The above is equivalent to manually creating a codec:
 * val ManualTestCodec: Codec<TestData> = RecordCodecBuilder.create {
 *     it.group(
 *         Codec.STRING.fieldOf("str").forGetter(TestData::str),
 *         Codec.INT.fieldOf("int").forGetter(TestData::int),
 *         Codec.FLOAT.fieldOf("float").forGetter(TestData::float),
 *         Codec.DOUBLE.fieldOf("double").forGetter(TestData::double),
 *         Codec.BOOL.fieldOf("boolean").forGetter(TestData::boolean)
 *     ).apply(it, ::TestData)
 * }
 * ```
 *
 * @return A [Codec] of type `T` defined by the [KSerializer].
 * @throws UnsupportedOperationException If the provided `DynamicOps` type is not supported.
 */
val <T : Any> KSerializer<T>.codec: Codec<T>
	get() = SerializerCodec(this)

/**
 * Converts any [KSerializer] into a [StreamCodec] using Cbor
 */
val <T : Any> KSerializer<T>.streamCodec: StreamCodec<RegistryFriendlyByteBuf, T>
	get() = StreamCodec.of<RegistryFriendlyByteBuf, T>(
		{ buffer, value -> buffer.writeByteArray(SerializationManager.cbor.encodeToByteArray(this, value)) },
		{ buffer -> SerializationManager.cbor.decodeFromByteArray(this, buffer.readByteArray()) }
	)

/**
 * Returns serial descriptor that delegates all the calls to descriptor returned by [deferred] block.
 * Used to resolve cyclic dependencies between recursive serializable structures.
 */
@OptIn(SealedSerializationApi::class)
fun defer(deferred: () -> SerialDescriptor): SerialDescriptor = object : SerialDescriptor {

	private val original: SerialDescriptor by lazy(deferred)

	override val serialName: String
		get() = original.serialName
	override val kind: SerialKind
		get() = original.kind
	override val elementsCount: Int
		get() = original.elementsCount
	override val isInline: Boolean
		get() = original.isInline
	override val isNullable: Boolean
		get() = original.isNullable
	override val annotations: List<Annotation>
		get() = original.annotations

	override fun getElementName(index: Int): String = original.getElementName(index)
	override fun getElementIndex(name: String): Int = original.getElementIndex(name)
	override fun getElementAnnotations(index: Int): List<Annotation> = original.getElementAnnotations(index)
	override fun getElementDescriptor(index: Int): SerialDescriptor = original.getElementDescriptor(index)
	override fun isElementOptional(index: Int): Boolean = original.isElementOptional(index)
}

/**
 * Used for [KSerializer.codec], you could extend this class to make any modifications you like.
 *
 * **Note:** It is HIGHLY recommended to just use the extension function [KSerializer.codec] instead of manually using this class.
 */
open class SerializerCodec<T : Any>(private val serializer: KSerializer<T>) : Codec<T> {
	@Suppress("UNCHECKED_CAST")
	override fun <V : Any> encode(input: T, ops: DynamicOps<V>, prefix: V): DataResult<V>
	{
		return tryOrThrow {
			val cod = SerializationManager[ops]
				?: throw UnsupportedOperationException("${ops::class.simpleName} is not a supported DynamicOps instance.")

			cod.encode(input, serializer as KSerializer<Any>) as V
		}
	}

	@Suppress("UNCHECKED_CAST")
	override fun <V : Any> decode(
		ops: DynamicOps<V>,
		input: V
	): DataResult<Pair<T, V>> {
		return tryOrThrow {
			val cod = SerializationManager[ops]
				?: throw UnsupportedOperationException("${ops::class.simpleName} is not a supported DynamicOps instance.")

			val value = cod.decode(input, serializer as KSerializer<Any>) as T

			Pair(value, input)
		}
	}
}

internal fun <T : Any> tryOrThrow(action: () -> T): DataResult<T> {
	return try {
		DataResult.success(action())
	} catch (err: Exception) {
		DataResult.error(err::message)
	}
}

/**
 * Used for [Codec.kSerializer], you could extend this class to make any modifications you like.
 *
 * **Note:** It is HIGHLY recommended to just use the extension function [Codec.kSerializer] instead of manually using this class.
 */
@Suppress("UNCHECKED_CAST")
open class CodecSerializer<T>(private val codec: Codec<T>) : KSerializer<T> {
	@OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
	override val descriptor: SerialDescriptor = defer {
		buildSerialDescriptor("CodecSerializer", PolymorphicKind.SEALED) {
			SerializationManager.serializers.forEach {
				element(it.name ?: it.descriptor.serialName, defer { it.descriptor })
			}
		}
	}

	override fun serialize(encoder: Encoder, value: T) {
		val ser = SerializationManager[encoder]
			?: throw UnsupportedOperationException("${encoder::class.simpleName} is not a supported serializer type.")

		ser.operation.encode(encoder, codec as Codec<Any>, value as Any)
	}

	override fun deserialize(decoder: Decoder): T {
		val ser = SerializationManager[decoder]
			?: throw UnsupportedOperationException("${decoder::class.simpleName} is not a supported serializer type.")

		return ser.operation.decode(decoder, codec as Codec<Any>) as T
	}
}

/**
 * Convert any [JsonElement] from kotlinx.serialization.json into [GsonElement] from gson
 */
val JsonElement.toGson: GsonElement
	get() = JsonParser.parseString(this.toString())

/**
 * Convert any [GsonElement] from gson into [JsonElement] kotlinx.serialization.json
 */
val GsonElement.toKson: JsonElement
	get() = SerializationManager.json.parseToJsonElement(this.toString())

/**
 * Returns serializer for reference [Array] of type [E] with [descriptor][SerialDescriptor] of [StructureKind.LIST] kind.
 * Each element of the array is serialized with the given [elementSerializer].
 *
 * [KSerializer.descriptor] is deferred to resolve cyclic dependencies
 */
@ExperimentalSerializationApi
inline fun <reified T : Any, reified E : T> DeferredArraySerializer(elementSerializer: KSerializer<E>): KSerializer<Array<E>> =
	DeferredArraySerializer(T::class, elementSerializer)

/**
 * Returns serializer for reference [Array] of type [E] with [descriptor][SerialDescriptor] of [StructureKind.LIST] kind.
 * Each element of the array is serialized with the given [elementSerializer].
 *
 * [KSerializer.descriptor] is deferred to resolve cyclic dependencies
 */
@ExperimentalSerializationApi
fun <T : Any, E : T> DeferredArraySerializer(
	kClass: KClass<T>,
	elementSerializer: KSerializer<E>
): KSerializer<Array<E>> = object : KSerializer<Array<E>>
{
	private val surrogate by lazy { ArraySerializer(kClass, elementSerializer) }

	override val descriptor: SerialDescriptor = defer { surrogate.descriptor }

	override fun deserialize(decoder: Decoder): Array<E> = surrogate.deserialize(decoder)

	override fun serialize(encoder: Encoder, value: Array<E>) = surrogate.serialize(encoder, value)
}

/**
 * Creates a serializer for [`List<T>`][List] for the given serializer of type [T].
 *
 * [KSerializer.descriptor] is deferred to resolve cyclic dependencies
 */
fun <T> DeferredListSerializer(elementSerializer: KSerializer<T>): KSerializer<List<T>> = object : KSerializer<List<T>>
{
	private val surrogate by lazy { ListSerializer(elementSerializer) }

	override val descriptor: SerialDescriptor = defer { surrogate.descriptor }

	override fun deserialize(decoder: Decoder): List<T> = surrogate.deserialize(decoder)

	override fun serialize(encoder: Encoder, value: List<T>) = surrogate.serialize(encoder, value)
}

/**
 * Creates a serializer for [`Set<T>`][Set] for the given serializer of type [T].
 *
 * [KSerializer.descriptor] is deferred to resolve cyclic dependencies
 */
fun <T> DeferredSetSerializer(elementSerializer: KSerializer<T>): KSerializer<Set<T>> = object : KSerializer<Set<T>>
{
	private val surrogate by lazy { SetSerializer(elementSerializer) }

	override val descriptor: SerialDescriptor = defer { surrogate.descriptor }

	override fun deserialize(decoder: Decoder): Set<T> = surrogate.deserialize(decoder)

	override fun serialize(encoder: Encoder, value: Set<T>) = surrogate.serialize(encoder, value)
}

/**
 * Creates a serializer for [`Map<K, V>`][Map] for the given serializers for
 * its ket type [K] and value type [V].
 *
 * [KSerializer.descriptor] is deferred to resolve cyclic dependencies
 */
fun <K, V> DeferredMapSerializer(
	keySerializer: KSerializer<K>,
	valueSerializer: KSerializer<V>
): KSerializer<Map<K, V>> = object : KSerializer<Map<K, V>>
{
	private val surrogate by lazy { MapSerializer(keySerializer, valueSerializer) }
	override val descriptor: SerialDescriptor = defer { surrogate.descriptor }

	override fun deserialize(decoder: Decoder): Map<K, V> = surrogate.deserialize(decoder)

	override fun serialize(encoder: Encoder, value: Map<K, V>) = surrogate.serialize(encoder, value)
}