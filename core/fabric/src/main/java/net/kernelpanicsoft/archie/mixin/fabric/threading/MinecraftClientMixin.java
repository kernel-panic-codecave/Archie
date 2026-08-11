package net.kernelpanicsoft.archie.mixin.fabric.threading;

import net.kernelpanicsoft.archie.gametest.platform.ThreadingImpl;
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

    @Inject(method = "runTick(Z)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;runAllTasks()V"))
    private void archie$preRunTasks(CallbackInfo ci) {
        ThreadingImpl.preRunTasks();
    }

    @Inject(method = "runTick(Z)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;runAllTasks()V", shift = At.Shift.AFTER))
    private void archie$postRunTasks(CallbackInfo ci) {
        ThreadingImpl.postRunTasks();
    }

    @Inject(method = "delayCrashRaw", at = @At("HEAD"))
    private void archie$onDelayCrashRaw(CallbackInfo ci) {
        ThreadingImpl.setGameCrashed();
    }

    @Inject(method = "emergencySaveAndCrash", at = @At("HEAD"))
    private void archie$onEmergencySaveAndCrash(CallbackInfo ci) {
        ThreadingImpl.setGameCrashed();
    }
}



