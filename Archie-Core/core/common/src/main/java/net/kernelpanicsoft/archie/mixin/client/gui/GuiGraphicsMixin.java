package net.kernelpanicsoft.archie.mixin.client.gui;

import net.kernelpanicsoft.archie.Archie;
import net.kernelpanicsoft.archie.gui.access.SlotLayerDepthContext;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Debug(export = true)
@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin
{
    @Unique
    private static final float ITEM_TRANSLATE_Z = 150.0F;

    @ModifyArgs(
        method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;IIII)V",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V")
    )
    private void archie$flattenSlotItemDepth(Args args)
    {
        if (!SlotLayerDepthContext.isActive())
        {
            return;
        }
        float originalZ = args.<Float>get(2);
        float adjusted = originalZ - ITEM_TRANSLATE_Z;
        Archie.LOGGER.debug(
            "Slot depth context active: target={}, translate={} -> {}",
            SlotLayerDepthContext.currentDepth(),
            originalZ,
            adjusted
        );
        args.set(2, adjusted);
    }
}
