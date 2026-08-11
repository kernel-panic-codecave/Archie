package net.kernelpanicsoft.archie.mixin.neoforge.gametest;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestRegistry;
import net.minecraft.gametest.framework.TestFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.lang.reflect.Method;

/**
 * `GameTestRegistry#turnMethodIntoTestFunction` unconditionally wraps its result with
 * {@code GameTestHooks.getTemplateNamespace(method) + ":"}, regardless of what {@code
 * GameTestHooks#prefixGameTestTemplate} returns - {@code GameTestHooksMixin}'s overrides of those
 * two methods alone can't prevent an already-namespaced {@code @GameTest(template = ...)} string
 * (like Archie's own {@code EMPTY} constant) from coming out double-prefixed. Fix up the result
 * directly instead: {@link TestFunction} is a record, so every other field can just be copied
 * from vanilla's own (otherwise correct) result.
 */
@Mixin(GameTestRegistry.class)
public abstract class GameTestRegistryMixin
{
    @ModifyReturnValue(method = "turnMethodIntoTestFunction", at = @At("RETURN"))
    private static TestFunction archie$fixNamespacedTemplate(TestFunction original, Method method)
    {
        GameTest gameTest = method.getAnnotation(GameTest.class);
        if (gameTest == null || !gameTest.template().contains(":"))
        {
            return original;
        }

        return new TestFunction(
            original.batchName(), original.testName(), gameTest.template(), original.rotation(),
            original.maxTicks(), original.setupTicks(), original.required(), original.manualOnly(),
            original.maxAttempts(), original.requiredSuccesses(), original.skyAccess(), original.function()
        );
    }
}
