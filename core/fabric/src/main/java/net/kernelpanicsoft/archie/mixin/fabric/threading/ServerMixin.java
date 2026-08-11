package net.kernelpanicsoft.archie.mixin.fabric.threading;

import net.kernelpanicsoft.archie.gametest.ThreadingImpl;
import net.kernelpanicsoft.archie.gametest.ADedicatedServerPlatformInternal;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Injects ThreadingImpl.onServerTick() into MinecraftServer tick cycle for GameTest coordination.
 * Allows ThreadingImpl to coordinate server-side task execution with client-side gametest thread.
 */
@Mixin(MinecraftServer.class)
public class ServerMixin {
    @Inject(method = "runServer", at = @At("HEAD"))
    private void archie$onRunServerStart(CallbackInfo ci) {
        ADedicatedServerPlatformInternal.captureRunningServer((MinecraftServer) (Object) this);
        ThreadingImpl.onServerRunStart();
    }

    @Inject(method = "runServer", at = @At("RETURN"))
    private void archie$onRunServerStop(CallbackInfo ci) {
        ThreadingImpl.onServerRunStop();
    }

    @Inject(method = "tickServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;tickChildren(Ljava/util/function/BooleanSupplier;)V", shift = At.Shift.BEFORE))
    private void archie$onServerTick(CallbackInfo ci) {
        ThreadingImpl.onServerTick();
    }
}

