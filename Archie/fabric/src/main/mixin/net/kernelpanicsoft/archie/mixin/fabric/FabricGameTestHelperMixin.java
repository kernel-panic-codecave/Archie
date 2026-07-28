package net.kernelpanicsoft.archie.mixin.fabric;

import net.fabricmc.fabric.impl.gametest.FabricGameTestHelper;
import net.kernelpanicsoft.archie.gametest.AGameTestPlatformInternal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FabricGameTestHelper.class)
public class FabricGameTestHelperMixin {
    @Inject(method = "runHeadlessServer(Lnet/minecraft/world/level/storage/LevelStorageSource$LevelStorageAccess;Lnet/minecraft/server/packs/repository/PackRepository;)V", at = @At("HEAD"))
    private static void runHeadlessServer(CallbackInfo ci) {
        AGameTestPlatformInternal.registerGameTests();
    }
}
