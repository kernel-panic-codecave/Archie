package net.kernelpanicsoft.archie.gametest

import com.mojang.realmsclient.RealmsMainScreen
import dev.architectury.platform.Mod
import net.kernelpanicsoft.archie.Archie
import net.minecraft.SharedConstants
import net.minecraft.client.Minecraft
import net.minecraft.client.Screenshot
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.screens.GenericMessageScreen
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.TitleScreen
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState
import net.minecraft.core.Holder
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.contents.TranslatableContents
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.levelgen.presets.WorldPreset
import net.minecraft.world.level.levelgen.presets.WorldPresets
import org.apache.commons.lang3.function.FailableConsumer
import org.apache.commons.lang3.function.FailableFunction
import org.joml.Vector2i
import org.lwjgl.glfw.GLFW
import java.nio.file.Path
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer
import java.util.function.Predicate
import java.util.function.Supplier
import java.lang.reflect.Modifier
import java.nio.file.Files
import kotlin.reflect.full.primaryConstructor

private const val DEFAULT_TICK_MILLIS = 50L
private const val CLIENT_EXEC_TIMEOUT_SECONDS = 10L
private const val WORLD_BUILDER_EXEC_TIMEOUT_SECONDS = 300L
private const val SCREEN_SET_TIMEOUT_TICKS = 40
private const val CLIENT_GAMETEST_MOD_ID_FILTER_PROPERTY = "fabric.client.gametest.modid"

private object DedicatedServerLifecycleTracker {
    private val activeServers = ConcurrentHashMap.newKeySet<Any>()

    fun register(server: Any) {
        activeServers += server
    }

    fun unregister(server: Any) {
        activeServers -= server
    }

    fun stopAllLeakedServers() {
        activeServers.toList().forEach { server ->
            runCatching {
                ADedicatedServerPlatform.stop(server)
            }.onFailure { error ->
                Archie.LOGGER.warn("Failed stopping leaked dedicated server: ${error.message}")
            }
        }

        val deadlineNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
        while (System.nanoTime() < deadlineNanos) {
            val stillAlive = activeServers.filter { server ->
                runCatching { ADedicatedServerPlatform.isAlive(server) }.getOrDefault(false)
            }
            if (stillAlive.isEmpty()) break
            Thread.sleep(50L)
        }

        activeServers.removeIf { server ->
            runCatching { !ADedicatedServerPlatform.isAlive(server) }.getOrDefault(true)
        }
    }
}

/** Marks a test method to be executed by the Archie client GameTest backport harness. */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ClientGameTest(val name: String = "")

data class TestScreenshotOptions(
    val name: String,
)

data class TestScreenshotComparisonOptions(
    val templateImage: String,
    val screenshot: TestScreenshotOptions = TestScreenshotOptions(name = "client-gametest"),
)

data class AClientGameTestFailure(
    val testId: String,
    val rootCause: String,
)

interface TestInput {
    fun click(x: Double, y: Double, button: Int = 0)
    fun keyPress(keyCode: Int, scanCode: Int = 0, modifiers: Int = 0)
    fun holdKey(keyCode: Int, scanCode: Int = 0, modifiers: Int = 0)
    fun releaseKey(keyCode: Int, scanCode: Int = 0, modifiers: Int = 0)
    fun pressKey(keyCode: Int, scanCode: Int = 0, modifiers: Int = 0)
    fun holdMouse(button: Int = 0)
    fun releaseMouse(button: Int = 0)
    fun pressMouse(button: Int = 0)
    fun holdControl()
    fun releaseControl()
    fun holdShift()
    fun releaseShift()
    fun holdAlt()
    fun releaseAlt()
    fun charTyped(char: Char, modifiers: Int = 0)
    fun typeChars(value: String)
    fun scroll(x: Double = 0.0, y: Double = 1.0)
    fun setCursor(x: Double, y: Double)
    fun moveCursor(deltaX: Double, deltaY: Double)
    fun clearInputs()
}

@Suppress("unused")
interface TestWorldBuilder {
    fun setUseConsistentSettings(useConsistentSettings: Boolean): TestWorldBuilder

    fun adjustSettings(settingsAdjuster: Consumer<WorldCreationUiState>): TestWorldBuilder

    fun create(): TestSingleplayerContext

    fun withSingleplayer(callback: TestSingleplayerContext.() -> Unit) {
        val context = create()
        try {
            context.callback()
        } finally {
            context.close()
        }
    }

    fun createServer(serverProperties: Properties): TestDedicatedServerContext

    fun withServer(serverProperties: Properties, callback: TestDedicatedServerContext.() -> Unit) {
        val context = createServer(serverProperties)
        try {
            context.callback()
        } finally {
            context.close()
        }
    }
}

@Suppress("unused")
interface TestSingleplayerContext {
    val clientContext: ClientGameTestContext
    val saveDirectory: Path
    val clientWorld: TestClientWorldContext
    val server: TestServerContext

    fun close()
}

@Suppress("unused")
interface TestDedicatedServerContext {
    val clientContext: ClientGameTestContext

    val serverDirectory: Path

    fun connect(): TestServerConnection

    fun withConnection(callback: TestServerConnection.() -> Unit) {
        val connection = connect()
        try {
            connection.callback()
        } finally {
            runCatching { connection.disconnect() }
                .onFailure { error ->
                    Archie.LOGGER.warn("Failed to disconnect server connection cleanly: ${error.message}")
                }
        }
    }

    fun close()
}

@Suppress("unused")
interface TestServerConnection {
    val clientContext: ClientGameTestContext
    val clientWorld: TestClientWorldContext

    fun disconnect()
}

@Suppress("unused")
interface TestClientWorldContext {
    fun waitForChunksDownload(timeout: Int = ClientGameTestContext.DEFAULT_TIMEOUT): Int

    fun waitForChunksRender(waitForDownload: Boolean = true, timeout: Int = ClientGameTestContext.DEFAULT_TIMEOUT): Int
}

@Suppress("unused")
interface TestServerContext {
    fun runCommand(command: String)

    fun <E : Throwable> runOnServer(action: FailableConsumer<MinecraftServer, E>)

    fun <T, E : Throwable> computeOnServer(function: FailableFunction<MinecraftServer, T, E>): T
}

