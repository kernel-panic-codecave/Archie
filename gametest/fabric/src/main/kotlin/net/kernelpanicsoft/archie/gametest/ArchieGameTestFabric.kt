package net.kernelpanicsoft.archie.gametest

import net.fabricmc.api.ModInitializer
import net.kernelpanicsoft.archie.Archie
import net.kernelpanicsoft.archie.events.gametest.AGametestEvents
import net.kernelpanicsoft.archie.gametest.internal.ArchieGameTest
import net.kernelpanicsoft.archie.gametest.platform.AGameTestPlatform

/**
 * Fabric entrypoint for archie-gametest (`fabric.mod.json` `main`).
 *
 * Activates [ArchieGameTest] directly rather than via `ArchieExtension`/`ServiceLoader` - NeoForge's
 * own per-mod JPMS module boundaries make classic `META-INF/services` discovery unreliable across
 * mods, so each product now triggers its own hooks from its own real entrypoint instead. Also
 * registers [Archie.MOD] with [AGametestEvents] itself, on Archie's behalf - `archie-core` has no
 * gametest entrypoint of its own to do this from, since it doesn't know about gametest concepts.
 */
object ArchieGameTestFabric : ModInitializer
{
	override fun onInitialize()
	{
		if (!AGameTestPlatform.isGameTest) return
		AGametestEvents += Archie.MOD
		ArchieGameTest.init()
	}
}
