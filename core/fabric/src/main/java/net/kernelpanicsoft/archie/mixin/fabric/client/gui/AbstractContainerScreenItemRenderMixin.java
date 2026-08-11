package net.kernelpanicsoft.archie.mixin.fabric.client.gui;

import net.kernelpanicsoft.archie.gui.access.SlotLayerDepthContext;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Wraps the {@code GuiGraphics#renderItem} call inside vanilla's own {@code renderSlot} with the
 * depth override {@code AbstractContainerScreenMixin#archie$adjustSlotLayer} computed just before
 * it. NeoForge's own patched {@code AbstractContainerScreen} moves this call into a separate
 * {@code renderSlotContents} method, hence a per-loader mixin - see the NeoForge module's own
 * copy of this class.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenItemRenderMixin<T extends AbstractContainerMenu> extends Screen implements MenuAccess<T>
{
    protected AbstractContainerScreenItemRenderMixin(Component title)
    {
        super(title);
    }

    @Redirect(method = "renderSlot", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;renderItem(Lnet/minecraft/world/item/ItemStack;III)V"))
    private void archie$wrapSlotItemRender(GuiGraphics guiGraphics, ItemStack stack, int x, int y, int seed)
    {
        Float override = SlotLayerDepthContext.takePendingOverride();
        boolean pushed = false;
        if (override != null)
        {
            SlotLayerDepthContext.push(override);
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
        }
    }
}
