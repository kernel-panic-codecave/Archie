package net.kernelpanicsoft.archie.data.platform

/** Fabric implementation of [ADataGeneratorPlatform]. */
@Suppress("unused")
actual object ADataGeneratorPlatform
{
	/** True when launched via `fabric:runDatagen`, which sets the `archie.datagen` system property. */
	actual val isDataGen: Boolean
		get() = System.getProperty("archie.datagen").toBoolean()
}
