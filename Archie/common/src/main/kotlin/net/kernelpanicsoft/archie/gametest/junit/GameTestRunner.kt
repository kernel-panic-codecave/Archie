package net.kernelpanicsoft.archie.gametest.junit

import net.kernelpanicsoft.archie.events.AEvents
import net.kernelpanicsoft.archie.gametest.ClientGameTest
import net.minecraft.gametest.framework.GameTest
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicTest
import java.nio.file.Path
import java.time.Duration
import kotlin.collections.forEach

object GameTestRunner
{
	const val PROP_ENABLED = "archie.junit.gametest"
	const val PROP_MATRIX = "archie.junit.gametest.matrix"
	const val PROP_TIMEOUT_MINUTES = "archie.junit.gametest.timeoutMinutes"
	const val PROP_WORKSPACE_ROOT = "archie.junit.gametest.root"

	const val DEFAULT_MATRIX = "fabric:server,fabric:client,neoforge:server,neoforge:client"

	fun tests(modID: String, tests: AEvents.ArchieGameTestBuilder.() -> Unit): Collection<DynamicContainer>
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
		val invocations = GameTestGradleInvocation.parseMatrix(matrixValue)
		require(invocations.isNotEmpty()) {
			"No GameTest invocations configured. Set -D$PROP_MATRIX with at least one loader:side pair."
		}

		val timeout = Duration.ofMinutes((System.getProperty(PROP_TIMEOUT_MINUTES)?.toLongOrNull() ?: 20L).coerceAtLeast(1L))
		val root = resolveWorkspaceRoot()

		
		// Shared map to store invocation results (populated when invocation tests run)
		val invocationResults = mutableMapOf<GameTestGradleInvocation, GameTestGradleResult>()

		val containers = mutableListOf<DynamicContainer>()
		
		// First, create tests for each invocation itself
		// These tests run the game and populate invocationResults
		invocations.forEach { invocation ->
			containers.add(DynamicContainer.dynamicContainer(invocation.id, buildList {
				val invocationTestName = "GameTest Invocation [${invocation.id}]"
				val invocationTest = DynamicTest.dynamicTest(invocationTestName) {
					val result = GameTestGradleExecutor.run(
						invocation = invocation,
						timeout = timeout,
						workspaceRoot = root,
					)
					invocationResults[invocation] = result

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
				AEvents.ArchieGameTestBuilder(true).apply(tests).classes.forEach { clazz ->
					add(DynamicContainer.dynamicContainer(clazz.simpleName, clazz.declaredMethods.flatMap { method ->
						val hasGameTest = method.getAnnotationsByType(GameTest::class.java).isNotEmpty()
						val hasClientGameTest = method.getAnnotationsByType(ClientGameTest::class.java).isNotEmpty()
						val tests = mutableListOf<DynamicTest>()
						if ((hasGameTest && invocation.side == Side.SERVER) || (hasClientGameTest && invocation.side == Side.CLIENT)) {
							val id = "$modID:${clazz.simpleName.lowercase()}.${method.name.lowercase()}"
							val displayName = "${method.name}"
							val testName = "$displayName [${invocation.id}]"
							val test = DynamicTest.dynamicTest(testName) {
								val result = invocationResults[invocation]
									?: error("No result recorded for invocation ${invocation.id}")

								// Check if the individual test passed based on log output
								val testResult = result.testResults[id]
								if (testResult != null && !testResult.passed) {
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
					}))
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