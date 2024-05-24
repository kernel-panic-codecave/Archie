package com.withertech.archie.util

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.experimental.ExperimentalTypeInference

@OptIn(ExperimentalTypeInference::class, ExperimentalContracts::class)
inline fun <reified T> buildArray(@BuilderInference builderAction: MutableList<T>.() -> Unit): Array<T>
{
	contract { callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE) }
	return buildList(builderAction).toTypedArray()
}

@OptIn(ExperimentalTypeInference::class, ExperimentalContracts::class)
inline fun <reified T> buildArray(capacity: Int, @BuilderInference builderAction: MutableList<T>.() -> Unit): Array<T>
{
	contract { callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE) }
	return buildList(capacity, builderAction).toTypedArray()
}