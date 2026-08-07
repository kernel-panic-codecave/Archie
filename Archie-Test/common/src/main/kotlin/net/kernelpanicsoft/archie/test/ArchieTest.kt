package net.kernelpanicsoft.archie.test

import com.mojang.logging.LogUtils
import dev.architectury.platform.Mod
import dev.architectury.platform.Platform
import net.kernelpanicsoft.archie.Archie
import net.kernelpanicsoft.archie.data.ADataGeneratorPlatform
import net.kernelpanicsoft.archie.events.AEvents
import net.kernelpanicsoft.archie.gametest.AGameTestPlatform
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
		AEvents += MOD
		if (ADataGeneratorPlatform.isDataGen)
			ArchieTestDatagen.init()
		if (AGameTestPlatform.isGameTest)
			ArchieTestGameTest.init()
		BlockRegistry.init()
		ItemRegistry.init()
		TileRegistry.init()
		GuiRegistry.init()
		// Gated the same way as ArchieTestGameTest.init() above, for the same reason: this is a
		// dev-only test fixture, not something real gameplay needs registered. Referencing
		// DataAttachmentTestFixtures here also runs its property initializers (which queue up its
		// attachments) before init() actually registers them.
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