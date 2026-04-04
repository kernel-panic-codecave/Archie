package net.kernelpanicsoft.archie.gametest.junit

enum class Loader {
	FABRIC,
	NEOFORGE,
}

enum class Side {
	SERVER,
	CLIENT,
}

data class GameTestGradleInvocation(
	val loader: Loader,
	val side: Side,
) {
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

	val id: String
		get() = "${loader.name.lowercase()}:${side.name.lowercase()}"

	companion object {
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

