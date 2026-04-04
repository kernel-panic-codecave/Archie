package net.kernelpanicsoft.archie.util

@JvmName("getReflectionExtension")
inline fun <reified T, R> T.getReflection(field: String): R = getReflection(this, field)
@JvmName("setReflectionExtension")
inline fun <reified T, R> T.setReflection(field: String, value: R) = setReflection(this, field, value)

inline fun <reified T, R> getReflection(instance: T, field: String): R
{
	val f = generateSequence(T::class.java as Class<*>) { it.superclass }
		.mapNotNull { clazz -> runCatching { clazz.getDeclaredField(field) }.getOrNull() }
		.firstOrNull()
		?: throw NoSuchFieldException(field)
	f.isAccessible = true
	@Suppress("UNCHECKED_CAST")
	return f.get(instance) as R
}

inline fun <reified T, R> setReflection(instance: T, field: String, value: R)
{
	val f = generateSequence(T::class.java as Class<*>) { it.superclass }
		.mapNotNull { clazz -> runCatching { clazz.getDeclaredField(field) }.getOrNull() }
		.firstOrNull()
		?: throw NoSuchFieldException(field)
	f.isAccessible = true
	f.set(instance, value)
}