/** Minimal assertion/report API passed to client harness test methods. */
@Suppress("unused")
interface ClientGameTestContext {
    val testId: String

    companion object {
        const val NO_TIMEOUT: Int = -1
        const val DEFAULT_TIMEOUT: Int = 10 * SharedConstants.TICKS_PER_SECOND
    }

    fun assertTrue(condition: Boolean, message: () -> String)

    fun assertEquals(expected: Any?, actual: Any?, message: () -> String = { "Expected <$expected>, got <$actual>" })

    fun fail(message: String): Nothing

    fun assertScreenshotContains(templateImage: String): Vector2i =
        assertScreenshotContains(TestScreenshotComparisonOptions(templateImage = templateImage))

    fun assertScreenshotContains(options: TestScreenshotComparisonOptions): Vector2i

    fun assertScreenshotEquals(templateImage: String) =
        assertScreenshotEquals(TestScreenshotComparisonOptions(templateImage = templateImage))

    fun assertScreenshotEquals(options: TestScreenshotComparisonOptions)

    fun clickScreenButton(translationKey: String)

    fun <T> computeOnClient(function: (Minecraft) -> T): T

    fun <T, E : Throwable> computeOnClient(function: FailableFunction<Minecraft, T, E>): T

    fun getInput(): TestInput

    fun restoreDefaultGameOptions()

    fun runOnClient(action: (Minecraft) -> Unit)

    fun <E : Throwable> runOnClient(action: FailableConsumer<Minecraft, E>)

    fun setScreen(screen: Supplier<out Screen?>)

    fun takeScreenshot(name: String): Path = takeScreenshot(TestScreenshotOptions(name))

    fun takeScreenshot(options: TestScreenshotOptions): Path

    fun tryClickScreenButton(translationKey: String): Boolean

    fun waitFor(predicate: Predicate<Minecraft>): Int =
        waitFor(predicate, DEFAULT_TIMEOUT)

    fun waitFor(predicate: Predicate<Minecraft>, timeout: Int): Int

    fun waitForScreen(screenClass: Class<out Screen>?): Int

    fun waitTick()

    fun waitTicks(ticks: Int)

    fun worldBuilder(): TestWorldBuilder

    fun withWorld(callback: TestWorldBuilder.() -> Unit) {
        worldBuilder().apply(callback)
    }
}

