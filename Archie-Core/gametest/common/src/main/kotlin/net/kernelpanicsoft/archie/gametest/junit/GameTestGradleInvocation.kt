package net.kernelpanicsoft.archie.gametest.junit

/** A mod loader [GameTestRunner] can launch a GameTest Gradle task for. */
enum class Loader {
	FABRIC,
	NEOFORGE,
}

/** The GameTest side (matches [net.kernelpanicsoft.archie.gametest.AGameTestSide]) to launch. */
enum class Side {
	SERVER,
	CLIENT,
}

/** One `loader:side` entry from [GameTestRunner]'s matrix, resolving to a single Gradle [taskPath] to run. */
data class GameTestGradleInvocation(
	val loader: Loader,
	val side: Side,
) {
	/** The fully-qualified Gradle task path that launches this invocation, e.g. `:fabric:runGametest`. */
	val taskPath: String
		get() = when (loader) {
			Loader.FABRIC -> when (side) {
				Side.SERVER -> ":fabric:runGametest"
				Side.CLIENT -> ":fabric:runGametestClient"
			}

			Loader.NEOFORGE -> when (side) {
				Side.SERVER -> ":neoforge:runGametest"
				Side.CLIENT -> ":neoforge:runGametestClient"
			}
		}

	/** A short id for this invocation, e.g. `"fabric:server"`, used in test/container display names. */
	val id: String
		get() = "${loader.name.lowercase()}:${side.name.lowercase()}"

	companion object {
		/**
		 * Parses a comma-separated list of `loader:side` tokens (e.g. `"fabric:server,neoforge:client"`)
		 * into invocations, as used by [GameTestRunner.PROP_MATRIX].
		 *
		 * @throws IllegalArgumentException if a token isn't in `loader:side` form or names an unknown [Loader]/[Side].
		 */
		fun parseMatrix(value: String): List<GameTestGradleInvocation> {
			if (value.isBlank()) return emptyList()
			return value.split(',').map { token ->
				val parts = token.trim().split(':')
				require(parts.size == 2) {
					"Invalid matrix token '$token'. Expected format <loader>:<side>, e.g. fabric:server"
				}
				GameTestGradleInvocation(
					loader = Loader.valueOf(parts[0].trim().uppercase()),
					side = Side.valueOf(parts[1].trim().uppercase()),
				)
			}
		}
	}
}

