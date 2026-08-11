package net.kernelpanicsoft.archie.mixin.neoforge.client.gui;

import net.kernelpanicsoft.archie.gui.access.SlotHighlightClipProvider;
import net.kernelpanicsoft.archie.gui.layout.IntRect;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * NeoForge-specific counterpart to AbstractContainerScreenMixin#archie$clipSlotHighlight: NeoForge
 * moved the per-slot highlight draw out of the plain static {@code renderSlotHighlight(GuiGraphics,
 * int, int, int)} helper vanilla's {@code render} calls directly, replacing that call site with a
 * new protected instance overload {@code renderSlotHighlight(GuiGraphics, Slot, int, int, float)}
 * (added between NeoForge 20.6.5-beta and 21.1.80) that computes the slot's own pixel position and
 * highlight color before delegating to a *different* static overload. The clip rect this mixin
 * applies is keyed on the highlight's actual pixel position, so it reads that from the Slot
 * directly (slot.x/slot.y) rather than from this overload's int/int params, which are mouseX/mouseY
 * here, not the highlight position.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenSlotHighlightMixin<T extends AbstractContainerMenu> extends Screen implements MenuAccess<T>
{
    protected AbstractContainerScreenSlotHighlightMixin(Component title)
    {
        super(title);
    }

    @Invoker("renderSlotHighlight")
    abstract void archie$callRenderSlotHighlight(GuiGraphics guiGraphics, Slot slot, int mouseX, int mouseY, float partialTick);

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderSlotHighlight(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/inventory/Slot;IIF)V"))
    private void archie$clipSlotHighlight(AbstractContainerScreen<T> self, GuiGraphics guiGraphics, Slot slot, int mouseX, int mouseY, float partialTick)
    {
        IntRect clip = null;
        if (this instanceof SlotHighlightClipProvider provider)
        {
            clip = provider.slotHighlightClipRect(slot.x, slot.y);
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
            archie$callRenderSlotHighlight(guiGraphics, slot, mouseX, mouseY, partialTick);
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