internal class DefaultClientGameTestContext(
    override val testId: String,
) : ClientGameTestContext {
    companion object {
        private val optionSnapshotLock = Any()

        @Volatile
        private var defaultOptionValues: Map<String, Any?>? = null

        @Volatile
        private var deterministicOptionsInitialized: Boolean = false
    }

    internal fun describeScreen(screen: Screen?): String =
        screen?.let { "${it::class.java.name}@${System.identityHashCode(it)}" } ?: "null"

    private fun timeoutMessage(action: String, timeoutSeconds: Long, client: Minecraft): String {
        val caller = Thread.currentThread()
        return "Timed out waiting for client thread execution " +
            "(testId=$testId, action=$action, timeoutSeconds=$timeoutSeconds, " +
            "callerThread=${caller.name}, callerState=${caller.state}, " +
            "clientScreen=${describeScreen(client.screen)}, levelLoaded=${client.level != null})"
    }

    private val input: TestInput = object : TestInput {
        private val keysDown = mutableSetOf<Pair<Int, Int>>()
        private val mouseButtonsDown = mutableSetOf<Int>()
        private var cursorX: Double? = null
        private var cursorY: Double? = null

        private fun currentCursorX(screen: Screen): Double = cursorX ?: (screen.width / 2.0)

        private fun currentCursorY(screen: Screen): Double = cursorY ?: (screen.height / 2.0)

        override fun click(x: Double, y: Double, button: Int) {
            runOnClient { client ->
                val screen = client.screen ?: return@runOnClient
                cursorX = x
                cursorY = y
                screen.mouseClicked(x, y, button)
                screen.mouseReleased(x, y, button)
            }
        }

        override fun keyPress(keyCode: Int, scanCode: Int, modifiers: Int) {
            pressKey(keyCode, scanCode, modifiers)
        }

        override fun holdKey(keyCode: Int, scanCode: Int, modifiers: Int) {
            val key = keyCode to scanCode
            if (!keysDown.add(key)) return
            runOnClient { client ->
                client.screen?.keyPressed(keyCode, scanCode, modifiers)
            }
        }

        override fun releaseKey(keyCode: Int, scanCode: Int, modifiers: Int) {
            val key = keyCode to scanCode
            if (!keysDown.remove(key)) return
            runOnClient { client ->
                client.screen?.keyReleased(keyCode, scanCode, modifiers)
            }
        }

        override fun pressKey(keyCode: Int, scanCode: Int, modifiers: Int) {
            holdKey(keyCode, scanCode, modifiers)
            waitTick()
            releaseKey(keyCode, scanCode, modifiers)
        }

        override fun holdMouse(button: Int) {
            if (!mouseButtonsDown.add(button)) return
            runOnClient { client ->
                val screen = client.screen ?: return@runOnClient
                val x = currentCursorX(screen)
                val y = currentCursorY(screen)
                screen.mouseClicked(x, y, button)
            }
        }

        override fun releaseMouse(button: Int) {
            if (!mouseButtonsDown.remove(button)) return
            runOnClient { client ->
                val screen = client.screen ?: return@runOnClient
                val x = currentCursorX(screen)
                val y = currentCursorY(screen)
                screen.mouseReleased(x, y, button)
            }
        }

        override fun pressMouse(button: Int) {
            holdMouse(button)
            waitTick()
            releaseMouse(button)
        }

        override fun holdControl() {
            holdKey(GLFW.GLFW_KEY_LEFT_CONTROL)
        }

        override fun releaseControl() {
            releaseKey(GLFW.GLFW_KEY_LEFT_CONTROL)
        }

        override fun holdShift() {
            holdKey(GLFW.GLFW_KEY_LEFT_SHIFT)
        }

        override fun releaseShift() {
            releaseKey(GLFW.GLFW_KEY_LEFT_SHIFT)
        }

        override fun holdAlt() {
            holdKey(GLFW.GLFW_KEY_LEFT_ALT)
        }

        override fun releaseAlt() {
            releaseKey(GLFW.GLFW_KEY_LEFT_ALT)
        }

        override fun charTyped(char: Char, modifiers: Int) {
            runOnClient { client ->
                client.screen?.charTyped(char, modifiers)
            }
        }

        override fun typeChars(value: String) {
            value.forEach { charTyped(it) }
        }

        override fun scroll(x: Double, y: Double) {
            runOnClient { client ->
                client.screen?.mouseScrolled(0.0, 0.0, x, y)
            }
        }

        override fun setCursor(x: Double, y: Double) {
            cursorX = x
            cursorY = y
            runOnClient { client ->
                client.screen?.mouseMoved(x, y)
            }
        }

        override fun moveCursor(deltaX: Double, deltaY: Double) {
            runOnClient { client ->
                val screen = client.screen ?: return@runOnClient
                val nextX = currentCursorX(screen) + deltaX
                val nextY = currentCursorY(screen) + deltaY
                cursorX = nextX
                cursorY = nextY
                screen.mouseMoved(nextX, nextY)
            }
        }

        override fun clearInputs() {
            keysDown.toList().forEach { (keyCode, scanCode) ->
                releaseKey(keyCode, scanCode)
            }
            mouseButtonsDown.toList().forEach { button ->
                releaseMouse(button)
            }
            cursorX = null
            cursorY = null
        }
    }

    override fun assertTrue(condition: Boolean, message: () -> String) {
        if (!condition) fail(message())
    }

    override fun assertEquals(expected: Any?, actual: Any?, message: () -> String) {
        if (expected != actual) fail(message())
    }

    override fun fail(message: String): Nothing = throw IllegalStateException(message)

    override fun assertScreenshotContains(options: TestScreenshotComparisonOptions): Vector2i {
        val templatePath = ScreenshotManager.resolveTemplate(options.templateImage)
        if (!templatePath.toFile().exists()) {
            fail("Screenshot template not found: ${options.templateImage} (resolved to: $templatePath)")
        }

        val capturePath = takeScreenshot(options.screenshot)
        val matchPos = ScreenshotComparer.findInImage(templatePath.toFile(), capturePath.toFile())
        return matchPos ?: fail("Template image '${options.templateImage}' not found in screenshot")
    }

    override fun assertScreenshotEquals(options: TestScreenshotComparisonOptions) {
        val templatePath = ScreenshotManager.resolveTemplate(options.templateImage)
        if (!templatePath.toFile().exists()) {
            fail("Screenshot template not found: ${options.templateImage} (resolved to: $templatePath)")
        }

        val capturePath = takeScreenshot(options.screenshot)
        val equal = ScreenshotComparer.imagesEqual(templatePath.toFile(), capturePath.toFile())
        if (!equal) {
            fail("Screenshot does not match template '${options.templateImage}' (expected: $templatePath, actual: $capturePath)")
        }
    }

    override fun clickScreenButton(translationKey: String) {
        if (!tryClickScreenButton(translationKey)) {
            fail("No screen button found for translation key '$translationKey'")
        }
    }

    override fun <T> computeOnClient(function: (Minecraft) -> T): T {
        return computeOnClient("anonymous-client-action", CLIENT_EXEC_TIMEOUT_SECONDS, function)
    }

    internal fun <T> computeOnClient(action: String, timeoutSeconds: Long, function: (Minecraft) -> T): T {
        val client = Minecraft.getInstance()
        return if (client.isSameThread) {
            ensureDeterministicGameOptionsInitialized(client)
            function(client)
        } else {
            var value: T? = null
            var throwable: Throwable? = null
            val latch = CountDownLatch(1)
            client.execute {
                runCatching {
                    ensureDeterministicGameOptionsInitialized(client)
                    function(client)
                }
                    .onSuccess { value = it }
                    .onFailure { throwable = it }
                latch.countDown()
            }

            if (!latch.await(timeoutSeconds, TimeUnit.SECONDS)) {
                fail(timeoutMessage(action, timeoutSeconds, client))
            }

            throwable?.let { throw it }
            @Suppress("UNCHECKED_CAST")
            value as T
        }
    }

    override fun <T, E : Throwable> computeOnClient(function: FailableFunction<Minecraft, T, E>): T {
        return computeOnClient("failable-client-function", CLIENT_EXEC_TIMEOUT_SECONDS) { client -> function.apply(client) }
    }

    override fun getInput(): TestInput = input

    override fun restoreDefaultGameOptions() {
        runOnClient { client ->
            restoreCapturedGameOptions(client)
            client.options.save()
        }
    }

    private fun initializeDeterministicGameOptions(client: Minecraft) {
        val options = client.options
        if (defaultOptionValues == null) {
            synchronized(optionSnapshotLock) {
                if (defaultOptionValues == null) {
                    defaultOptionValues = captureOptionValues(options)
                }
            }
        }

        applyDeterministicTweaks(options)
        options.save()
    }

    private fun ensureDeterministicGameOptionsInitialized(client: Minecraft) {
        if (deterministicOptionsInitialized) return

        synchronized(optionSnapshotLock) {
            if (deterministicOptionsInitialized) return
            initializeDeterministicGameOptions(client)
            deterministicOptionsInitialized = true
        }
    }

    private fun restoreCapturedGameOptions(client: Minecraft) {
        val options = client.options
        val snapshot = defaultOptionValues ?: synchronized(optionSnapshotLock) {
            defaultOptionValues ?: captureOptionValues(options).also { defaultOptionValues = it }
        }
        restoreOptionValues(options, snapshot)
    }

    private fun captureOptionValues(options: Any): Map<String, Any?> {
        val captured = mutableMapOf<String, Any?>()
        for (field in options.javaClass.declaredFields) {
            if (Modifier.isStatic(field.modifiers)) continue
            field.isAccessible = true
            val option = runCatching { field.get(options) }.getOrNull() ?: continue
            if (!isOptionLike(option)) continue
            captured[field.name] = readOptionValue(option)
        }
        return captured
    }

    private fun restoreOptionValues(options: Any, snapshot: Map<String, Any?>) {
        for ((fieldName, value) in snapshot) {
            val field = runCatching { options.javaClass.getDeclaredField(fieldName) }.getOrNull() ?: continue
            field.isAccessible = true
            val option = runCatching { field.get(options) }.getOrNull() ?: continue
            if (!isOptionLike(option)) continue
            writeOptionValue(option, value)
        }
    }

    private fun applyDeterministicTweaks(options: Any) {
        for (field in options.javaClass.declaredFields) {
            if (Modifier.isStatic(field.modifiers)) continue
            field.isAccessible = true
            val option = runCatching { field.get(options) }.getOrNull() ?: continue
            if (!isOptionLike(option)) continue

            when {
                field.name.contains("tutorial", ignoreCase = true) -> {
                    writeOptionEnumByName(option, "NONE")
                }
                field.name.contains("cloud", ignoreCase = true) -> {
                    writeOptionEnumByName(option, "OFF")
                }
                field.name.contains("renderDistance", ignoreCase = true) || field.name.contains("viewDistance", ignoreCase = true) -> {
                    writeOptionValue(option, 5)
                }
                field.name.contains("music", ignoreCase = true) -> {
                    writeOptionValue(option, 0.0)
                }
            }
        }
    }

    private fun isOptionLike(option: Any): Boolean {
        val n = option.javaClass.simpleName
        return n == "OptionInstance" || n == "SimpleOption"
    }

    private fun readOptionValue(option: Any): Any? {
        val getter = option.javaClass.methods.firstOrNull {
            (it.name == "get" || it.name == "getValue") && it.parameterCount == 0
        } ?: return null
        return runCatching { getter.invoke(option) }.getOrNull()
    }

    private fun writeOptionValue(option: Any, value: Any?) {
        val setter = option.javaClass.methods.firstOrNull { method ->
            (method.name == "set" || method.name == "setValue") && method.parameterCount == 1
        } ?: return

        runCatching {
            setter.invoke(option, value)
        }
    }

    private fun writeOptionEnumByName(option: Any, enumName: String) {
        val current = readOptionValue(option) ?: return
        if (!current.javaClass.isEnum) return

        val constant = current.javaClass.enumConstants
            ?.firstOrNull { (it as? Enum<*>)?.name == enumName } ?: return
        writeOptionValue(option, constant)
    }

    override fun runOnClient(action: (Minecraft) -> Unit) {
        computeOnClient("run-on-client", CLIENT_EXEC_TIMEOUT_SECONDS) { client ->
            action(client)
        }
    }

    internal fun postToClient(action: (Minecraft) -> Unit) {
        Minecraft.getInstance().execute { action(Minecraft.getInstance()) }
    }

    override fun <E : Throwable> runOnClient(action: FailableConsumer<Minecraft, E>) {
        runOnClient { client -> action.accept(client) }
    }

    override fun setScreen(screen: Supplier<out Screen?>) {
        var expected: Screen? = null
        runOnClient { client ->
            expected = screen.get()
            client.setScreen(expected)
        }

        runCatching {
            waitFor(
                { client ->
                    val current = client.screen
                    val target = expected
                    when {
                        target == null -> current == null
                        current === target -> true
                        current != null && current::class.java == target::class.java -> true
                        else -> false
                    }
                },
                SCREEN_SET_TIMEOUT_TICKS,
            )
        }.getOrElse {
            fail(
                "Screen transition did not complete in time " +
                    "(testId=$testId, expected=${describeScreen(expected)}, " +
                    "actual=${computeOnClient { describeScreen(it.screen) }}, cause=${it.message})"
            )
        }
    }

    override fun takeScreenshot(options: TestScreenshotOptions): Path {
        val capturePath = ScreenshotManager.generateCapturePath(testId, options.name)
        computeOnClient("take-screenshot", CLIENT_EXEC_TIMEOUT_SECONDS) { client ->
            try {
                capturePath.parent?.let { Files.createDirectories(it) }

                // 1.21.1 API: capture from the main render target and write with NativeImage.
                Screenshot.takeScreenshot(client.mainRenderTarget).use { screenshot ->
                    screenshot.writeToFile(capturePath)
                }
            } catch (e: Exception) {
                fail("Failed to take screenshot: ${e.message}")
            }
        }
        return capturePath
    }

    override fun tryClickScreenButton(translationKey: String): Boolean {
        return tryClickScreenButton(translationKey, CLIENT_EXEC_TIMEOUT_SECONDS)
    }

    internal fun tryClickScreenButton(translationKey: String, timeoutSeconds: Long): Boolean {
        return computeOnClient("try-click-screen-button", timeoutSeconds) { client: Minecraft ->
            val screen = client.screen ?: return@computeOnClient false
            val widget = screen.children()
                .filterIsInstance<AbstractWidget>()
                .firstOrNull {
                    val contents = it.message.contents
                    contents is TranslatableContents && contents.key == translationKey
                } ?: return@computeOnClient false

            val cx = widget.x + (widget.width / 2.0)
            val cy = widget.y + (widget.height / 2.0)
            screen.mouseClicked(cx, cy, 0)
            screen.mouseReleased(cx, cy, 0)
            true
        }
    }

    override fun waitFor(predicate: Predicate<Minecraft>, timeout: Int): Int {
        if (timeout == ClientGameTestContext.NO_TIMEOUT) {
            var ticksWaited = 0
            while (!computeOnClient("wait-for", CLIENT_EXEC_TIMEOUT_SECONDS) { client: Minecraft -> predicate.test(client) }) {
                ticksWaited++
                waitTick()
            }
            return ticksWaited
        }

        require(timeout > 0) { "timeout must be positive or NO_TIMEOUT" }
        for (tick in 0 until timeout) {
            val ready = computeOnClient("wait-for", CLIENT_EXEC_TIMEOUT_SECONDS) { client: Minecraft -> predicate.test(client) }
            if (ready) return tick
            waitTick()
        }

        if (!computeOnClient("wait-for-final-check", CLIENT_EXEC_TIMEOUT_SECONDS) { client: Minecraft -> predicate.test(client) }) {
            fail("Predicate did not become true within $timeout ticks")
        }

        return timeout
    }

    override fun waitForScreen(screenClass: Class<out Screen>?): Int {
        return waitFor { client ->
            val current = client.screen
            if (screenClass == null) current == null else current != null && screenClass.isInstance(current)
        }
    }

    override fun waitTick() {
        waitTicks(1)
    }

    override fun waitTicks(ticks: Int) {
        val remainingTicks = ticks.coerceAtLeast(0)
        if (remainingTicks == 0) return

        val timeoutMillis = (remainingTicks.toLong() * DEFAULT_TICK_MILLIS * 20L).coerceAtLeast(DEFAULT_TICK_MILLIS)
        if (!ThreadingImpl.awaitTicks(remainingTicks, timeoutMillis)) {
            fail("Timed out waiting for $remainingTicks client tick(s)")
        }
    }

    override fun worldBuilder(): TestWorldBuilder = DefaultTestWorldBuilder(this)
}

