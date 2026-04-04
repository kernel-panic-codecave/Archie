package net.kernelpanicsoft.archie.data


actual object ADataGeneratorPlatform
{
	actual val isDataGen: Boolean
		get() = System.getProperty("archie.datagen").toBoolean()
}