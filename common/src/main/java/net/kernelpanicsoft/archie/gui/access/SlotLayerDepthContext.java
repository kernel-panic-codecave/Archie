package net.kernelpanicsoft.archie.gui.access;

/**
 * Tracks when slot rendering overrides the default GUI depth so downstream
 * render calls (like GuiGraphics#renderItem) can adjust their transforms.
 */
public final class SlotLayerDepthContext
{
    private static final ThreadLocal<Integer> ACTIVE = ThreadLocal.withInitial(() -> 0);

    private SlotLayerDepthContext()
    {
    }

    public static void push()
    {
        ACTIVE.set(ACTIVE.get() + 1);
    }

    public static void pop()
    {
        int current = ACTIVE.get();
        if (current <= 1)
        {
            ACTIVE.remove();
            return;
        }
        ACTIVE.set(current - 1);
    }

    public static boolean isActive()
    {
        return ACTIVE.get() > 0;
    }
}