private class DefaultTestWorldBuilder(
    private val context: DefaultClientGameTestContext,
) : TestWorldBuilder {
    private var useConsistentSettings = true
    private var settingsAdjustor: Consumer<WorldCreationUiState> = Consumer { }

    override fun setUseConsistentSettings(useConsistentSettings: Boolean): TestWorldBuilder = apply {
        this.useConsistentSettings = useConsistentSettings
    }

    override fun adjustSettings(settingsAdjuster: Consumer<WorldCreationUiState>): TestWorldBuilder = apply {
        this.settingsAdjustor = settingsAdjuster
    }

    override fun create(): TestSingleplayerContext {
        val saveDirectory = context.computeOnClient("world-builder-open-create-screen", WORLD_BUILDER_EXEC_TIMEOUT_SECONDS) { client ->
            val oldScreen = client.screen
            CreateWorldScreen.openFresh(client, oldScreen)

            val createWorldScreen = client.screen as? CreateWorldScreen
                ?: context.fail("CreateWorldScreen.openFresh did not open a world-creation screen")

            val creator = createWorldScreen.uiState

            if (useConsistentSettings) {
                setConsistentSettings(creator)
            }

            settingsAdjustor.accept(creator)

            client.levelSource.baseDir.resolve(creator.targetFolder)
        }

        context.postToClient { client ->
            val screen = client.screen ?: return@postToClient
            val widget = screen.children()
                .filterIsInstance<AbstractWidget>()
                .firstOrNull {
                    val contents = it.message.contents
                    contents is TranslatableContents && contents.key == "selectWorld.create"
                } ?: return@postToClient

            val cx = widget.x + (widget.width / 2.0)
            val cy = widget.y + (widget.height / 2.0)
            screen.mouseClicked(cx, cy, 0)
            screen.mouseReleased(cx, cy, 0)
        }

        // World creation transitions can momentarily starve the harness tick gate.
        // Treat this first post-click tick as best-effort and rely on world-load checks below.
        runCatching { context.waitTick() }
        waitForWorldLoad()

        return DefaultTestSingleplayerContext(
            clientContext = context,
            saveDirectory = saveDirectory,
        )
    }

    override fun createServer(serverProperties: Properties): TestDedicatedServerContext {
        return DefaultServerWorldBuilder(context).create(serverProperties)
    }

    private fun waitForWorldLoad() {
        val worldLoadTimeoutTicks = (WORLD_BUILDER_EXEC_TIMEOUT_SECONDS * 1000 / DEFAULT_TICK_MILLIS).toInt()
        runCatching {
            context.waitTick()
            context.waitFor(
                { client -> client.level != null || client.singleplayerServer != null },
                worldLoadTimeoutTicks,
            )
        }.getOrElse {
            val client = Minecraft.getInstance()
            Archie.LOGGER.warn(
                "Timed out waiting for world load start (testId=${context.testId}, timeoutSeconds=$WORLD_BUILDER_EXEC_TIMEOUT_SECONDS, screen=${context.describeScreen(client.screen)}, levelLoaded=${client.level != null}, serverStarted=${client.singleplayerServer != null})"
            )
        }
    }

    private fun setConsistentSettings(creator: WorldCreationUiState) {
        val flatPreset: Holder<WorldPreset> = creator.settings
            .worldgenLoadContext()
            .lookupOrThrow(Registries.WORLD_PRESET)
            .getOrThrow(WorldPresets.FLAT)

        creator.worldType = WorldCreationUiState.WorldTypeEntry(flatPreset)
        creator.seed = "1"
    }
}

