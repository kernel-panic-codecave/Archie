package net.kernelpanicsoft.archie.gametest.junit

/** A mod loader [GameTestRunner] can launch a GameTest Gradle task for. */
enum class Loader {
	FABRIC,
	NEOFORGE,
}

/** The GameTest side (matches [net.kernelpanicsoft.archie.gametest.platform.AGameTestSide]) to launch. */
enum class Side {
	SERVER,
	CLIENT,
}

/**
 * One `loader:side` entry from [GameTestRunner]'s matrix, resolving to a single Gradle [taskPath]
 * to run.
 *
 * Two project layouts exist across the products that use this harness, and [version] is what picks
 * between them:
 *
 * - **Flat** ([version] `null`) - one Gradle project per loader, named `<prefix>-<loader>`. What
 *   Boilerplate uses: `:boilerplate-fabric:runGametest`, with [projectPrefix] `"boilerplate"`.
 * - **Nested** ([version] set) - a Stonecutter tree per product, a project per loader inside it, and
 *   a project per Minecraft version inside *that*. What Archie itself now uses:
 *   `:gametest:fabric:1.21.1:runGametest`, with [projectPrefix] `"gametest"`.
 *
 * The version is part of the address rather than a detail of the task's configuration: there is no
 * alias for it on the branch project, so `:gametest:fabric:runGametest` does not exist and only
 * `:gametest:fabric:1.21.1:runGametest` does. It is supplied by the `test` task from Stonecutter's
 * active version - see [GameTestRunner.PROP_VERSION] - and its absence *is* the flat layout, so a
 * product that never sets it keeps the paths it always had.
 *
 * Archie's own move to the nested layout is what forced this: the repo root became a single Gradle
 * build shared by every product rather than one workspace root per product, `:archie-gametest-fabric`
 * stopped existing, and every launch failed identically with "project not found".
 */
data class GameTestGradleInvocation(
	val loader: Loader,
	val side: Side,
	val projectPrefix: String,
	val version: String? = null,
) {
	/** The fully-qualified Gradle task path that launches this invocation, e.g. `:gametest:fabric:1.21.1:runGametest` or `:boilerplate-fabric:runGametest`. */
	val taskPath: String
		get() {
			val loaderName = loader.name.lowercase()
			val project = if (version == null) ":$projectPrefix-$loaderName" else ":$projectPrefix:$loaderName:$version"
			return when (side) {
				Side.SERVER -> "$project:runGametest"
				Side.CLIENT -> "$project:runGametestClient"
			}
		}

	/** A short id for this invocation, e.g. `"fabric:server"`, used in test/container display names. */
	val id: String
		get() = "${loader.name.lowercase()}:${side.name.lowercase()}"

	companion object {
		/**
		 * Parses a comma-separated list of `loader:side` tokens (e.g. `"fabric:server,neoforge:client"`)
		 * into invocations targeting [projectPrefix] at [version], as used by [GameTestRunner.PROP_MATRIX].
		 *
		 * @throws IllegalArgumentException if a token isn't in `loader:side` form or names an unknown [Loader]/[Side].
		 */
		fun parseMatrix(value: String, projectPrefix: String, version: String? = null): List<GameTestGradleInvocation> {
			if (value.isBlank()) return emptyList()
			return value.split(',').map { token ->
				val parts = token.trim().split(':')
				require(parts.size == 2) {
					"Invalid matrix token '$token'. Expected format <loader>:<side>, e.g. fabric:server"
				}
				GameTestGradleInvocation(
					loader = Loader.valueOf(parts[0].trim().uppercase()),
					side = Side.valueOf(parts[1].trim().uppercase()),
					projectPrefix = projectPrefix,
					version = version,
				)
			}
		}
	}
}

