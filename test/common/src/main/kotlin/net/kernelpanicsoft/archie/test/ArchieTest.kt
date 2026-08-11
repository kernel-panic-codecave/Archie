package net.kernelpanicsoft.archie.test

import com.mojang.logging.LogUtils
import dev.architectury.platform.Mod
import dev.architectury.platform.Platform
import net.kernelpanicsoft.archie.Archie
import net.kernelpanicsoft.archie.data.ADataGeneratorPlatform
import net.kernelpanicsoft.archie.events.ADatagenEvents
import net.kernelpanicsoft.archie.events.AGametestEvents
import net.kernelpanicsoft.archie.gametest.AGameTestPlatform
import net.kernelpanicsoft.archie.test.gametest.ArchieTestGameTest
import net.kernelpanicsoft.archie.test.gametest.DataAttachmentTestFixtures
import net.kernelpanicsoft.archie.test.gametest.CapabilityLookupTestFixtures
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
		// Must come after TileRegistry.init() (needs TestTile to actually exist), but is otherwise
		// still normal mod-init timing - see CapabilityLookupTestFixtures's KDoc for why this can't
		// be deferred to inside the @GameTest methods themselves.
		if (AGameTestPlatform.isGameTest)
			CapabilityLookupTestFixtures.init()
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