package net.kernelpanicsoft.archie.data

import net.kernelpanicsoft.archie.Archie
import net.kernelpanicsoft.archie.data.internal.ArchieDatagen
import net.kernelpanicsoft.archie.data.platform.ADataGeneratorPlatform
import net.kernelpanicsoft.archie.events.datagen.ADatagenEvents
import net.neoforged.fml.common.Mod

/**
 * NeoForge entrypoint for archie-datagen, registered via the `@Mod` annotation.
 *
 * Activates [ArchieDatagen] directly rather than via `ArchieExtension`/`ServiceLoader` - NeoForge's
 * own per-mod JPMS module boundaries make classic `META-INF/services` discovery unreliable across
 * mods, so each product now triggers its own hooks from its own real entrypoint instead. Also
 * registers [Archie.MOD] with [ADatagenEvents] itself, on Archie's behalf - `archie-core` has no
 * datagen entrypoint of its own to do this from, since it doesn't know about datagen concepts.
 */
@Mod("archie_datagen")
object ArchieDatagenNeoForge
{
	init
	{
		if (ADataGeneratorPlatform.isDataGen)
		{
			ADatagenEvents += Archie.MOD
			ArchieDatagen.init()
		}
	}
}
