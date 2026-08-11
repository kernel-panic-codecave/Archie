package net.kernelpanicsoft.archie.test

import com.mojang.logging.LogUtils
import dev.architectury.platform.Mod
import dev.architectury.platform.Platform
import net.kernelpanicsoft.archie.Archie
import net.kernelpanicsoft.archie.data.platform.ADataGeneratorPlatform
import net.kernelpanicsoft.archie.events.datagen.ADatagenEvents
import net.kernelpanicsoft.archie.events.gametest.AGametestEvents
import net.kernelpanicsoft.archie.gametest.platform.AGameTestPlatform
import net.kernelpanicsoft.archie.test.gametest.ArchieTestGameTest
import net.kernelpanicsoft.archie.test.gametest.DataAttachmentTestFixtures
import net.kernelpanicsoft.archie.test.data.ArchieTestDatagen
import net.kernelpanicsoft.archie.util.onClient
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
	operator fun get(loc: String): ResourceLocation = ResourceLocation.fromNamespaceAndPath(MOD_ID, loc)

	@JvmStatic
	fun init()
	{
		ADatagenEvents += MOD
		AGametestEvents += MOD
		if (ADataGeneratorPlatform.isDataGen)
			ArchieTestDatagen.init()
		if (AGameTestPlatform.isGameTest)
			ArchieTestGameTest.init()
		BlockRegistry.init()
		ItemRegistry.init()
		TileRegistry.init()
		GuiRegistry.init()
		if (AGameTestPlatform.isGameTest)
			DataAttachmentTestFixtures.init()
	}

	@JvmStatic
	fun initClient()
	{
	}

	@JvmStatic
	fun initCommon()
	{
	}
}