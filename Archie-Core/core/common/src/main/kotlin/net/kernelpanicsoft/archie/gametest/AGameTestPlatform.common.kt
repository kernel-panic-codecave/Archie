package net.kernelpanicsoft.archie.gametest

import dev.architectury.platform.Mod
import dev.architectury.utils.Env

/** Logical side used while collecting/running GameTests. */
enum class AGameTestSide {
	SERVER,
	CLIENT,
}

internal fun AGameTestSide.toEnv(): Env = when (this) {
	AGameTestSide.SERVER -> Env.SERVER
	AGameTestSide.CLIENT -> Env.CLIENT
}

/**
 * Cross-loader GameTest integration point.
 *
 * Implementations detect whether GameTest mode is active and collect test classes per mod.
 */
expect object AGameTestPlatform
{
	/** `true` when the current process is running under a GameTest task (`runGametest`/`runGametestClient`). */
	val isGameTest: Boolean

	/** Active logical side for this GameTest run (supports launcher/property overrides). */
	val side: AGameTestSide?

	/** Register a test class for [mod] when GameTest bootstrapping occurs. */
	fun register(clazz: Class<*>, mod: Mod)
}

/**
 * Restricts which [AEvents.REGISTER_GAME_TEST]-registered mods a single `runGametest`/
 * `runGametestClient` invocation actually runs, via the [GAMETEST_MOD_ID_FILTER_PROPERTY]
 * system property (a comma-separated list of mod ids).
 *
 * Every mod that has called `AEvents += MOD` shares one JVM-wide [AEvents.MODS] list - which
 * matters because a composite build's included builds can *both* end up in that list within
 * the same process. Archie-Test's Loom `runs{}` blocks `includeBuild("../Archie")`, and both
 * `Archie` and `ArchieTest`'s mod init call `AEvents += MOD`, so launching Archie-Test's own
 * `runGametestClient`/`runGametest` previously ran Archie's *entire* GameTest suite a second
 * time in the same process, without Archie-Test's own suite being any bigger - only
 * distinguishable by the test count not matching the log's actual line count. Every loader's
 * `AGameTestPlatformInternal`/`AClientGameTestHarness` server- and client-side test collection
 * should call [selectMods] on [AEvents.MODS] before iterating, instead of iterating it directly.
 */
object AGameTestModFilter {
	private const val GAMETEST_MOD_ID_FILTER_PROPERTY = "archie.gametest.modid"

	/**
	 * Filters [mods] down to just the ones named in [GAMETEST_MOD_ID_FILTER_PROPERTY], or returns
	 * [mods] unchanged if that property isn't set. Each project's own Loom `runs{}` block should
	 * set this to its own `mod_id` gradle property on its `gametest`/`gametestClient` runs.
	 *
	 * @throws IllegalStateException if the property is set but names no mod present in [mods].
	 */
	fun selectMods(mods: Collection<Mod>): List<Mod> {
		val filter = System.getProperty(GAMETEST_MOD_ID_FILTER_PROPERTY)?.trim().orEmpty()
		if (filter.isEmpty()) return mods.toList()

		val requestedIds = filter.split(',')
			.map { it.trim() }
			.filter { it.isNotEmpty() }
			.toSet()

		check(requestedIds.isNotEmpty()) {
			"No valid mod IDs specified in gametest filter '$GAMETEST_MOD_ID_FILTER_PROPERTY'"
		}

		val selected = mods.filter { it.modId in requestedIds }
		check(selected.isNotEmpty()) {
			"No gametests found for requested mod IDs: ${requestedIds.joinToString(",")} (available: ${mods.joinToString(",") { it.modId }})"
		}
		return selected
	}
}
