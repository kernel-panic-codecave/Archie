package net.kernelpanicsoft.archie.gametest

import net.kernelpanicsoft.archie.Archie
import net.kernelpanicsoft.archie.events.gametest.AGametestEvents
import net.kernelpanicsoft.archie.gametest.internal.ArchieGameTest
import net.kernelpanicsoft.archie.gametest.platform.AGameTestPlatform
import net.neoforged.fml.common.Mod

/**
 * NeoForge entrypoint for archie-gametest, registered via the `@Mod` annotation.
 *
 * Activates [ArchieGameTest] directly rather than via `ArchieExtension`/`ServiceLoader` - NeoForge's
 * own per-mod JPMS module boundaries make classic `META-INF/services` discovery unreliable across
 * mods, so each product now triggers its own hooks from its own real entrypoint instead. Also
 * registers [Archie.MOD] with [AGametestEvents] itself, on Archie's behalf - `archie-core` has no
 * gametest entrypoint of its own to do this from, since it doesn't know about gametest concepts.
 */
@Mod("archie_gametest")
object ArchieGameTestNeoForge
{
	init
	{
		if (AGameTestPlatform.isGameTest)
		{
			AGametestEvents += Archie.MOD
			ArchieGameTest.init()
		}
	}
}
