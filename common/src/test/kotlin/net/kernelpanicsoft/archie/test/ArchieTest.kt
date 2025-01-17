package net.kernelpanicsoft.archie.test

import com.mojang.logging.LogUtils
import dev.architectury.platform.Mod
import dev.architectury.platform.Platform
import net.kernelpanicsoft.archie.Archie
import net.minecraft.resources.ResourceLocation
import org.slf4j.Logger

object ArchieTest
{
	const val MOD_ID = "${Archie.MOD_ID}_test"

	@JvmField
	val MOD: Mod = Platform.getMod(MOD_ID)

	@JvmField
	val LOGGER: Logger = LogUtils.getLogger()

	@JvmStatic
	operator fun get(loc: String): ResourceLocation = ResourceLocation.fromNamespaceAndPath(Archie.MOD_ID, loc)

	@JvmStatic
	fun init()
	{
		BlockRegistry.init()
		ItemRegistry.init()
		TileRegistry.init()
		GuiRegistry.init()
	}

	@JvmStatic
	fun initClient()
	{
		GuiRegistry.initClient()
	}

	@JvmStatic
	fun initCommon()
	{
	}

}