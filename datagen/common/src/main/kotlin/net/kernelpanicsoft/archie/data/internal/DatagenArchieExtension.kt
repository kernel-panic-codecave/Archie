package net.kernelpanicsoft.archie.data.internal

import net.kernelpanicsoft.archie.ArchieExtension

/** [ArchieExtension] hook that activates [ArchieDatagen] when `archie-datagen` is on the classpath. */
internal class DatagenArchieExtension : ArchieExtension
{
	override fun onDataGen()
	{
		ArchieDatagen.init()
	}
}
