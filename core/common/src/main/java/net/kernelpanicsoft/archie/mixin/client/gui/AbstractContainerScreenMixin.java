package net.kernelpanicsoft.archie.mixin.client.gui;

import net.kernelpanicsoft.archie.Archie;
import net.kernelpanicsoft.archie.gui.access.SlotLayerDepthContext;
import net.kernelpanicsoft.archie.gui.access.SlotLayerDepthProvider;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Debug(export = true)
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin<T extends AbstractContainerMenu> extends Screen implements MenuAccess<T>
{
    protected AbstractContainerScreenMixin(Component title)
    {
        super(title);
    }

    /**
     * Computes the depth override and hands it off via {@link SlotLayerDepthContext} rather than
     * a {@code @Unique} field - the actual {@code GuiGraphics#renderItem} call this override
     * applies to is wrapped by a loader-specific mixin (NeoForge moved it out of this method into
     * its own {@code renderSlotContents}, see AbstractContainerScreenItemRenderMixin in each
     * loader module), so the two can no longer be @Unique fields on the same mixin class.
     */
    @ModifyArgs(method = "renderSlot", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"))
    private void archie$adjustSlotLayer(Args args, GuiGraphics guiGraphics, Slot slot)
    {
        float originalZ = args.<Float>get(2);
        float adjustedZ = originalZ;
        Float custom = null;
        if (this instanceof SlotLayerDepthProvider provider)
        {
            custom = provider.slotRenderLayerOffset(slot);
            if (custom != null)
            {
                adjustedZ = custom;
                Archie.LOGGER.debug("Adjusting slot layer depth for {} to {}", slot, custom);
            }
        }
        SlotLayerDepthContext.setPendingOverride(custom);
        args.set(2, adjustedZ);
    }
}
