package com.withertech.archie.gametest.internal

import com.withertech.archie.Archie
import com.withertech.archie.events.AEvents
import com.withertech.archie.gametest.AGameTestEventObject
import com.withertech.archie.gametest.internal.tests.CommonTests

internal object ArchieGameTest : AGameTestEventObject(Archie.MOD)
{
	override fun AEvents.ArchieGameTestBuilder.handler()
	{
		register<CommonTests>()
	}
}