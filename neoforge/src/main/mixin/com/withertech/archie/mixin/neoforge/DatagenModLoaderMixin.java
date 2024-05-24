package com.withertech.archie.mixin.neoforge;

import com.withertech.archie.Archie;
import com.withertech.archie.data.ArchieDataGeneratorPlatform;
import com.withertech.archie.data.neoforge.ArchieDataGeneratorPlatformImpl;
import net.neoforged.neoforge.data.loading.DatagenModLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;

@Mixin(DatagenModLoader.class)
class DatagenModLoaderMixin
{
	@Inject(method = "begin(Ljava/util/Set;Ljava/nio/file/Path;Ljava/util/Collection;Ljava/util/Collection;Ljava/util/Set;ZZZZZZLjava/lang/String;Ljava/io/File;)V", at = @At(value = "INVOKE", target = "Lnet/neoforged/fml/ModLoader;runEventGenerator(Ljava/util/function/Function;)V"))
	private static void addEventHandlers(Set<String> mods, Path path, Collection<Path> inputs, Collection<Path> existingPacks, Set<String> existingMods, boolean serverGenerators, boolean clientGenerators, boolean devToolGenerators, boolean reportsGenerator, boolean structureValidator, boolean flat, String assetIndex, File assetsDir, CallbackInfo ci)
	{
		if (ArchieDataGeneratorPlatform.isDataGen())
		{
			Archie.LOGGER.info("Registering DataGen Handlers");
			ArchieDataGeneratorPlatformImpl.addEventHandlers();
		}
	}
}