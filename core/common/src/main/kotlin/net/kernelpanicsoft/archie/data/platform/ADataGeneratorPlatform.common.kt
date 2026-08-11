package net.kernelpanicsoft.archie.data.platform

/**
 * Cross-loader switch reporting whether the current run is a datagen run.
 *
 * Loader implementations resolve [isDataGen] from run configuration system properties set by
 * the `runDatagen` Gradle tasks.
 */
expect object ADataGeneratorPlatform
{
	val isDataGen: Boolean
}