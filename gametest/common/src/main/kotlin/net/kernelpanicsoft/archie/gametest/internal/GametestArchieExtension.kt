package net.kernelpanicsoft.archie.gametest.internal

import net.kernelpanicsoft.archie.ArchieExtension

/** [ArchieExtension] hook that activates [ArchieGameTest] when `archie-gametest` is on the classpath. */
internal class GametestArchieExtension : ArchieExtension
{
	override fun onGameTest()
	{
		ArchieGameTest.init()
	}
}
