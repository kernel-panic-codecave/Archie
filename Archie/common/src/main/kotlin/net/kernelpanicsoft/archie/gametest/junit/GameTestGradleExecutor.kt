package net.kernelpanicsoft.archie.gametest.junit

import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.io.path.appendText
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.outputStream
import kotlin.io.path.readLines

/** One test's pass/fail outcome, parsed from a GameTest invocation's log output. */
internal data class TestResult(
	val testId: String,
	val passed: Boolean,
)

/** The outcome of one [GameTestGradleExecutor.start] invocation, including per-test results parsed from its log. */
internal data class GameTestGradleResult(
	val success: Boolean,
	val command: List<String>,
	val exitCode: Int,
	val logFile: Path,
	val logTail: String,
	val testResults: Map<String, TestResult> = emptyMap(), // testId -> TestResult
)

/**
 * A [GameTestGradleInvocation]'s Gradle process, already running by the time this is returned.
 * `liveTestResults` is populated live as PASS/FAIL lines are parsed from the process's output, so
 * a caller can report an individual test as soon as it's known instead of waiting for [result]
 * (the invocation's overall outcome) to complete.
 */
internal class GameTestGradleHandle(
	val invocation: GameTestGradleInvocation,
	private val liveTestResults: ConcurrentHashMap<String, TestResult>,
	val result: CompletableFuture<GameTestGradleResult>,
) {
	/**
	 * Blocks until [testId] has been observed in the live log output, or [result] completes -
	 * whichever happens first. Returns `null` if the invocation finished without ever reporting
	 * a result for [testId] (e.g. it wasn't reached before a crash/timeout).
	 */
	fun awaitTestResult(testId: String, pollInterval: Duration = Duration.ofMillis(200)): TestResult? {
		while (!result.isDone) {
			liveTestResults[testId]?.let { return it }
			Thread.sleep(pollInterval.toMillis())
		}
		return liveTestResults[testId]
	}
}

/** Shells out to the Gradle wrapper to run one [GameTestGradleInvocation], capturing and parsing its log output. */
internal object GameTestGradleExecutor {
	/** Backs every in-flight invocation's blocking wait-for-exit; sized generously since these are I/O-bound, not CPU-bound. */
	private val ioExecutor = Executors.newCachedThreadPool { runnable ->
		Thread(runnable, "archie-gametest-runner").apply { isDaemon = true }
	}

	/**
	 * Every invocation is a separate --no-daemon Gradle process, and (at least for Archie-Test)
	 * they all depend on the same upstream composite-build artifact (e.g. Archie:common's
	 * remapped jar) - starting all of them at once races multiple processes rebuilding/rewriting
	 * that shared output concurrently, corrupting it (observed: `:Archie:common:remapJar FAILED
	 * ... ZipException: invalid stored block lengths`). Whichever invocation calls [start] first
	 * claims [primingClaimed] and runs a single, fast `assemble` build to force those shared
	 * outputs to exist once; everyone else blocks on [primingComplete] until that finishes. Once
	 * it's done, Gradle's up-to-date checks mean every invocation's own process only reads the
	 * already-built artifact, so all of them - including the priming one - start their real
	 * (long-running) invocation concurrently right after, instead of one invocation blocking
	 * every other one on its entire run.
	 */
	private val primingClaimed = AtomicBoolean(false)
	private val primingComplete = CompletableFuture<Void>()

	/**
	 * Starts [invocation]'s Gradle task via `ProcessBuilder` and returns immediately with a
	 * [GameTestGradleHandle] tracking it - the process itself, and the background threads
	 * streaming its output, are already running. This lets a caller [start] every matrix entry
	 * up front so independent invocations run concurrently, instead of only starting the next
	 * one once a prior invocation's JUnit node happens to execute.
	 */
	fun start(
		invocation: GameTestGradleInvocation,
		timeout: Duration,
		workspaceRoot: Path,
	): GameTestGradleHandle {
		if (primingClaimed.compareAndSet(false, true)) {
			runCatching { primeSharedBuildOutputs(workspaceRoot) }
				.onSuccess { primingComplete.complete(null) }
				.onFailure { primingComplete.completeExceptionally(it) }
				.getOrThrow()
		} else {
			try {
				primingComplete.join()
			} catch (e: CompletionException) {
				throw IllegalStateException("Shared build output priming failed; see cause", e.cause ?: e)
			}
		}

		return startProcess(invocation, timeout, workspaceRoot)
	}

	/**
	 * Runs a single, fast `assemble` (compiles and packages every subproject, including
	 * composite-included ones, without launching anything) so the shared upstream artifacts every
	 * matrix invocation depends on exist before any of them starts its own (much longer) process.
	 * Deliberately generic (not a hardcoded task path) so it works the same for both Archie's and
	 * Archie-Test's workspace roots.
	 */
	private fun primeSharedBuildOutputs(workspaceRoot: Path) {
		val logsDir = workspaceRoot.resolve("build/tmp/junit-gametest-runner").createDirectories()
		val logFile = logsDir.resolve("priming.log")

		val wrapper = resolveGradleWrapper(workspaceRoot)
		val command = listOf(wrapper.toString(), "assemble", "--console=plain", "--no-daemon")

		val process = ProcessBuilder(command)
			.directory(workspaceRoot.toFile())
			.redirectErrorStream(true)
			.start()

		logFile.outputStream().bufferedWriter().use { writer ->
			process.inputStream.bufferedReader().useLines { lines ->
				lines.forEach { line ->
					println(line)
					writer.appendLine(line)
				}
			}
		}

		val exitCode = process.waitFor()
		check(exitCode == 0) {
			"Priming build ('${command.joinToString(" ")}') failed with exit code $exitCode. Log file: $logFile\n--- Log tail ---\n${tail(logFile)}"
		}
	}

	private fun startProcess(
		invocation: GameTestGradleInvocation,
		timeout: Duration,
		workspaceRoot: Path,
	): GameTestGradleHandle {
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

		val liveTestResults = ConcurrentHashMap<String, TestResult>()
		val testPattern = when (invocation.side) {
			Side.SERVER -> Regex("""\[GameTest] (PASS|FAIL) (.+)""")
			Side.CLIENT -> Regex("""\[ClientGameTest] (PASS|FAIL) (.+)""")
		}

		val outputPump = Thread {
			logFile.outputStream().bufferedWriter().use { writer ->
				process.inputStream.bufferedReader().useLines { lines ->
					lines.forEach { line ->
						println(line)
						writer.appendLine(line)
						writer.flush()

						val match = testPattern.find(line)
						if (match != null) {
							val passed = match.groupValues[1] == "PASS"
							val testId = match.groupValues[2].trim()
							liveTestResults[testId] = TestResult(testId, passed)
						}
					}
				}
			}
		}.apply {
			name = "archie-gametest-output-${invocation.id}"
			isDaemon = true
			start()
		}

		val result = CompletableFuture.supplyAsync(
			{ awaitCompletion(process, outputPump, timeout, logFile, command, liveTestResults) },
			ioExecutor,
		)

		return GameTestGradleHandle(invocation, liveTestResults, result)
	}

	private fun awaitCompletion(
		process: Process,
		outputPump: Thread,
		timeout: Duration,
		logFile: Path,
		command: List<String>,
		liveTestResults: Map<String, TestResult>,
	): GameTestGradleResult {
		val finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)
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
		return GameTestGradleResult(
			success = finished && exitCode == 0,
			command = command,
			exitCode = exitCode,
			logFile = logFile,
			logTail = tail,
			testResults = liveTestResults.toMap(),
		)
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
