package net.kernelpanicsoft.archie.mixin.neoforge.client.gui;

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
 * Wraps the {@code GuiGraphics#renderItem} call with the depth override
 * {@code AbstractContainerScreenMixin#archie$adjustSlotLayer} computed just before it.
 * NeoForge's own patched {@code AbstractContainerScreen} moves this call out of vanilla's
 * {@code renderSlot} into its own {@code renderSlotContents} extension point (added between
 * NeoForge 20.6.5-beta and 21.1.80), so this targets that method instead - see the Fabric
 * module's own copy of this class, which targets {@code renderSlot} directly.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenItemRenderMixin<T extends AbstractContainerMenu> extends Screen implements MenuAccess<T>
{
    protected AbstractContainerScreenItemRenderMixin(Component title)
    {
        super(title);
    }

    @Redirect(method = "renderSlotContents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;renderItem(Lnet/minecraft/world/item/ItemStack;III)V"))
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
