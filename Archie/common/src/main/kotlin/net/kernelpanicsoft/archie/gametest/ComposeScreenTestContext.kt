package net.kernelpanicsoft.archie.gametest

import dev.architectury.registry.menu.ExtendedMenuProvider
import dev.architectury.registry.menu.MenuRegistry
import net.kernelpanicsoft.archie.gui.ComposeContainerScreen
import net.kernelpanicsoft.archie.gui.LayerManagerProvider
import net.kernelpanicsoft.archie.gui.layer.Layer
import net.kernelpanicsoft.archie.gui.layout.LayoutNode
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import org.apache.commons.lang3.function.FailableConsumer
import org.apache.commons.lang3.function.FailableFunction

/** Selects which [Layer] a [ComposeScreenTestContext] node lookup searches. */
enum class LayerSelector {
    /** The frontmost layer (a modal/dialog if one is open, otherwise the base screen). */
    Top,

    /** The screen's original base layer, regardless of any modals stacked on top of it. */
    Base,
}

/** A resolved [LayoutNode] handle, scoped to a single [ComposeScreenTestContext.node] block. */
@Suppress("unused")
class TestNodeScope internal constructor(
    private val context: ClientGameTestContext,
    val node: LayoutNode,
) {
    /** Clicks the center of this node's on-screen bounds. */
    fun click(button: Int = 0) {
        val (x, y) = context.computeOnClient {
            val (nx, ny) = node.absoluteCoords
            (nx + node.width / 2.0) to (ny + node.height / 2.0)
        }
        context.getInput().click(x, y, button)
    }
}

/**
 * Kotlin-idiomatic access to a [ComposeContainerScreen]'s layer/node tree from a client game
 * test, replacing manual `layerManager.layers...findNode(...)` bookkeeping with a small
 * receiver-block DSL.
 */
@Suppress("unused")
class ComposeScreenTestContext<S : LayerManagerProvider> internal constructor(
    val context: ClientGameTestContext,
    val screen: S,
) {
    /** The number of layers currently on the stack (1 = no modal open). */
    val layerCount: Int get() = context.computeOnClient { screen.layerManager.layers.size }

    /** The frontmost [Layer] (a modal/dialog if one is open, otherwise the base screen). */
    val topLayer: Layer get() = layer(layer = LayerSelector.Top)
    /** The screen's original base [Layer], regardless of any modals stacked on top of it. */
    val baseLayer: Layer get() = layer(layer = LayerSelector.Base)

    private fun resolveLayer(selector: LayerSelector): Layer? = when (selector) {
        LayerSelector.Top -> screen.layerManager.top
        LayerSelector.Base -> screen.layerManager.layers.firstOrNull()
    }

    private fun resolveNode(name: String, layer: LayerSelector): LayoutNode? =
        resolveLayer(layer)?.findNode(name)

    /** Checks whether a node named [name] currently exists, without waiting for it. */
    fun hasNode(name: String, layer: LayerSelector = LayerSelector.Top): Boolean =
        context.computeOnClient { resolveNode(name, layer) != null }

    /** Resolves [layer] to a [Layer] immediately, without waiting. Fails if it doesn't currently exist. */
    fun layer(layer: LayerSelector = LayerSelector.Top): Layer = context.computeOnClient { resolveLayer(layer) ?: error("Layer not found") }

    /** Resolves the layer at stack position [index] immediately, without waiting. Fails if it doesn't currently exist. */
    fun layer(index: Int): Layer = context.computeOnClient { screen.layerManager.layers.getOrNull(index) ?: error("Layer not found") }

    /** Waits for a layer to appear at stack position [index], then runs [block] against it. */
    fun waitForLayer(index: Int, block: Layer.() -> Unit = {}) {
        context.waitFor { screen.layerManager.layers.getOrNull(index) != null }
        val resolved = context.computeOnClient { screen.layerManager.layers.getOrNull(index) }
            ?: context.fail("Layer '$index' not found (testId=${context.testId})")
        return resolved.block()
    }

    /** Waits for a node named [name] to appear on this specific [Layer], then runs [block] against it. */
    fun <R> Layer.node(
        name: String,
        timeout: Int = ClientGameTestContext.DEFAULT_TIMEOUT,
        block: TestNodeScope.() -> R,
    ): R {
        context.waitFor { findNode(name) != null }
        val resolved = context.computeOnClient { findNode(name) }
            ?: context.fail("Node '$name' not found (testId=${context.testId})")
        return TestNodeScope(context, resolved).block()
    }

    /**
     * Waits for a node named [name] to appear on [layer] (default: the topmost layer), then
     * runs [block] against it. Fails the test if the node doesn't appear within [timeout] ticks.
     */
    fun <R> node(
        name: String,
        layer: LayerSelector = LayerSelector.Top,
        timeout: Int = ClientGameTestContext.DEFAULT_TIMEOUT,
        block: TestNodeScope.() -> R,
    ): R {
        context.waitFor({ _ -> resolveNode(name, layer) != null }, timeout)
        val resolved = context.computeOnClient { resolveNode(name, layer) }
            ?: context.fail("Node '$name' not found on ${layer.name.lowercase()} layer (testId=${context.testId})")
        return TestNodeScope(context, resolved).block()
    }
}

/** Reified convenience for [ClientGameTestContext.waitForScreen] that resolves [S]'s [Class] automatically. */
inline fun <reified S> ClientGameTestContext.waitForScreen(noinline block: ComposeScreenTestContext<S>.() -> Unit = {}) where S : Screen, S : LayerManagerProvider = waitForScreen(S::class.java, block)

/** Waits until the client-side player entity exists, then returns it. */
fun ClientGameTestContext.waitForPlayer(): LocalPlayer = waitFor { client -> client.player != null }.let { computeOnClient { client -> client.player!! } }

/** Waits until a block entity of type [T] exists at [pos] on the client, then returns the server-side instance. */
inline fun <reified T : BlockEntity> TestSingleplayerContext.waitForTile(pos: BlockPos): T
{
    clientContext.waitFor { client -> client.level?.getBlockEntity(pos) is T }
    return server.computeOnServer { minecraftServer ->
        val player = minecraftServer.playerList.players.first()
        val level = player.level()
        val tile = level.getBlockEntity(pos) as? T
        tile ?: error("Tile not found at $pos")
    }
}

/** Places [state] at [pos], waits for its [BlockEntity] of type [T] to exist, then opens its menu for the test's player. */
inline fun <reified T> TestSingleplayerContext.placeTileAndOpenMenu(pos: BlockPos, state: BlockState) where T : BlockEntity, T : ExtendedMenuProvider
{
    server.runOnServer { minecraftServer ->
        val player = minecraftServer.playerList.players.first()
        val level = player.level()
        level.setBlockAndUpdate(pos, state)
    }
    val tile = waitForTile<T>(pos)
    server.runOnServer { minecraftServer ->
        val player = minecraftServer.playerList.players.first()
        MenuRegistry.openExtendedMenu(player, tile)
    }
}

/** Combines [placeTileAndOpenMenu] and [waitForScreen]: places [state], opens its menu, then waits for [S] and runs [block]. */
inline fun <reified T, reified S> TestSingleplayerContext.placeTileAndWaitForScreen(pos: BlockPos, state: BlockState, noinline block: ComposeScreenTestContext<S>.() -> Unit = {}) where T : BlockEntity, T : ExtendedMenuProvider, S : Screen, S : LayerManagerProvider
{
    placeTileAndOpenMenu<T>(pos, state)
    clientContext.waitForScreen<S>(block)
}
