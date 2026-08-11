package net.kernelpanicsoft.archie.gui.item

import kotlinx.serialization.KSerializer
import net.kernelpanicsoft.archie.gui.ComposeContainerMenuBase
import net.kernelpanicsoft.archie.gui.blockentity.BlockEntityStatePacket
import net.kernelpanicsoft.archie.gui.blockentity.toSerializedValue
import net.kernelpanicsoft.archie.networking.ArchieNetworkChannel
import net.kernelpanicsoft.archie.serialization.NBTHolder
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.MenuType

/**
 * Base class for [net.minecraft.world.item.ItemStack]-backed Compose container menus - e.g. a
 * backpack/bag with its own GUI. The [ComposeBlockContainerMenu][net.kernelpanicsoft.archie.gui.ComposeBlockContainerMenu]
 * equivalent for items.
 *
 * See [ComposeContainerMenuBase] for slot pre-registration/positioning behavior, shared with
 * [net.kernelpanicsoft.archie.gui.ComposeBlockContainerMenu] - this class only adds the
 * item-specific pieces: locating the backing stack via [itemAccess], the [itemState] sync path,
 * and periodic validity checking via [ItemStateManager].
 *
 * ### Subclassing
 * ```kotlin
 * class MyBackpackMenu(id: Int, inventory: Inventory, access: ItemContainerAccess) :
 *     ComposeItemContainerMenu<MyBackpackMenu>(MY_MENU_TYPE, id, inventory, access) {
 *
 *     @Sync
 *     var progress by holder.intField()
 *
 *     override fun registerSlotHandlers() {
 *         handler("inventory", holder.itemField(27))
 *     }
 * }
 * ```
 *
 * @param SELF The concrete menu subclass (self-referential for the [MenuType]).
 * @param type The registered [MenuType] for this menu.
 * @param id The container id assigned by the server.
 * @param playerInventory The opening player's inventory.
 * @param itemAccess Locates the backing [net.minecraft.world.item.ItemStack] and reports whether
 *   this menu should stay open.
 */
abstract class ComposeItemContainerMenu<SELF : ComposeItemContainerMenu<SELF>>(
	type: MenuType<SELF>,
	id: Int,
	playerInventory: Inventory,
	protected val itemAccess: ItemContainerAccess,
) : ComposeContainerMenuBase<SELF>(type, id, playerInventory), SyncedItemHolder {

	/** Client- and server-side sync state for this menu's own `@Sync`-annotated [holder] fields. */
	val itemState: ComposeItemState = ComposeItemState(containerId)

	/**
	 * An [NBTHolder] view of the backing stack, captured **once** at construction - mirroring
	 * [net.kernelpanicsoft.archie.gui.ComposeBlockContainerMenu]'s `tile` (stable for this menu's
	 * lifetime, not re-resolved per access). Declare this menu's own `@Sync`-annotated scalar
	 * fields against it, e.g. `@Sync var progress by holder.intField()`.
	 *
	 * Unlike [itemAccess]'s own `getStack()` (re-resolved fresh every call, since slot *contents*
	 * must always reflect the live inventory slot), this menu's own bookkeeping fields behave the
	 * same way [net.kernelpanicsoft.archie.gui.ComposeBlockContainerMenu]'s fields do against
	 * `tile` - captured once, not defended against the backing stack reference being swapped out
	 * from under an already-open menu. That's an unusual scenario that isn't defended against for
	 * block-entity-backed menus either (`tile` is captured the same way there).
	 */
	protected val holder: NBTHolder = NBTHolder.item(itemAccess.getStack())

	/** Property names changed since the last [tickSync], with their serialized values ready to send. */
	private val dirtyUpdates = mutableMapOf<String, BlockEntityStatePacket.SerializedValue>()

	init
	{
		// Must run here, in this class's own init - not from ComposeContainerMenuBase's, which
		// would dispatch into onMenuOpened() before `itemAccess` (this class's own constructor
		// property) is actually assigned. See ComposeContainerMenuBase.onMenuOpened's KDoc.
		onMenuOpened()
	}

	override fun onMenuOpened()
	{
		if (!level.isClientSide)
			ItemStateManager.register(this)
	}

	override fun onMenuClosed(player: Player)
	{
		if (!level.isClientSide)
			ItemStateManager.unregister(this)
	}

	@Suppress("UNCHECKED_CAST")
	override fun <T> registerSyncedProperty(name: String, serializer: KSerializer<T>)
	{
		// Runs on both sides, at field-declaration time - independent of observeItemProperty(),
		// which only ever runs client-side inside a composable. Without this, the server never
		// learns a serializer for `name` at all unless a fresh delegate's own initial-value write
		// happens to fire onSyncedPropertyChanged first (which it doesn't for a property whose
		// value already exists on an already-populated stack).
		itemState.propertySerializers[name] = serializer as KSerializer<out Any>
	}

	override fun <T> onSyncedPropertyChanged(name: String, serializer: KSerializer<T>, value: T)
	{
		dirtyUpdates[name] = value.toSerializedValue(serializer)
	}

	/**
	 * Applies a client-sent [ItemUpdatePacket] edit: a raw, low-level write straight into
	 * [holder]'s stored data (bypassing whatever property setter owns [name], the same way
	 * [net.kernelpanicsoft.archie.gui.blockentity.BlockEntityStatePacketRegistry]'s serverbound
	 * handler calls `blockEntity.updateProperty(...)` rather than going through the property
	 * setter) - re-entering through [onSyncedPropertyChanged] here would only mark [name] dirty
	 * without ever actually persisting the new value, since that method is a notification hook,
	 * not a write path. Marks [name] dirty directly afterward so [tickSync] re-broadcasts it.
	 */
	internal fun <T> applyRemoteUpdate(name: String, serializer: KSerializer<T>, value: T)
	{
		holder.updateProperty(name, serializer, value)
		dirtyUpdates[name] = value.toSerializedValue(serializer)
	}

	/**
	 * Called once per server tick by [ItemStateManager]: force-closes this menu if [itemAccess]
	 * reports it's no longer valid, otherwise sends any accumulated [dirtyUpdates] as a single
	 * [ItemStatePacket].
	 */
	internal fun tickSync(currentTick: Long)
	{
		if (!itemAccess.stillValid(player))
		{
			player.closeContainer()
			return
		}
		if (dirtyUpdates.isEmpty()) return
		val packet = ItemStatePacket(containerId, dirtyUpdates.toMap(), currentTick)
		dirtyUpdates.clear()
		(player as? ServerPlayer)?.let { ArchieNetworkChannel.toPlayers(listOf(it), packet) }
	}

	override fun stillValid(player: Player): Boolean = itemAccess.stillValid(player)

	/** Freezes the backpack's own slot in the player's inventory while its GUI is open - see [ComposeContainerMenuBase.isPlayerSlotExcluded]. */
	override fun isPlayerSlotExcluded(index: Int): Boolean =
		(itemAccess as? PlayerInventoryItemAccess)?.slot == index
}
