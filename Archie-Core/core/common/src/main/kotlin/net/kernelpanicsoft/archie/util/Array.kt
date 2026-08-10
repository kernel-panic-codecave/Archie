package net.kernelpanicsoft.archie.util

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.experimental.ExperimentalTypeInference

/** Builds a reference [Array] of [T] using the [buildList] DSL via [builderAction]. */
@OptIn(ExperimentalTypeInference::class, ExperimentalContracts::class)
inline fun <reified T> buildArray(@BuilderInference builderAction: MutableList<T>.() -> Unit): Array<T>
{
	contract { callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE) }
	return buildList(builderAction).toTypedArray()
}

/** Like [buildArray], but pre-sizes the backing list to [capacity]. */
@OptIn(ExperimentalTypeInference::class, ExperimentalContracts::class)
inline fun <reified T> buildArray(capacity: Int, @BuilderInference builderAction: MutableList<T>.() -> Unit): Array<T>
{
	contract { callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE) }
	return buildList(capacity, builderAction).toTypedArray()
}