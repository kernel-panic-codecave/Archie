package com.withertech.archie.mixin.neoforge;

import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.neoforge.gametest.GameTestHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Method;
import java.util.function.Consumer;

@Mixin(GameTestRegistry.class)
public abstract class GameTestRegistryMixin {
    @Shadow
    private static Consumer<GameTestHelper> turnMethodIntoConsumer(Method testMethod) {
        return null;
    }

    @Inject(method = "turnMethodIntoTestFunction(Ljava/lang/reflect/Method;)Lnet/minecraft/gametest/framework/TestFunction;", at = @At("HEAD"), cancellable = true)
    private static void  turnMethodIntoTestFunctionMixin(Method testMethod, CallbackInfoReturnable<TestFunction> cir)
    {
        GameTest gameTest = testMethod.getAnnotation(GameTest.class);
        if (gameTest.template().contains(":"))
        {
            ResourceLocation template = new ResourceLocation(gameTest.template());

            String s = testMethod.getDeclaringClass().getSimpleName();
            String s1 = s.toLowerCase();
            boolean prefixGameTestTemplate = GameTestHooks.prefixGameTestTemplate(testMethod);
            String s2 = (prefixGameTestTemplate ? s1 + "." : "") + testMethod.getName().toLowerCase();
            String s3 = GameTestHooks.getTemplateNamespace(testMethod) + ":" + (prefixGameTestTemplate ? s1 + "." : "") + template.getPath();
            String s4 = gameTest.batch();
            Rotation rotation = StructureUtils.getRotationForRotationSteps(gameTest.rotationSteps());

            cir.setReturnValue(new TestFunction(s4, s2, s3, rotation, gameTest.timeoutTicks(), gameTest.setupTicks(), gameTest.required(), gameTest.manualOnly(), gameTest.requiredSuccesses(), gameTest.attempts(), gameTest.skyAccess(), turnMethodIntoConsumer(testMethod)));
        }
    }
}
