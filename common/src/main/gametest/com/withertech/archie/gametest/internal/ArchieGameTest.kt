package com.withertech.archie.gametest.internal

import com.withertech.archie.Archie
import com.withertech.archie.events.ArchieEvents
import com.withertech.archie.gametest.internal.tests.CommonTests

internal object ArchieGameTest
{
	fun init()
	{
		ArchieEvents.REGISTER_GAME_TEST.register(ArchieEvents.RegisterGameTestHandler.create(Archie.MOD) {
			register<CommonTests>()
		})
	}
}