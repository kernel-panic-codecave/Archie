package net.kernelpanicsoft.archie.mixin.neoforge.threading;

import net.kernelpanicsoft.archie.gametest.ThreadingImpl;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftClientMixin {
    @Inject(method = "run", at = @At("HEAD"))
    private void archie$onRunStart(CallbackInfo ci) {
        ThreadingImpl.onClientRunStart();
    }

    @Inject(method = "run", at = @At("RETURN"))
    private void archie$onRunStop(CallbackInfo ci) {
        ThreadingImpl.onClientRunStop();
    }

    @Inject(method = "cleanUpAfterCrash", at = @At("HEAD"))
    private void archie$onCrashCleanup(CallbackInfo ci) {
        ThreadingImpl.setGameCrashed();
    }
}

