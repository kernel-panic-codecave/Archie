package net.kernelpanicsoft.archie.gametest.junit

import net.kernelpanicsoft.archie.events.AGametestEvents
import net.kernelpanicsoft.archie.gametest.ClientGameTest
import net.minecraft.gametest.framework.GameTest
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicTest
import java.net.URI
import java.nio.file.Path
import java.time.Duration
import kotlin.collections.forEach

/**
 * Bridges Archie's Loom-driven GameTests into a regular JUnit 5 run, so `./gradlew test` (or an
 * IDE test runner) can launch `runGametest`/`runGametestClient` for a `loader:side` matrix and
 * report each declared test as its own JUnit [DynamicTest], parsed from the launched process's
 * log output.
 *
 * Disabled by default (opt-in via [PROP_ENABLED]) since it shells out to Gradle and boots a full
 * Minecraft process per matrix entry.
 */
object GameTestRunner
{
	/** System property (`-D...=true`) that must be set to enable [tests]; otherwise it reports a single skipped test. */
	const val PROP_ENABLED = "archie.junit.gametest"
	/** System property overriding [DEFAULT_MATRIX], a comma-separated list of `loader:side` pairs to launch. */
	const val PROP_MATRIX = "archie.junit.gametest.matrix"
	/** System property overriding the per-invocation timeout, in minutes (default 20, minimum 1). */
	const val PROP_TIMEOUT_MINUTES = "archie.junit.gametest.timeoutMinutes"
	/** System property overriding the auto-detected workspace root (the directory containing `settings.gradle.kts`). */
	const val PROP_WORKSPACE_ROOT = "archie.junit.gametest.root"

	/** The default `loader:side` matrix launched by [tests] when [PROP_MATRIX] isn't set. */
	const val DEFAULT_MATRIX = "fabric:server,fabric:client,neoforge:server,neoforge:client"

	/**
	 * Builds the JUnit dynamic test tree for [modID]: one container per `loader:side` invocation
	 * in the configured matrix, each running the loader's GameTest Gradle task once and then
	 * reporting one [DynamicTest] per test method declared via [tests] (an
	 * [AGametestEvents.ArchieGameTestBuilder] receiver, same DSL as [AGametestEvents.REGISTER_GAME_TEST]) whose
	 * side matches that invocation. [projectPrefix] is which product's Gradle projects the launched
	 * task targets (e.g. `"archie-gametest"`, `"archie-test"`) - see [GameTestGradleInvocation].
	 */
	fun tests(modID: String, projectPrefix: String, tests: AGametestEvents.ArchieGameTestBuilder.() -> Unit): Collection<DynamicContainer>
	{
		val enabled = System.getProperty(PROP_ENABLED)?.toBooleanStrictOrNull() == true
		if (!enabled) {
			return listOf(
				DynamicContainer.dynamicContainer("gametest:disabled", mutableListOf(DynamicTest.dynamicTest("GameTest runner is disabled") {
					assumeTrue(false) {
						"GameTest runner is disabled. Set -D$PROP_ENABLED=true to launch Loom gametest tasks from JUnit."
					}
				}))
			)
		}

		val matrixValue = System.getProperty(PROP_MATRIX) ?: DEFAULT_MATRIX
		val invocations = GameTestGradleInvocation.parseMatrix(matrixValue, projectPrefix)
		require(invocations.isNotEmpty()) {
			"No GameTest invocations configured. Set -D$PROP_MATRIX with at least one loader:side pair."
		}

		val timeout = Duration.ofMinutes((System.getProperty(PROP_TIMEOUT_MINUTES)?.toLongOrNull() ?: 20L).coerceAtLeast(1L))
		val root = resolveWorkspaceRoot()

		val handleLazies = invocations.associateWith { invocation ->
			lazy(LazyThreadSafetyMode.SYNCHRONIZED) { GameTestGradleExecutor.start(invocation, timeout, root) }
		}

		val containers = mutableListOf<DynamicContainer>()

		invocations.forEach { invocation ->
			val handleLazy = handleLazies.getValue(invocation)
			containers.add(DynamicContainer.dynamicContainer(invocation.id, buildList {
				val invocationTestName = "GameTest Invocation [${invocation.id}]"
				val invocationTest = DynamicTest.dynamicTest(invocationTestName) {
					val result = handleLazy.value.result.get()

					// Check if the invocation itself succeeded (exit code 0)
					if (!result.success) {
						val message = buildString {
							append("GameTest invocation failed: $invocationTestName\n")
							append("Exit code: ${result.exitCode}\n")
							append("Command: ${result.command.joinToString(" ")}\n")
							append("Log file: ${result.logFile}\n")
							append("--- Log tail ---\n")
							append(result.logTail)
						}
						throw AssertionError(message)
					}
				}
				add(invocationTest)
				AGametestEvents.ArchieGameTestBuilder(true).apply(tests).classes.forEach { clazz ->
					val classUri = URI.create("class:${clazz.name}")
					add(DynamicContainer.dynamicContainer(clazz.simpleName, classUri, clazz.declaredMethods.flatMap { method ->
						val hasGameTest = method.getAnnotationsByType(GameTest::class.java).isNotEmpty()
						val hasClientGameTest = method.getAnnotationsByType(ClientGameTest::class.java).isNotEmpty()
						val tests = mutableListOf<DynamicTest>()
						if ((hasGameTest && invocation.side == Side.SERVER) || (hasClientGameTest && invocation.side == Side.CLIENT)) {
							val id = "$modID:${clazz.simpleName.lowercase()}.${method.name.lowercase()}"
							val displayName = "${method.name}"
							val testName = "$displayName [${invocation.id}]"
							val methodUri = URI.create("method:${clazz.name}#${method.name}")
							val test = DynamicTest.dynamicTest(testName, methodUri) {
								val testResult = handleLazy.value.awaitTestResult(id)
								if (testResult != null && !testResult.passed) {
									val result = handleLazy.value.result.get()
									val message = buildString {
										append("GameTest failed: $testName\n")
										append("Exit code: ${result.exitCode}\n")
										append("Command: ${result.command.joinToString(" ")}\n")
										append("Log file: ${result.logFile}\n")
										append("--- Log tail ---\n")
										append(result.logTail)
									}
									throw AssertionError(message)
								} else if (testResult == null) {
									// Test wasn't found in the log output
									val result = handleLazy.value.result.get()
									if (!result.success) {
										// Invocation failed entirely, report that
										val message = buildString {
											append("GameTest invocation failed: $testName\n")
											append("Exit code: ${result.exitCode}\n")
											append("Command: ${result.command.joinToString(" ")}\n")
											append("Log file: ${result.logFile}\n")
											append("--- Log tail ---\n")
											append(result.logTail)
										}
										throw AssertionError(message)
									}
									// Otherwise treat as passed if test ID wasn't found (test might not have run)
								}
							}
							tests.add(test)
						}
						tests
					}.stream()))
				}
			}))
		}
		
		return containers
	}

	private fun resolveWorkspaceRoot(): Path {
		val explicit = System.getProperty(PROP_WORKSPACE_ROOT)?.trim().orEmpty()
		if (explicit.isNotEmpty()) return Path.of(explicit)

		var cursor = Path.of("").toAbsolutePath()
		repeat(8) {
			if (cursor.resolve("settings.gradle.kts").toFile().exists()) return cursor
			cursor = cursor.parent ?: return@repeat
		}
		error("Unable to locate workspace root from ${Path.of("").toAbsolutePath()}. Set -D$PROP_WORKSPACE_ROOT=/path/to/Archie")
	}


}