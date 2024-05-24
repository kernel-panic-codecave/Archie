package com.withertech.archie.util

@JvmName("getReflectionExtension")
inline fun <reified T, R> T.getReflection(field: String): R = getReflection(this, field)
@JvmName("setReflectionExtension")
inline fun <reified T, R> T.setReflection(field: String, value: R) = setReflection(this, field, value)

inline fun <reified T, R> getReflection(instance: T, field: String): R
{
	val f = T::class.java.getDeclaredField(field)
	f.isAccessible = true
	@Suppress("UNCHECKED_CAST")
	return f.get(instance) as R
}

inline fun <reified T, R> setReflection(instance: T, field: String, value: R)
{
	val f = T::class.java.getDeclaredField(field)
	f.isAccessible = true
	f.set(instance, value)
}