private class DefaultTestSingleplayerContext(
    override val clientContext: ClientGameTestContext,
    override val saveDirectory: Path,
) : TestSingleplayerContext {
    override val clientWorld: TestClientWorldContext = DefaultTestClientWorldContext(clientContext)
    override val server: TestServerContext = DefaultTestServerContext(clientContext)

    override fun close() {
        ThreadingImpl.checkOnGametestThread("close")

        clientContext.runOnClient { client ->
            val hasLevel = client.level != null
            val hasLocalServer = client.singleplayerServer != null || client.isLocalServer
            if (!hasLevel && !hasLocalServer) return@runOnClient

            client.level?.disconnect()
            if (hasLocalServer) {
                client.disconnect(GenericMessageScreen(Component.translatable("menu.savingLevel")))
            } else {
                client.disconnect()
            }
        }

        runCatching {
            clientContext.waitFor(
                { client -> client.level == null && client.singleplayerServer == null },
                SharedConstants.TICKS_PER_MINUTE,
            )
        }.getOrElse { error ->
            Archie.LOGGER.warn(
                "Singleplayer world did not close cleanly (testId=${clientContext.testId}, saveDir=$saveDirectory, cause=${error.message})"
            )
        }

        // Final recovery pass: always try to land on title and clear any lingering local session state.
        clientContext.runOnClient { client ->
            if (client.level != null || client.singleplayerServer != null || client.isLocalServer) {
                client.level?.disconnect()
                if (client.singleplayerServer != null || client.isLocalServer) {
                    client.disconnect(GenericMessageScreen(Component.translatable("menu.savingLevel")))
                } else {
                    client.disconnect()
                }
            }
            if (client.screen !is TitleScreen) {
                client.setScreen(TitleScreen())
            }
        }

        runCatching {
            clientContext.waitFor(
                { client -> client.level == null && client.singleplayerServer == null },
                SharedConstants.TICKS_PER_SECOND * 10,
            )
        }.getOrElse { error ->
            Archie.LOGGER.warn(
                "Singleplayer session still present after forced close (testId=${clientContext.testId}, saveDir=$saveDirectory, cause=${error.message})"
            )
        }
    }
}

