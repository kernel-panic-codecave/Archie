package net.kernelpanicsoft.archie.mixin.client.gui;

import net.kernelpanicsoft.archie.gui.access.DeferredFloatingItems;
import net.kernelpanicsoft.archie.gui.access.FloatingItemDraw;
import net.kernelpanicsoft.archie.gui.access.ScreenOverlayDeferral;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds back the item that follows the cursor - carried, mid-drag, or snapping back - for screens
 * that asked to draw it themselves; see {@link ScreenOverlayDeferral} for why re-ordering is the
 * only fix available here.
 * <p>
 * Both {@code renderFloatingItem} call sites in {@code render} go through this one redirect, so a
 * snapback animation is deferred on the same terms as the carried stack. Unlike the slot highlight,
 * this needs no per-loader copy: {@code renderFloatingItem(GuiGraphics, ItemStack, int, int,
 * String)} is private and untouched by NeoForge, which patches only its body.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenFloatingItemMixin<T extends AbstractContainerMenu> extends Screen implements MenuAccess<T>, DeferredFloatingItems
{
    protected AbstractContainerScreenFloatingItemMixin(Component title)
    {
        super(title);
    }

    /**
     * What this frame's {@code render} held back. A list because {@code render} can reach the call
     * twice - a stack snapping back while another is carried - and cleared as it is replayed, so a
     * frame that defers and then never replays (the screen stopped deferring mid-frame) cannot leak
     * a stale draw into the next one.
     */
    @Unique
    private final List<FloatingItemDraw> archie$deferredFloatingItems = new ArrayList<>();

    @Invoker("renderFloatingItem")
    abstract void archie$callRenderFloatingItem(GuiGraphics guiGraphics, ItemStack stack, int x, int y, String text);

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderFloatingItem(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V"))
    private void archie$deferFloatingItem(AbstractContainerScreen<T> self, GuiGraphics guiGraphics, ItemStack stack, int x, int y, String text)
    {
        if (this instanceof ScreenOverlayDeferral deferral && deferral.defersScreenOverlays())
        {
            this.archie$deferredFloatingItems.add(new FloatingItemDraw(stack, x, y, text));
            return;
        }
        this.archie$callRenderFloatingItem(guiGraphics, stack, x, y, text);
    }

    @Override
    public void archieDrawDeferredFloatingItems(GuiGraphics guiGraphics)
    {
        if (this.archie$deferredFloatingItems.isEmpty())
        {
            return;
        }
        for (FloatingItemDraw draw : this.archie$deferredFloatingItems)
        {
            this.archie$callRenderFloatingItem(guiGraphics, draw.getStack(), draw.getX(), draw.getY(), draw.getText());
        }
        this.archie$deferredFloatingItems.clear();
    }
}
