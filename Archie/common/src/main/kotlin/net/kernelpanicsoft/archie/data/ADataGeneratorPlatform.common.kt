package net.kernelpanicsoft.archie.data

/**
 * Cross-loader datagen switch.
 *
 * Loader implementations resolve this from run configuration system properties.
 */
expect object ADataGeneratorPlatform
{
	val isDataGen: Boolean
}