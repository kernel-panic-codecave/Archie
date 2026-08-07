package net.kernelpanicsoft.archie.gui.item

import dev.architectury.event.events.common.TickEvent
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Server-side manager for currently-open [ComposeItemContainerMenu]s: drives dirty-property sync
 * packets each tick (mirroring [net.kernelpanicsoft.archie.gui.blockentity.BlockEntityStateManager]),
 * and force-closes a menu whose [ItemContainerAccess] reports it's no longer valid - e.g. the
 * backing item was consumed/dropped while the GUI was passively open. Vanilla's own `stillValid`
 * polling only fires reactively on player-initiated clicks otherwise, so without this a stale
 * menu could sit open indefinitely against a stack that no longer exists.
 *
 * Much lighter than [net.kernelpanicsoft.archie.gui.blockentity.BlockEntityStateManager]: an
 * item-backed menu is inherently 1:1 with one player's session (unlike a block entity, which can
 * be watched by multiple players simultaneously), so there's no position-keyed registry or
 * per-menu tracked-player set needed - just the set of currently-open menus themselves.
 */
object ItemStateManager {
	private val openMenus: MutableSet<ComposeItemContainerMenu<*>> = CopyOnWriteArraySet()

	/**
	 * Registers the server tick listener that drives per-tick syncing/validity checks.
	 *
	 * Must be called once during mod init.
	 */
	fun init() {
		TickEvent.SERVER_POST.register {
			val currentTick = it.tickCount.toLong()
			openMenus.forEach { menu -> menu.tickSync(currentTick) }
		}
	}

	/** Registers [menu] for tick-driven syncing. Called from [ComposeItemContainerMenu.onMenuOpened]. */
	fun register(menu: ComposeItemContainerMenu<*>) {
		openMenus += menu
	}

	/** Unregisters [menu]. Called from [ComposeItemContainerMenu.onMenuClosed]. */
	fun unregister(menu: ComposeItemContainerMenu<*>) {
		openMenus -= menu
	}
}
