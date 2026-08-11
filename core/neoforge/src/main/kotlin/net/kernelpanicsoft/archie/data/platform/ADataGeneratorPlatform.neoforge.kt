package net.kernelpanicsoft.archie.data.platform


/** NeoForge implementation of [ADataGeneratorPlatform]. */
actual object ADataGeneratorPlatform
{
	/** True when launched via `neoforge:runDatagen`, which sets the `archie.datagen` system property. */
	actual val isDataGen: Boolean
		get() = System.getProperty("archie.datagen").toBoolean()
}