package net.kernelpanicsoft.archie.util

/** Extension form of [getReflection]; reads [field] (searching up the class hierarchy) from this instance. */
@JvmName("getReflectionExtension")
inline fun <reified T, R> T.getReflection(field: String): R = getReflection(this, field)
/** Extension form of [setReflection]; writes [field] (searching up the class hierarchy) on this instance. */
@JvmName("setReflectionExtension")
inline fun <reified T, R> T.setReflection(field: String, value: R) = setReflection(this, field, value)

/**
 * Reads a private/inaccessible declared field named [field] off [instance] via reflection,
 * searching [T] and its superclasses.
 *
 * @throws NoSuchFieldException if no field named [field] is found anywhere in the hierarchy.
 */
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

/**
 * Writes [value] to a private/inaccessible declared field named [field] on [instance] via
 * reflection, searching [T] and its superclasses.
 *
 * @throws NoSuchFieldException if no field named [field] is found anywhere in the hierarchy.
 */
inline fun <reified T, R> setReflection(instance: T, field: String, value: R)
{
	val f = generateSequence(T::class.java as Class<*>) { it.superclass }
		.mapNotNull { clazz -> runCatching { clazz.getDeclaredField(field) }.getOrNull() }
		.firstOrNull()
		?: throw NoSuchFieldException(field)
	f.isAccessible = true
	f.set(instance, value)
}