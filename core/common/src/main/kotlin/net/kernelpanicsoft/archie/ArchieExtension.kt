package net.kernelpanicsoft.archie

/**
 * Extension point for modules that hook into a dedicated datagen/gametest run without `archie-core`
 * needing a compile-time dependency on them. `archie-datagen`/`archie-gametest` each register an
 * implementation via `META-INF/services/net.kernelpanicsoft.archie.ArchieExtension`
 * ([java.util.ServiceLoader]); [Archie.init] invokes whichever hook applies, and does nothing if
 * neither module is on the classpath (the normal case for a production build).
 */
interface ArchieExtension
{
	/** Called from [Archie.init] when running under a datagen task ([net.kernelpanicsoft.archie.data.ADataGeneratorPlatform.isDataGen]). */
	fun onDataGen()
	{
	}

	/** Called from [Archie.init] when running under a GameTest task ([net.kernelpanicsoft.archie.gametest.AGameTestPlatform.isGameTest]). */
	fun onGameTest()
	{
	}
}
