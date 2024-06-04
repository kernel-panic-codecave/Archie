package com.withertech.archie.mixin.fabric;

import com.terraformersmc.modmenu.ModMenu;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.withertech.archie.config.ArchieConfigPlatform;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(ModMenu.class)
public class ModMenuMixin
{
	@Shadow(remap = false)
	@Final
	private static Map<String, ConfigScreenFactory<?>> configScreenFactories;

	@Inject(remap = false, method = "onInitializeClient()V", at = @At(value = "INVOKE", target = "Lnet/fabricmc/loader/api/FabricLoader;getEntrypointContainers(Ljava/lang/String;Ljava/lang/Class;)Ljava/util/List;"))
	public void onInitializeClient(CallbackInfo ci)
	{
		ArchieConfigPlatform.screenHandlers.forEach((key, value) ->
				configScreenFactories.put(key.getModId(), (screen) -> value.invoke().setParentScreen(screen).build()));
	}
}
