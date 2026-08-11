package net.kernelpanicsoft.archie.mixin.neoforge.gametest;

import net.kernelpanicsoft.archie.Archie;
import net.kernelpanicsoft.archie.gametest.platform.AGameTestPlatform;
import net.kernelpanicsoft.archie.gametest.AGameTestRegistrationBridge;
import net.kernelpanicsoft.archie.gametest.VerboseTestReporter;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GlobalTestReporter;
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
        Mod mod = AGameTestRegistrationBridge.getTestClassToMod().get(method.getDeclaringClass());

        if (gameTest.template().contains(":"))
        {
            ResourceLocation template = ResourceLocation.parse(gameTest.template());
            cir.setReturnValue(template.getNamespace());
            cir.cancel();
            return;
        }

        // NoOpGameTest (the fallback placeholder used when a mod has zero real registered tests
        // for the current side) never goes through AGameTestPlatform.register, so it's never in
        // AGameTestRegistrationBridge.getTestClassToMod() - it's always Archie's own infrastructure
        // regardless of which mod's invocation triggered it, so default to Archie.MOD's id.
        cir.setReturnValue(mod != null ? mod.getModId() : Archie.MOD_ID);
        cir.cancel();
    }

    /**
     * Always suppresses vanilla's own "namespace." infix (see NeoForge's {@code GameTestHooks#
     * turnMethodIntoTestFunction}) - it unconditionally wraps the result with {@code
     * getTemplateNamespace(method) + ":"} regardless of this method's return value, so any
     * additional infixing here would only ever produce a malformed structure id for Archie's own
     * (bare-path) templates.
     */
    @Inject(method = "prefixGameTestTemplate(Ljava/lang/reflect/Method;)Z", at = @At("HEAD"), cancellable = true)
    private static void prefixGameTestTemplateMixin(Method method, CallbackInfoReturnable<Boolean> cir)
    {
        cir.setReturnValue(false);
        cir.cancel();
    }

    @Inject(method = "registerGametests()V", at = @At(value = "INVOKE", target = "Lnet/neoforged/fml/ModLoader;postEvent(Lnet/neoforged/bus/api/Event;)V"))
    private static void registerGametests(CallbackInfo ci)
    {
        if (AGameTestPlatform.INSTANCE.isGameTest())
        {
            Archie.LOGGER.info("Registering GameTests");
            GlobalTestReporter.replaceWith(VerboseTestReporter.INSTANCE);
            AGameTestRegistrationBridge.addEventHandlers();
        }
    }
}
