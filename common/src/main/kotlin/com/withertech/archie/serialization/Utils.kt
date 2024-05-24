@file:Suppress("FunctionName", "unused")

package com.withertech.archie.serialization

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ArraySerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.reflect.KClass

/**
 * Returns serial descriptor that delegates all the calls to descriptor returned by [deferred] block.
 * Used to resolve cyclic dependencies between recursive serializable structures.
 */
@OptIn(ExperimentalSerializationApi::class)
fun defer(deferred: () -> SerialDescriptor): SerialDescriptor = object : SerialDescriptor
{

	private val original: SerialDescriptor by lazy(deferred)

	override val serialName: String
		get() = original.serialName
	override val kind: SerialKind
		get() = original.kind
	override val elementsCount: Int
		get() = original.elementsCount

	override fun getElementName(index: Int): String = original.getElementName(index)
	override fun getElementIndex(name: String): Int = original.getElementIndex(name)
	override fun getElementAnnotations(index: Int): List<Annotation> = original.getElementAnnotations(index)
	override fun getElementDescriptor(index: Int): SerialDescriptor = original.getElementDescriptor(index)
	override fun isElementOptional(index: Int): Boolean = original.isElementOptional(index)
}

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