package net.kernelpanicsoft.archie.data

@Suppress("unused")
actual object ADataGeneratorPlatform
{
	actual val isDataGen: Boolean
		get() = System.getProperty("archie.datagen").toBoolean()
}
