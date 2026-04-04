package net.kernelpanicsoft.archie.testing

import net.kernelpanicsoft.archie.events.AEvents
import net.kernelpanicsoft.archie.gametest.internal.archieGameTests
import net.kernelpanicsoft.archie.gametest.junit.GameTestRunner
import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.TestFactory

class GameTests
{
	@TestFactory
	fun tests(): Collection<DynamicContainer> = GameTestRunner.tests("archie", AEvents.ArchieGameTestBuilder::archieGameTests)
}