private class DefaultTestServerContext(
    private val clientContext: ClientGameTestContext,
) : TestServerContext {
    override fun runCommand(command: String) {
        ThreadingImpl.checkOnGametestThread("runCommand")
        require(command.isNotBlank()) { "command cannot be blank" }

        runOnServer(FailableConsumer { server ->
            runCommandReflective(server, command)
        })
    }

    override fun <E : Throwable> runOnServer(action: FailableConsumer<MinecraftServer, E>) {
        ThreadingImpl.checkOnGametestThread("runOnServer")
        val server = requireSingleplayerServer()
        ThreadingImpl.runOnServer {
            action.accept(server)
        }
    }

    override fun <T, E : Throwable> computeOnServer(function: FailableFunction<MinecraftServer, T, E>): T {
        ThreadingImpl.checkOnGametestThread("computeOnServer")
        val server = requireSingleplayerServer()
        var result: T? = null
        ThreadingImpl.runOnServer {
            result = function.apply(server)
        }

        @Suppress("UNCHECKED_CAST")
        return result as T
    }

    private fun requireSingleplayerServer(): MinecraftServer {
        return clientContext.computeOnClient { client ->
            client.singleplayerServer ?: throw IllegalStateException("No integrated server is running")
        }
    }

    private fun runCommandReflective(server: MinecraftServer, command: String) {
        val source = runCatching {
            server.javaClass.methods.firstOrNull { it.name == "createCommandSourceStack" && it.parameterCount == 0 }
                ?.invoke(server)
        }.getOrNull()

        val commandsObj = runCatching {
            server.javaClass.methods.firstOrNull { it.name == "getCommands" && it.parameterCount == 0 }?.invoke(server)
                ?: server.javaClass.methods.firstOrNull { it.name == "getCommandManager" && it.parameterCount == 0 }?.invoke(server)
        }.getOrNull() ?: throw IllegalStateException("Could not resolve command manager from server")

        val executeMethod = commandsObj.javaClass.methods.firstOrNull { method ->
            method.parameterCount == 2 &&
                (method.name == "performPrefixedCommand" || method.name == "executeWithPrefix" || method.name == "performCommand")
        } ?: throw IllegalStateException("Could not find command execution method on ${commandsObj.javaClass.name}")

        executeMethod.invoke(commandsObj, source, command)
    }
}

private class DefaultServerWorldBuilder(
    private val context: DefaultClientGameTestContext,
) {
    fun create(serverProperties: Properties): TestDedicatedServerContext {
        val serverStartTimeout = WORLD_BUILDER_EXEC_TIMEOUT_SECONDS

        lateinit var serverInstance: Any
        lateinit var serverDirectory: Path

        try {
            // Prepare server directory on client thread
            context.computeOnClient("setup-dedicated-server-dir", serverStartTimeout) { mc ->
                try {
                    val gameDir = mc.gameDirectory.toPath()
                    val dir = gameDir.resolve("test_server_${System.nanoTime()}")
                    Files.createDirectories(dir)
                    serverDirectory = dir

                    // Apply server properties
                    writeServerBootstrapFiles(dir, serverProperties)
                } catch (e: Exception) {
                    context.fail("Failed to set up server directory: ${e.message}")
                }
            }

            // Start server asynchronously (blocks gametest thread, not client thread)
            try {
                serverInstance = ADedicatedServerPlatform.start(
                    serverDirectory,
                    serverProperties,
                    serverStartTimeout
                )
                DedicatedServerLifecycleTracker.register(serverInstance)
            } catch (e: Exception) {
                context.fail("Failed to start dedicated server: ${e.message}")
            }

            return DefaultTestDedicatedServerContext(
                clientContext = context,
                serverInstance = serverInstance,
                serverDirectory = serverDirectory,
            )
        } catch (e: Exception) {
            context.fail("Error creating dedicated server: ${e.message}")
        }
    }

    private fun writeServerBootstrapFiles(serverDirectory: Path, serverProperties: Properties) {
        val merged = Properties()
        merged.putAll(serverProperties)
        merged.putIfAbsent("online-mode", "false")
        merged.putIfAbsent("spawn-protection", "0")
        merged.putIfAbsent("max-players", "1")

        // The dedicated-server launcher reads these files from the process working directory,
        // while the harness also keeps an isolated copy under the generated per-test server dir.
        writeBootstrapFiles(serverDirectory, merged)
        writeBootstrapFiles(Path.of("."), merged)
    }

    private fun writeBootstrapFiles(targetDirectory: Path, properties: Properties) {
        writeTextFile(targetDirectory, "server.properties") { writer ->
            properties.store(writer, "Archie GameTest dedicated server properties")
        }
        writeTextFile(targetDirectory, "eula.txt") { writer ->
            writer.write("eula=true")
            writer.newLine()
        }
    }

    private fun writeTextFile(targetDirectory: Path, fileName: String, writerAction: (java.io.BufferedWriter) -> Unit) {
        Files.newBufferedWriter(targetDirectory.resolve(fileName)).use(writerAction)
    }
}

