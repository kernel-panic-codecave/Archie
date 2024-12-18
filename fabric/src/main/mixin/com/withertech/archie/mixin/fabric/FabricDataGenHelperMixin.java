package com.withertech.archie.mixin.fabric;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.withertech.archie.Archie;
import com.withertech.archie.data.ADataGeneratorPlatform;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.impl.datagen.FabricDataGenHelper;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@SuppressWarnings("UnstableApiUsage")
@Mixin(FabricDataGenHelper.class)
class FabricDataGenHelperMixin
{
	@SuppressWarnings("UnresolvedLocalCapture")
	@Inject(remap = false, method = "runInternal()V", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/fabricmc/loader/api/FabricLoader;getEntrypointContainers(Ljava/lang/String;Ljava/lang/Class;)Ljava/util/List;"))
	private static void addEntrypoints(CallbackInfo ci, @Local LocalRef<List<EntrypointContainer<DataGeneratorEntrypoint>>> dataGeneratorInitializers)
	{
		if (ADataGeneratorPlatform.INSTANCE.isDataGen())
		{
			Archie.LOGGER.info("Registering DataGen Handlers");
			ADataGeneratorPlatform.addEntrypoints(dataGeneratorInitializers);
		}
	}
}