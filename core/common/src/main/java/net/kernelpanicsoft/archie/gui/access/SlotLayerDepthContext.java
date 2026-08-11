package net.kernelpanicsoft.archie.gui.access;

import java.util.ArrayDeque;
import java.util.Deque;
import net.kernelpanicsoft.archie.Archie;

/**
 * Tracks when slot rendering overrides the default GUI depth so downstream
 * render calls (like GuiGraphics#renderItem) can adjust their transforms.
 */
public final class SlotLayerDepthContext
{
    private static final ThreadLocal<Deque<Float>> DEPTHS = ThreadLocal.withInitial(ArrayDeque::new);
    private static final ThreadLocal<Float> PENDING_OVERRIDE = new ThreadLocal<>();

    private SlotLayerDepthContext()
    {
    }

    /**
     * Hands off a depth override computed while still inside {@code renderSlot} (see
     * AbstractContainerScreenMixin) to the loader-specific mixin that wraps the actual
     * {@code GuiGraphics#renderItem} call - NeoForge moved that call out of {@code renderSlot}
     * into its own {@code renderSlotContents} method, so the two can no longer share a
     * {@code @Unique} field on one mixin class.
     */
    public static void setPendingOverride(Float depth)
    {
        PENDING_OVERRIDE.set(depth);
    }

    public static Float takePendingOverride()
    {
        Float depth = PENDING_OVERRIDE.get();
        PENDING_OVERRIDE.remove();
        return depth;
    }

    public static void push(float depth)
    {
        Deque<Float> depths = DEPTHS.get();
        depths.push(depth);
        Archie.LOGGER.debug("Slot depth push -> {} (stack size={})", depth, depths.size());
    }

    public static void pop()
    {
        Deque<Float> depths = DEPTHS.get();
        if (depths.isEmpty())
        {
            DEPTHS.remove();
            return;
        }
        Float removed = depths.pop();
        Archie.LOGGER.debug("Slot depth pop -> {} (remaining={})", removed, depths.size());
        if (depths.isEmpty())
        {
            DEPTHS.remove();
        }
    }

    public static boolean isActive()
    {
        Deque<Float> depths = DEPTHS.get();
        return !depths.isEmpty();
    }

    public static Float currentDepth()
    {
        return DEPTHS.get().peek();
    }
}
