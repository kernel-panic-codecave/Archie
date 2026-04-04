package net.kernelpanicsoft.archie.mixin.fabric.threading;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.server.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Main.class)
public class MainMixin {
    @WrapWithCondition(method = "main", remap = false, at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Util;startTimerHack()V", remap = true))
    private static boolean dontStartAnotherTimerHack() {
        return false;
    }
}

