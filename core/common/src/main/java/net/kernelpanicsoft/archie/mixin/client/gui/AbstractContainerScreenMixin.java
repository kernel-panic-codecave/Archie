package net.kernelpanicsoft.archie.mixin.client.gui;

import net.kernelpanicsoft.archie.Archie;
import net.kernelpanicsoft.archie.gui.access.SlotHighlightClipProvider;
import net.kernelpanicsoft.archie.gui.access.SlotLayerDepthContext;
import net.kernelpanicsoft.archie.gui.access.SlotLayerDepthProvider;
import net.kernelpanicsoft.archie.gui.layout.IntRect;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Debug(export = true)
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin<T extends AbstractContainerMenu> extends Screen implements MenuAccess<T>
{
    @Unique
    private Float archie$slotDepthOverride;

    protected AbstractContainerScreenMixin(Component title)
    {
        super(title);
    }

    @ModifyArgs(method = "renderSlot", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"))
    private void archie$adjustSlotLayer(Args args, GuiGraphics guiGraphics, Slot slot)
    {
        float originalZ = args.<Float>get(2);
        float adjustedZ = originalZ;
        archie$slotDepthOverride = null;
        if (this instanceof SlotLayerDepthProvider provider)
        {
            Float custom = provider.slotRenderLayerOffset(slot);
            archie$slotDepthOverride = custom;
            if (custom != null)
            {
                adjustedZ = custom;
                Archie.LOGGER.debug("Adjusting slot layer depth for {} to {}", slot, custom);
            }
        }
        args.set(2, adjustedZ);
    }

    @Redirect(method = "renderSlot", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;renderItem(Lnet/minecraft/world/item/ItemStack;III)V"))
    private void archie$wrapSlotItemRender(GuiGraphics guiGraphics, ItemStack stack, int x, int y, int seed)
    {
        boolean pushed = false;
        if (archie$slotDepthOverride != null)
        {
            SlotLayerDepthContext.push(archie$slotDepthOverride);
            pushed = true;
        }
        try
        {
            guiGraphics.renderItem(stack, x, y, seed);
        }
        finally
        {
            if (pushed)
            {
                SlotLayerDepthContext.pop();
            }
            archie$slotDepthOverride = null;
        }
    }

    /**
     * Vanilla's per-slot hover highlight is drawn via a static helper that only takes the
     * slot's raw x/y/blitOffset - there's no per-slot instance override point to clip it the
     * way {@link #archie$adjustSlotLayer} clips the item icon, so this redirects the call
     * site directly instead.
     */
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderSlotHighlight(Lnet/minecraft/client/gui/GuiGraphics;III)V"))
    private void archie$clipSlotHighlight(GuiGraphics guiGraphics, int x, int y, int blitOffset)
    {
        IntRect clip = null;
        if (this instanceof SlotHighlightClipProvider provider)
        {
            clip = provider.slotHighlightClipRect(x, y);
            if (clip == null)
            {
                return;
            }
        }
        if (clip != null)
        {
            guiGraphics.enableScissor(clip.getMinX(), clip.getMinY(), clip.getMaxX(), clip.getMaxY());
        }
        try
        {
            AbstractContainerScreen.renderSlotHighlight(guiGraphics, x, y, blitOffset);
        }
        finally
        {
            if (clip != null)
            {
                guiGraphics.disableScissor();
            }
        }
    }
}
