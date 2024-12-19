package com.withertech.archie

import com.withertech.archie.test.BlockRegistry
import com.withertech.archie.test.GuiRegistry
import com.withertech.archie.test.ItemRegistry
import com.withertech.archie.test.TileRegistry

object ArchieTest
{
	@JvmStatic
	fun init()
	{
		BlockRegistry.init()
		ItemRegistry.init()
		TileRegistry.init()
		GuiRegistry.init()
	}
}