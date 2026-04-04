package net.kernelpanicsoft.archie.gametest

import dev.architectury.platform.Mod
import net.kernelpanicsoft.archie.Archie
import net.minecraft.gametest.framework.GameTest
import kotlin.reflect.full.primaryConstructor

/** Marks a test method to be executed by the Archie client GameTest backport harness. */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class AClientGameTest(val name: String = "")

/** Minimal assertion/report API passed to client harness test methods. */
interface AClientGameTestContext {
    val testId: String
    fun assertTrue(condition: Boolean, message: () -> String)
    fun assertEquals(expected: Any?, actual: Any?, message: () -> String = { "Expected <$expected>, got <$actual>" })
    fun fail(message: String): Nothing
}

internal class DefaultAClientGameTestContext(
    override val testId: String,
) : AClientGameTestContext {
    override fun assertTrue(condition: Boolean, message: () -> String) {
        if (!condition) fail(message())
    }

    override fun assertEquals(expected: Any?, actual: Any?, message: () -> String) {
        if (expected != actual) fail(message())
    }

    override fun fail(message: String): Nothing = throw IllegalStateException(message)
}

data class AClientGameTestSummary(
    val passed: Int,
    val failed: Int,
    val skipped: Int,
)

internal object AClientGameTestHarness {
    fun run(modToClasses: Map<Mod, List<Class<*>>>, side: AGameTestSide): AClientGameTestSummary {
        if (side != AGameTestSide.CLIENT) return AClientGameTestSummary(passed = 0, failed = 0, skipped = 0)

        var passed = 0
        var failed = 0
        var skipped = 0

        modToClasses.forEach { (mod, classes) ->
            classes.forEach { clazz ->
                val methods = clazz.declaredMethods.filter {
                    it.isAnnotationPresent(AClientGameTest::class.java) || it.isAnnotationPresent(GameTest::class.java)
                }

                if (methods.isEmpty()) return@forEach

                val instance = clazz.kotlin.objectInstance ?: clazz.kotlin.primaryConstructor?.call()

                methods.forEach { method ->
                    val gameTest = method.getAnnotation(GameTest::class.java)
                    val clientTest = method.getAnnotation(AClientGameTest::class.java)
                    val explicitName = clientTest?.name?.takeIf { it.isNotBlank() }
                    val testId = explicitName ?: "${mod.modId}:${clazz.simpleName}.${method.name}"
                    val context = DefaultAClientGameTestContext(testId)

                    val params = method.parameterTypes
                    val supported = when {
                        params.isEmpty() -> true
                        params.size == 1 && AClientGameTestContext::class.java.isAssignableFrom(params[0]) -> true
                        gameTest != null -> false
                        else -> false
                    }

                    if (!supported) {
                        skipped++
                        Archie.LOGGER.warn("[ClientGameTest] Skipping {} (unsupported signature: {} params)", testId, params.size)
                        return@forEach
                    }

                    runCatching {
                        method.isAccessible = true
                        if (params.isEmpty()) method.invoke(instance)
                        else method.invoke(instance, context)
                    }.onSuccess {
                        passed++
                        Archie.LOGGER.info("[ClientGameTest] PASS {}", testId)
                    }.onFailure { error ->
                        failed++
                        Archie.LOGGER.error("[ClientGameTest] FAIL {}", testId, error)
                    }
                }
            }
        }

        Archie.LOGGER.info("[ClientGameTest] Completed: passed={}, failed={}, skipped={}", passed, failed, skipped)
        return AClientGameTestSummary(passed = passed, failed = failed, skipped = skipped)
    }
}

