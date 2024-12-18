package com.withertech.archie.mixin.fabric;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.withertech.archie.Archie;
import com.withertech.archie.gametest.AGameTestPlatform;
import net.fabricmc.fabric.impl.gametest.FabricGameTestModInitializer;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@SuppressWarnings("UnstableApiUsage")
@Mixin(FabricGameTestModInitializer.class)
class FabricGameTestModInitializerMixin
{
	@SuppressWarnings("UnresolvedLocalCapture")
	@Inject(method = "onInitialize()V", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/fabricmc/loader/api/FabricLoader;getEntrypointContainers(Ljava/lang/String;Ljava/lang/Class;)Ljava/util/List;"), remap = false)
	public void onInitialize(CallbackInfo ci, @Local LocalRef<List<EntrypointContainer<Object>>> entrypointContainers)
	{
		if (AGameTestPlatform.INSTANCE.isGameTest())
		{
			Archie.LOGGER.info("Registering GameTests");
			AGameTestPlatform.addEntrypoints(entrypointContainers);
		}
	}
}