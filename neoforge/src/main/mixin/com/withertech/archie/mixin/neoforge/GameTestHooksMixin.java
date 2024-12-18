package com.withertech.archie.mixin.neoforge;

import com.withertech.archie.Archie;
import com.withertech.archie.gametest.AGameTestPlatform;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.architectury.platform.Mod;

import java.lang.reflect.Method;

@Mixin(GameTestHooks.class)
public abstract class GameTestHooksMixin {

    @Inject(method = "getTemplateNamespace(Ljava/lang/reflect/Method;)Ljava/lang/String;", at = @At("HEAD"), cancellable = true)
    private static void getTemplateNamespaceMixin(Method method, CallbackInfoReturnable<String> cir)
    {
        GameTest gameTest = method.getAnnotation(GameTest.class);
        Mod mod = AGameTestPlatform.INSTANCE.getTestClassToMod$archie_neoforge().get(method.getDeclaringClass());

        if (gameTest.template().contains(":"))
        {
            ResourceLocation template = ResourceLocation.parse(gameTest.template());
            cir.setReturnValue(template.getNamespace());
            return;
        }

        if (mod != null)
        {
            cir.setReturnValue(mod.getModId());
            return;
        }

    }

    @Inject(method = "prefixGameTestTemplate(Ljava/lang/reflect/Method;)Z", at = @At("HEAD"), cancellable = true)
    private static void prefixGameTestTemplateMixin(Method method, CallbackInfoReturnable<Boolean> cir)
    {
        GameTest gameTest = method.getAnnotation(GameTest.class);
        if (gameTest.template().contains(":"))
        {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "registerGametests()V", at = @At(value = "INVOKE", target = "Lnet/neoforged/fml/ModLoader;postEvent(Lnet/neoforged/bus/api/Event;)V"))
    private static void registerGametests(CallbackInfo ci)
    {
        if (AGameTestPlatform.INSTANCE.isGameTest())
        {
            Archie.LOGGER.info("Registering GameTests");
            AGameTestPlatform.addEventHandlers();
        }
    }
}
