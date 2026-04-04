package net.kernelpanicsoft.archie.gametest.runner

import java.nio.file.Path
import java.time.Duration
import kotlin.io.path.appendText
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.outputStream
import kotlin.io.path.readLines

internal data class TestResult(
	val testId: String,
	val passed: Boolean,
)

internal data class GameTestGradleResult(
	val success: Boolean,
	val command: List<String>,
	val exitCode: Int,
	val logFile: Path,
	val logTail: String,
	val testResults: Map<String, TestResult> = emptyMap(), // testId -> TestResult
)

internal object GameTestGradleExecutor {
	fun run(
		invocation: GameTestGradleInvocation,
		timeout: Duration,
		workspaceRoot: Path,
	): GameTestGradleResult {
		val logsDir = workspaceRoot.resolve("build/tmp/junit-gametest-runner").createDirectories()
		val logFile = logsDir.resolve("${invocation.id}.log")

		val wrapper = resolveGradleWrapper(workspaceRoot)
		val command = mutableListOf(wrapper.toString())
		command += invocation.taskPath
		command += "--console=plain"
		command += "--no-daemon"

		val extraArgs = System.getProperty("archie.junit.gametest.extraArgs")?.trim().orEmpty()
		if (extraArgs.isNotEmpty()) {
			command.addAll(extraArgs.split(Regex("\\s+")))
		}

		val process = ProcessBuilder(command)
			.directory(workspaceRoot.toFile())
			.redirectErrorStream(true)
			.start()

		val outputPump = Thread {
			logFile.outputStream().bufferedWriter().use { writer ->
				process.inputStream.bufferedReader().useLines { lines ->
					lines.forEach { line ->
						println(line)
						writer.appendLine(line)
						writer.flush()
					}
				}
			}
		}.apply {
			name = "archie-gametest-output-${invocation.id}"
			isDaemon = true
			start()
		}

		val finished = process.waitFor(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS)
		val exitCode = if (finished) process.exitValue() else {
			process.destroyForcibly()
			process.waitFor()
			-1
		}

		outputPump.join(5_000)

		if (!finished) {
			logFile.appendText("\n[runner] Timed out after ${timeout.toMinutes()} minute(s).\n")
		}

		val tail = tail(logFile)
		val testResults = parseTestResults(logFile, invocation.side)
		return GameTestGradleResult(
			success = finished && exitCode == 0,
			command = command,
			exitCode = exitCode,
			logFile = logFile,
			logTail = tail,
			testResults = testResults,
		)
	}

	private fun parseTestResults(logFile: Path, side: Side): Map<String, TestResult> {
		if (!logFile.exists()) return emptyMap()

		val results = mutableMapOf<String, TestResult>()
		val lines = logFile.readLines()
		val testPattern = when (side) {
			Side.SERVER -> Regex("""\[(GameTest|.*?)] (PASS|FAIL) (.+)""")
			Side.CLIENT -> Regex("""\[ClientGameTest] (PASS|FAIL) (.+)""")
		}

		lines.forEach { line ->
			val match = testPattern.find(line)
			if (match != null) {
				val (passFailGroup, testIdGroup) = when (side) {
					Side.SERVER -> match.groupValues[2] to match.groupValues[3]
					Side.CLIENT -> match.groupValues[1] to match.groupValues[2]
				}
				val testId = testIdGroup.trim()
				val passed = passFailGroup == "PASS"
				results[testId] = TestResult(testId, passed)
			}
		}

		return results
	}

	private fun resolveGradleWrapper(workspaceRoot: Path): Path {
		val unix = workspaceRoot.resolve("gradlew")
		if (unix.exists()) return unix

		val windows = workspaceRoot.resolve("gradlew.bat")
		if (windows.exists()) return windows

		error("Could not locate Gradle wrapper in $workspaceRoot")
	}

	private fun tail(file: Path): String {
		if (!file.exists()) return "<missing log: $file>"
		val lines = file.readLines()
		return lines.takeLast(120).joinToString("\n")
	}
}








