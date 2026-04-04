package net.kernelpanicsoft.archie.mixin.neoforge.lifecycle;

import net.kernelpanicsoft.archie.gametest.AGameTestClientHarnessInternal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Overlay;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftClientMixin {
    @Unique
    private boolean startedClientGametests = false;

    @Shadow
    @Nullable
    private Overlay overlay;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        if (!startedClientGametests && overlay == null) {
            startedClientGametests = true;
            AGameTestClientHarnessInternal.runIfNeeded(null);
        }
    }
}

