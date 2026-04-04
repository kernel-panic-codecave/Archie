package net.kernelpanicsoft.archie.gametest

import dev.architectury.platform.Platform
import net.kernelpanicsoft.archie.Archie
import net.minecraft.gametest.framework.GameTestInfo
import net.minecraft.gametest.framework.TestReporter
import net.minecraft.resources.ResourceLocation

object VerboseTestReporter : TestReporter
{
	override fun onTestFailed(testInfo: GameTestInfo)
	{
		Archie.LOGGER.error("[GameTest] FAIL {}", testId(testInfo), testInfo.error)
	}

	override fun onTestSuccess(testInfo: GameTestInfo)
	{

		Archie.LOGGER.info("[GameTest] PASS {}", testId(testInfo))
	}

	fun testId(testInfo: GameTestInfo): String
	{
		val testModId = ResourceLocation.parse(testInfo.structureName).namespace
		if (!Platform.isModLoaded(testModId)) return testInfo.testName
		return "${testModId}:${testInfo.testName}"
	}
}