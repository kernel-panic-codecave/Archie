package net.kernelpanicsoft.archie.mixin.fabric.client.gui;

import net.kernelpanicsoft.archie.gui.access.DeferredSlotHighlights;
import net.kernelpanicsoft.archie.gui.access.ScreenOverlayDeferral;
import net.kernelpanicsoft.archie.gui.access.SlotHighlightClipProvider;
import net.kernelpanicsoft.archie.gui.layout.IntRect;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Vanilla's per-slot hover highlight is drawn via a static helper that only takes the slot's raw
 * x/y/blitOffset - there's no per-slot instance override point to clip it the way
 * AbstractContainerScreenMixin#archie$adjustSlotLayer clips the item icon, so this redirects the
 * call site directly instead. NeoForge's own patched {@code AbstractContainerScreen} calls a
 * different, newer overload here, hence a per-loader mixin - see the NeoForge module's own copy
 * of this class.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenSlotHighlightMixin<T extends AbstractContainerMenu> extends Screen implements MenuAccess<T>, DeferredSlotHighlights
{
    protected AbstractContainerScreenSlotHighlightMixin(Component title)
    {
        super(title);
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderSlotHighlight(Lnet/minecraft/client/gui/GuiGraphics;III)V"))
    private void archie$clipSlotHighlight(GuiGraphics guiGraphics, int x, int y, int blitOffset)
    {
        if (this instanceof ScreenOverlayDeferral deferral && deferral.defersScreenOverlays())
        {
            // Held back rather than drawn here - see ScreenOverlayDeferral. Only the args are kept;
            // the clip is recomputed at replay time, when the layout it depends on has settled.
            this.archie$deferredHighlightX = x;
            this.archie$deferredHighlightY = y;
            this.archie$deferredBlitOffset = blitOffset;
            this.archie$hasDeferredHighlight = true;
            return;
        }
        this.archie$drawSlotHighlight(guiGraphics, x, y, blitOffset);
    }

    /** The one hovered slot's highlight, held back this frame - at most one, since only one slot is hovered. */
    @Unique
    private boolean archie$hasDeferredHighlight;
    @Unique
    private int archie$deferredHighlightX;
    @Unique
    private int archie$deferredHighlightY;
    @Unique
    private int archie$deferredBlitOffset;

    @Override
    public void archieDrawDeferredSlotHighlights(GuiGraphics guiGraphics)
    {
        if (!this.archie$hasDeferredHighlight)
        {
            return;
        }
        this.archie$hasDeferredHighlight = false;
        this.archie$drawSlotHighlight(guiGraphics, this.archie$deferredHighlightX, this.archie$deferredHighlightY, this.archie$deferredBlitOffset);
    }

    @Unique
    private void archie$drawSlotHighlight(GuiGraphics guiGraphics, int x, int y, int blitOffset)
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
