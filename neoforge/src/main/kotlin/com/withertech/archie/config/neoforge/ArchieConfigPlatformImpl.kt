package com.withertech.archie.config.neoforge

import dev.architectury.platform.Mod
import me.shedaniel.clothconfig2.api.ConfigBuilder
import net.neoforged.fml.ModList
import net.neoforged.neoforge.client.gui.IConfigScreenFactory
import java.util.function.Supplier

object ArchieConfigPlatformImpl
{
	@JvmStatic
	fun registerScreenHandler(mod: Mod, builder: () -> ConfigBuilder)
	{
		ModList.get().getModContainerById(mod.modId).ifPresent { container ->
			container.registerExtensionPoint(IConfigScreenFactory::class.java, Supplier {
				IConfigScreenFactory { _, screen ->
					builder().setParentScreen(screen).build()
				}
			})
		}
	}
}