private data class DefaultTestDedicatedServerContext(
    override val clientContext: ClientGameTestContext,
    val serverInstance: Any,
    override val serverDirectory: Path,
) : TestDedicatedServerContext {
    override fun connect(): TestServerConnection {
        ThreadingImpl.checkOnGametestThread("connect")

        val port = ADedicatedServerPlatform.port(serverInstance)
        clientContext.runOnClient { client ->
            connectToLocalhost(client, port)
        }

        clientContext.waitFor(
            { client -> client.level != null },
            ClientGameTestContext.DEFAULT_TIMEOUT,
        )

        return DefaultTestServerConnection(
            clientContext = clientContext,
            clientWorld = DefaultTestClientWorldContext(clientContext),
        )
    }

    override fun close() {
        ThreadingImpl.checkOnGametestThread("close")

        try {
            val stopRequested = requestStopWithTimeout(timeoutMillis = TimeUnit.SECONDS.toMillis(10))
            if (!stopRequested) {
                Archie.LOGGER.warn(
                    "Timed out requesting dedicated server stop; forcing halt (testId=${clientContext.testId}, serverDir=$serverDirectory)"
                )
                forceHaltServer()
            }

            runCatching {
                clientContext.waitFor(
                    { _ -> !ADedicatedServerPlatform.isAlive(serverInstance) },
                    SharedConstants.TICKS_PER_MINUTE,
                )
            }.getOrElse { error ->
                Archie.LOGGER.warn(
                    "Dedicated server did not stop cleanly (testId=${clientContext.testId}, serverDir=$serverDirectory, alive=${ADedicatedServerPlatform.isAlive(serverInstance)}, cause=${error.message})"
                )
            }
        } finally {
            DedicatedServerLifecycleTracker.unregister(serverInstance)
        }
    }

    private fun requestStopWithTimeout(timeoutMillis: Long): Boolean {
        var stopFailure: Throwable? = null
        val stopThread = Thread({
            runCatching {
                // Stopping from outside the server tick thread avoids self-stop deadlocks.
                ADedicatedServerPlatform.stop(serverInstance)
            }.recoverCatching {
                ThreadingImpl.runOnServer {
                    ADedicatedServerPlatform.stop(serverInstance)
                }
            }.onFailure {
                stopFailure = it
            }
        }, "Archie Dedicated GameTest Server Stop")

        stopThread.isDaemon = true
        stopThread.start()
        stopThread.join(timeoutMillis)

        stopFailure?.let {
            throw IllegalStateException("Failed to stop dedicated server context", it)
        }

        return !stopThread.isAlive
    }

    private fun forceHaltServer() {
        runCatching {
            val haltMethod = serverInstance.javaClass.methods.firstOrNull { method ->
                (method.name == "stopServer") && method.parameterCount <= 1
            } ?: return

            when (haltMethod.parameterCount) {
                0 -> haltMethod.invoke(serverInstance)
                1 -> {
                    val paramType = haltMethod.parameterTypes[0]
                    when (paramType) {
                        Boolean::class.javaPrimitiveType, Boolean::class.java -> haltMethod.invoke(serverInstance, true)
                        else -> haltMethod.invoke(serverInstance, null)
                    }
                }
            }
        }
    }

    private fun connectToLocalhost(client: Minecraft, port: Int) {
        val connectScreenClass = runCatching {
            Class.forName("net.minecraft.client.gui.screens.ConnectScreen")
        }.getOrElse {
            throw IllegalStateException("ConnectScreen class not found")
        }

        val addressClass = runCatching {
            Class.forName("net.minecraft.client.multiplayer.resolver.ServerAddress")
        }.getOrNull() ?: runCatching {
            Class.forName("net.minecraft.client.multiplayer.ServerAddress")
        }.getOrElse {
            throw IllegalStateException("ServerAddress class not found")
        }

        val serverDataClass = runCatching {
            Class.forName("net.minecraft.client.multiplayer.ServerData")
        }.getOrElse {
            throw IllegalStateException("ServerData class not found")
        }

        val parseMethod = addressClass.methods.firstOrNull {
            it.name == "parseString" && it.parameterCount == 1
        } ?: addressClass.methods.firstOrNull {
            it.name == "parse" && it.parameterCount == 1
        } ?: throw IllegalStateException("Could not find ServerAddress parse method")

        val address = parseMethod.invoke(null, "localhost:$port")

        val serverTypeClass = serverDataClass.declaredClasses.firstOrNull {
            it.simpleName == "Type" && it.isEnum
        }
        val serverTypeValue = serverTypeClass?.enumConstants?.firstOrNull()
        val serverData = serverDataClass.constructors.firstOrNull { it.parameterCount >= 3 }
            ?.newInstance("localhost", "localhost:$port", serverTypeValue)

        val connectMethod = connectScreenClass.methods.firstOrNull {
            it.name == "startConnecting" || it.name == "connect"
        } ?: throw IllegalStateException("Could not find ConnectScreen connect method")

        val args = connectMethod.parameterTypes.map { param ->
            when {
                Screen::class.java.isAssignableFrom(param) -> client.screen
                Minecraft::class.java.isAssignableFrom(param) -> client
                param.isAssignableFrom(addressClass) -> address
                serverData != null && param.isAssignableFrom(serverDataClass) -> serverData
                param == Boolean::class.javaPrimitiveType || param == Boolean::class.java -> false
                else -> null
            }
        }.toTypedArray()

        connectMethod.invoke(null, *args)
    }

}

private data class DefaultTestServerConnection(
    override val clientContext: ClientGameTestContext,
    override val clientWorld: TestClientWorldContext,
) : TestServerConnection {
    override fun disconnect() {
        ThreadingImpl.checkOnGametestThread("close")

        clientContext.runOnClient { client ->
            if (client.level == null) {
                if (client.screen !is TitleScreen) {
                    client.setScreen(TitleScreen())
                }
                return@runOnClient
            }

            client.level?.disconnect()
            client.disconnect()
        }

        runCatching {
            clientContext.waitFor({ client -> client.level == null }, ClientGameTestContext.DEFAULT_TIMEOUT)
        }.getOrElse { error ->
            Archie.LOGGER.warn(
                "Timed out waiting for dedicated-server client disconnect (testId=${clientContext.testId}, cause=${error.message})"
            )
        }
        clientContext.setScreen(Supplier { TitleScreen() })
    }
}

private class DefaultTestClientWorldContext(
    private val clientContext: ClientGameTestContext,
) : TestClientWorldContext {
    override fun waitForChunksDownload(timeout: Int): Int {
        ThreadingImpl.checkOnGametestThread("waitForChunksDownload")
        return clientContext.waitFor({ client -> areChunksLoaded(client) }, timeout)
    }

    override fun waitForChunksRender(waitForDownload: Boolean, timeout: Int): Int {
        ThreadingImpl.checkOnGametestThread("waitForChunksRender")
        return clientContext.waitFor(
            { client ->
                (!waitForDownload || areChunksLoaded(client)) && areChunksRendered(client)
            },
            timeout,
        )
    }

    private fun areChunksLoaded(client: Minecraft): Boolean {
        val level = client.level ?: return false
        val player = client.player ?: return false

        val viewDistance = resolveClientViewDistance(client).coerceAtLeast(2)
        val centerChunkX = player.blockX shr 4
        val centerChunkZ = player.blockZ shr 4
        val chunkSource = level.chunkSource

        val hasChunkMethod = chunkSource.javaClass.methods.firstOrNull {
            (it.name == "hasChunk" || it.name == "isChunkLoaded") &&
                it.parameterCount == 2 &&
                it.parameterTypes[0] == Int::class.javaPrimitiveType &&
                it.parameterTypes[1] == Int::class.javaPrimitiveType
        }

        if (hasChunkMethod != null) {
            for (dz in -viewDistance..viewDistance) {
                for (dx in -viewDistance..viewDistance) {
                    val loaded = runCatching {
                        hasChunkMethod.invoke(chunkSource, centerChunkX + dx, centerChunkZ + dz) as? Boolean
                    }.getOrNull() ?: false
                    if (!loaded) return false
                }
            }
            return true
        }

        // Fallback when chunk-source internals differ across mappings.
        return true
    }

    private fun areChunksRendered(client: Minecraft): Boolean {
        val levelRenderer = client.levelRenderer ?: return false
        val renderCompleteMethod = levelRenderer.javaClass.methods.firstOrNull {
            (it.name == "isTerrainRenderComplete" || it.name == "isRenderComplete") &&
                it.parameterCount == 0
        }

        if (renderCompleteMethod != null) {
            return runCatching {
                renderCompleteMethod.invoke(levelRenderer) as? Boolean
            }.getOrNull() ?: false
        }

        return true
    }

    private fun resolveClientViewDistance(client: Minecraft): Int {
        val options = client.options
        val method = options.javaClass.methods.firstOrNull {
            (it.name == "getEffectiveRenderDistance" || it.name == "getClampedViewDistance") && it.parameterCount == 0
        }

        return runCatching {
            (method?.invoke(options) as? Int) ?: 5
        }.getOrDefault(5)
    }
}

