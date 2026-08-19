package net.kernelpanicsoft.archie.mixin.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.kernelpanicsoft.archie.gui.access.SlotLayerDepthProvider;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenDepthMixin
{
    /**
     * Vanilla toggles the depth test off and on again around each individual item render inside
     * the slot loop ({@code GuiGraphics#renderItem}'s own internal 3D render), disabling it once
     * more when the last item finishes so labels/tooltips draw flat. Re-enabling it here after
     * every such call keeps items belonging to different {@link SlotLayerDepthProvider} layers
     * depth-sorted against each other across the whole slot loop, not just within one item's own
     * render - but by itself this only ever turns depth testing back ON, with nothing turning it
     * back OFF again once the loop as a whole is done. {@link #archie$restoreVanillaDepthState}
     * is the matching turn-it-back-off half.
     */
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;disableDepthTest()V", shift = At.Shift.AFTER))
    private void archie$restoreDepth(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci)
    {
        if (this instanceof SlotLayerDepthProvider)
        {
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
        }
    }

    /**
     * Restores vanilla's own expected depth state right before {@code renderLabels} - the title/
     * inventory-label text, any tooltip drawn afterward, and any Compose layer rendered on top of
     * this screen (modals, dropdowns) all expect depth testing off, the same as vanilla itself
     * would have left it here without {@link #archie$restoreDepth}'s per-item re-enable. Without
     * this, whichever of those draws last simply loses the depth test against whatever Z value the
     * slot loop's last item left in the depth buffer, rather than actually painting over it.
     */
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderLabels(Lnet/minecraft/client/gui/GuiGraphics;II)V"))
    private void archie$restoreVanillaDepthState(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci)
    {
        if (this instanceof SlotLayerDepthProvider)
        {
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
        }
    }
}

