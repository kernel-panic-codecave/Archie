package net.kernelpanicsoft.archie.mixin.fabric.lifecycle;

import net.kernelpanicsoft.archie.gametest.AGameTestClientHarnessInternal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Overlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import org.jetbrains.annotations.Nullable;

@Mixin(Minecraft.class)
public class MinecraftClientMixin {
    @Unique
    private boolean archie$startedClientGametests = false;

    @Shadow
    @Nullable
    private Overlay overlay;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        if (!archie$startedClientGametests && overlay == null) {
            archie$startedClientGametests = true;
            AGameTestClientHarnessInternal.runIfNeeded();
        }
    }
}

