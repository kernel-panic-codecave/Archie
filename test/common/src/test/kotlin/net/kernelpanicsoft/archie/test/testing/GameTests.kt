package net.kernelpanicsoft.archie.test.testing

import net.kernelpanicsoft.archie.events.gametest.AGametestEvents
import net.kernelpanicsoft.archie.gametest.junit.GameTestRunner
import net.kernelpanicsoft.archie.test.gametest.archieTestGameTests
import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

@Execution(ExecutionMode.CONCURRENT)
class GameTests
{
	@TestFactory
	fun tests(): Collection<DynamicContainer> = GameTestRunner.tests("archie_test", "test", AGametestEvents.ArchieGameTestBuilder::archieTestGameTests)
}