data class AClientGameTestSummary(
    val passed: Int,
    val failed: Int,
    val skipped: Int,
    val failedTests: List<String> = emptyList(),
    val failedDetails: List<AClientGameTestFailure> = emptyList(),
)

object AClientGameTestHarness {
    fun run(modToClasses: Map<Mod, List<Class<*>>>, side: AGameTestSide?): AClientGameTestSummary {
        if (side != AGameTestSide.CLIENT) return AClientGameTestSummary(passed = 0, failed = 0, skipped = 0)

        val selectedMods = selectModsToRun(modToClasses)

        var passed = 0
        var failed = 0
        var skipped = 0
        val failedTests = mutableListOf<String>()
        val failedDetails = mutableListOf<AClientGameTestFailure>()

        selectedMods.forEach { (mod, classes) ->
            classes.forEach { clazz ->
                val methods = clazz.declaredMethods.filter {
                    it.isAnnotationPresent(ClientGameTest::class.java)
                }

                if (methods.isEmpty()) return@forEach

                val instance = clazz.kotlin.objectInstance ?: clazz.kotlin.primaryConstructor?.call()

                methods.forEach { method ->
                    val clientTest = method.getAnnotation(ClientGameTest::class.java)
                    val explicitName = clientTest?.name?.takeIf { it.isNotBlank() }
                    val testId = explicitName ?: "${mod.modId}:${clazz.simpleName.lowercase()}.${method.name.lowercase()}"
                    val context = DefaultClientGameTestContext(testId)

                    val params = method.parameterTypes
                    val supported = when {
                        params.isEmpty() -> true
                        params.size == 1 && ClientGameTestContext::class.java.isAssignableFrom(params[0]) -> true
                        else -> false
                    }

                    if (!supported) {
                        skipped++
                        Archie.LOGGER.warn("[ClientGameTest] Skipping {} (unsupported signature: {} params)", testId, params.size)
                        return@forEach
                    }

                    runCatching {
                        context.getInput().clearInputs()
                        method.isAccessible = true
                        if (params.isEmpty()) method.invoke(instance)
                        else method.invoke(instance, context)
                    }.onSuccess {
                        context.getInput().clearInputs()
                        passed++
                        Archie.LOGGER.info("[ClientGameTest] PASS {}", testId)
                    }.onFailure { error ->
                        runCatching { context.getInput().clearInputs() }
                        failed++
                        failedTests += testId
                        failedDetails += AClientGameTestFailure(testId, rootCauseSummary(error))
                        Archie.LOGGER.error("[ClientGameTest] FAIL {}", testId, error)
                    }
                }
            }
        }

        Archie.LOGGER.info("[ClientGameTest] Completed: passed={}, failed={}, skipped={}", passed, failed, skipped)
        // Safety net: if a test aborted before context.close(), force-stop leaked dedicated servers
        // before the client begins shutdown to avoid dedicated tick crashes against torn-down GLFW.
        DedicatedServerLifecycleTracker.stopAllLeakedServers()

        val minecraft = Minecraft.getInstance()
        minecraft.execute {
            val flag = minecraft.isLocalServer
            val serverdata = minecraft.currentServer
            minecraft.level?.disconnect()
            if (flag) {
                minecraft.disconnect(GenericMessageScreen(Component.translatable("menu.savingLevel")))
            } else {
                minecraft.disconnect()
            }

            val titlescreen = TitleScreen()
            if (flag) {
                minecraft.setScreen(titlescreen)
            } else if (serverdata != null && serverdata.isRealm) {
                minecraft.setScreen(RealmsMainScreen(titlescreen))
            } else {
                minecraft.setScreen(JoinMultiplayerScreen(titlescreen))
            }
        }
        return AClientGameTestSummary(
            passed = passed,
            failed = failed,
            skipped = skipped,
            failedTests = failedTests,
            failedDetails = failedDetails,
        )
    }
}

private fun selectModsToRun(modToClasses: Map<Mod, List<Class<*>>>): Map<Mod, List<Class<*>>> {
    val filter = System.getProperty(CLIENT_GAMETEST_MOD_ID_FILTER_PROPERTY)?.trim().orEmpty()
    if (filter.isEmpty()) return modToClasses

    val requestedIds = filter.split(',')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .toSet()

    require(requestedIds.isNotEmpty()) {
        "No valid mod IDs specified in client game test filter '$CLIENT_GAMETEST_MOD_ID_FILTER_PROPERTY'"
    }

    val selected = modToClasses.filterKeys { mod -> mod.modId in requestedIds }
    require(selected.isNotEmpty()) {
        "No client gametests found for requested mod IDs: ${requestedIds.joinToString(",")}" 
    }
    return selected
}

private fun rootCauseSummary(error: Throwable): String {
    val root = generateSequence(error) { it.cause }.last()
    val message = root.message?.takeIf { it.isNotBlank() }
    return if (message != null) {
        "${root::class.java.name}: $message"
    } else {
        root::class.java.name
    }
}

