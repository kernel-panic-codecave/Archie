package net.kernelpanicsoft.archie.gui

import net.kernelpanicsoft.archie.gui.blockentity.BlockEntityStateManager
import net.kernelpanicsoft.archie.gui.blockentity.ComposeBlockEntityState
import net.kernelpanicsoft.archie.gui.blockentity.getOrCreateBlockEntityState
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.level.block.entity.BlockEntity

/**
 * Base class for [BlockEntity]-backed Compose container menus.
 *
 * See [ComposeContainerMenuBase] for slot pre-registration/positioning behavior, shared with
 * [net.kernelpanicsoft.archie.gui.item.ComposeItemContainerMenu] - this class only adds the
 * [BlockEntity]-specific pieces: holding [tile], deriving [blockEntityState] from its position,
 * and registering it with [BlockEntityStateManager].
 *
 * ### Subclassing
 * ```kotlin
 * class MyMenu(id: Int, inventory: Inventory, tile: MyTile) :
 *     ComposeBlockContainerMenu<MyTile, MyMenu>(MY_MENU_TYPE, id, inventory, tile) {
 *
 *     override fun registerSlotHandlers() {
 *         handler("inventory", tile.items)   // ties the "inventory" slot group to the storage
 *     }
 * }
 * ```
 *
 * @param T    The [BlockEntity] type that owns the storage.
 * @param SELF The concrete menu subclass (self-referential for the [MenuType]).
 * @param type The registered [MenuType] for this menu.
 * @param id   The container id assigned by the server.
 * @param playerInventory The opening player's inventory.
 * @param tile The block entity instance.
 */
abstract class ComposeBlockContainerMenu<T : BlockEntity, SELF : ComposeBlockContainerMenu<T, SELF>>(
    type: MenuType<SELF>,
    id: Int,
    playerInventory: Inventory,
    protected val tile: T,
) : ComposeContainerMenuBase<SELF>(type, id, playerInventory) {

    val blockEntityState: ComposeBlockEntityState = getOrCreateBlockEntityState(tile.blockPos)

    init
    {
        // Must run here, in this class's own init - not from ComposeContainerMenuBase's, which
        // would dispatch into onMenuOpened() before `tile` (this class's own constructor
        // property) is actually assigned. See onMenuOpened's KDoc.
        onMenuOpened()
    }

    override fun onMenuOpened()
    {
        BlockEntityStateManager.registerBlockEntity(tile)
        if (!level.isClientSide)
        {
            BlockEntityStateManager.addTrackedPlayer(tile, player as ServerPlayer)
        }
    }

    override fun onMenuClosed(player: Player)
    {
        BlockEntityStateManager.unregisterBlockEntity(tile)
        if (!level.isClientSide)
        {
            BlockEntityStateManager.removeTrackedPlayer(tile, player as ServerPlayer)
        }
    }

    override fun stillValid(player: Player): Boolean = true
}
