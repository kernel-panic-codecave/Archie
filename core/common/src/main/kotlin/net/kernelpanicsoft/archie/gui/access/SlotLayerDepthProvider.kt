package net.kernelpanicsoft.archie.gui.access

import net.minecraft.world.inventory.Slot

/**
 * Allows container screens to tweak the Z depth used when vanilla renders a [Slot].
 * Returning `null` keeps Minecraft's default blit offset.
 */
interface SlotLayerDepthProvider {
    fun slotRenderLayerOffset(slot: Slot): Float? = null
}

