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
 * to run. [projectPrefix] is which product's Gradle projects to target - e.g. `"archie-gametest"`
 * for `archie-gametest`'s own self-test suite, `"archie-test"` for the playground mod's - since
 * the repo root is now a single Gradle build shared by every product, not one workspace root per
 * product the way it used to be.
 */
data class GameTestGradleInvocation(
	val loader: Loader,
	val side: Side,
	val projectPrefix: String,
) {
	/** The fully-qualified Gradle task path that launches this invocation, e.g. `:archie-gametest-fabric:runGametest`. */
	val taskPath: String
		get() {
			val project = "$projectPrefix-${loader.name.lowercase()}"
			return when (side) {
				Side.SERVER -> ":$project:runGametest"
				Side.CLIENT -> ":$project:runGametestClient"
			}
		}

	/** A short id for this invocation, e.g. `"fabric:server"`, used in test/container display names. */
	val id: String
		get() = "${loader.name.lowercase()}:${side.name.lowercase()}"

	companion object {
		/**
		 * Parses a comma-separated list of `loader:side` tokens (e.g. `"fabric:server,neoforge:client"`)
		 * into invocations targeting [projectPrefix], as used by [GameTestRunner.PROP_MATRIX].
		 *
		 * @throws IllegalArgumentException if a token isn't in `loader:side` form or names an unknown [Loader]/[Side].
		 */
		fun parseMatrix(value: String, projectPrefix: String): List<GameTestGradleInvocation> {
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
				)
			}
		}
	}
}

