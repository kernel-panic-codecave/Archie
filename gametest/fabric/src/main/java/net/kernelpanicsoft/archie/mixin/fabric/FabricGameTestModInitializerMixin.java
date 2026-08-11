package net.kernelpanicsoft.archie.mixin.fabric;

import net.fabricmc.fabric.impl.gametest.FabricGameTestModInitializer;
import net.kernelpanicsoft.archie.Archie;
import net.kernelpanicsoft.archie.gametest.platform.AGameTestPlatform;
import net.kernelpanicsoft.archie.gametest.VerboseTestReporter;
import net.minecraft.gametest.framework.GlobalTestReporter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@SuppressWarnings("UnstableApiUsage")
@Mixin(FabricGameTestModInitializer.class)
public interface FabricGameTestModInitializerMixin
{

	@Accessor("GAME_TEST_IDS")
    static Map<Class<?>, String> getGameTestIds() { return null; }


    @Accessor("LOGGER")
    static org.slf4j.Logger getLogger() { return null; }
}