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
class TestNodeScope(
    val context: ClientGameTestContext,
    val node: LayoutNode,
) {
    /**
     * Waits for compose to settle before reading [node]'s on-screen bounds - without this, a
     * node's very first interaction (right after [ComposeScreenTestContext.waitForScreen]/
     * [ComposeScreenTestContext.node] finds it) can read a transient pre-layout-settle position
     * (e.g. before a wrapping Scrollable's initial measure has stabilized), computing a click/
     * hover target that no longer matches the node's real bounds one frame later - silently
     * missing the node (no ENTER/PRESS ever dispatches) rather than failing loudly.
     */
    private fun centerCoords(): Pair<Double, Double> {
        context.waitForComposeIdle()
        return context.computeOnClient {
            val (nx, ny) = node.absoluteCoords
            (nx + node.width / 2.0) to (ny + node.height / 2.0)
        }
    }

    /** Clicks the center of this node's on-screen bounds. */
    fun click(button: Int = 0) {
        val (x, y) = centerCoords()
        context.getInput().click(x, y, button)
    }

    /** Moves the cursor to the center of this node's on-screen bounds, without clicking - e.g. to assert a [TextureStates.HOVERED] visual state. */
    fun hover() {
        val (x, y) = centerCoords()
        context.getInput().setCursor(x, y)
    }

    /**
     * Presses and releases [keyCode]. Key input in this framework targets the active screen as
     * a whole, not a specific node - [click] (or [hover], for a text field that focuses on
     * hover) the target first if it needs focus.
     */
    fun pressKey(keyCode: Int, scanCode: Int = 0, modifiers: Int = 0) {
        context.getInput().pressKey(keyCode, scanCode, modifiers)
    }

    /** Types each character of [value] as if typed at the keyboard. See [pressKey] re: focus. */
    fun type(value: String) {
        context.getInput().typeChars(value)
    }

    /** Moves the cursor to this node's center, then scrolls there. See [TestInput.scroll]. */
    fun scroll(x: Double = 0.0, y: Double = 1.0) {
        val (cx, cy) = centerCoords()
        context.getInput().setCursor(cx, cy)
        context.getInput().scroll(x, y)
    }

    /**
     * The [TextureStates] key this node's [net.kernelpanicsoft.archie.gui.layout.Renderer] most
     * recently selected to draw (e.g. `"hovered"`), or `null` if this node doesn't render a
     * theme-state-driven visual. See [net.kernelpanicsoft.archie.gui.nodes.UINode.renderState].
     */
    val renderState: String? get() = context.computeOnClient { node.renderState }

    /**
     * Fails unless this node's [renderState] equals [expected].
     *
     * Reads [renderState] exactly once - the same value is used both to decide pass/fail and
     * (on failure) in the default message. Re-reading it live inside the message lambda instead
     * would race further recomposition between the comparison and the (lazily-evaluated, only
     * on failure) message being built, showing a misleading "got <newValue>" that no longer
     * matches whatever value the comparison actually failed on.
     */
    fun assertRenderState(
        expected: String,
        message: (() -> String)? = null,
    ) {
        val actual = renderState
        context.assertEquals(expected, actual, message ?: {
            "Expected node '${node.name}' render state <$expected>, got <$actual> (testId=${context.testId})"
        })
    }

    /** This node's direct children's names, in composition order. */
    fun childNames(): List<String> = context.computeOnClient { node.children.map { it.name } }

    /** Fails unless this node's direct children's names, in order, equal [expected]. */
    fun assertChildNames(vararg expected: String) {
        val actual = childNames()
        context.assertEquals(expected.toList(), actual) {
            "Expected node '${node.name}' children <${expected.toList()}>, got <$actual> (testId=${context.testId})\n${describeTree()}"
        }
    }

    /** Whether a descendant named [name] exists anywhere in this node's subtree, without failing. */
    fun hasDescendant(name: String): Boolean = context.computeOnClient { node.findNode(name) != null }

    /** Fails unless a descendant named [name] exists anywhere in this node's subtree. */
    fun assertHasDescendant(name: String) {
        context.assertTrue(hasDescendant(name)) {
            "Expected node '${node.name}' to have a descendant named '$name' (testId=${context.testId})\n${describeTree()}"
        }
    }

    /**
     * Fails if this node or any descendant has a non-positive width or height - the "zero-size
     * widget" class of layout bug, catchable without any pixel comparison.
     */
    fun assertAllDescendantsSized() {
        val unsized = context.computeOnClient { node.flatten().filter { it.width <= 0 || it.height <= 0 } }
        context.assertTrue(unsized.isEmpty()) {
            "Expected every node under '${node.name}' to have a positive size, but found zero-sized: " +
                unsized.joinToString { "${it.name}(${it.width}x${it.height})" } +
                " (testId=${context.testId})\n${describeTree()}"
        }
    }

    /** A recursive dump of this node's subtree (name, nested per child), for failure messages. */
    fun describeTree(): String = context.computeOnClient { node.toString() }

    /**
     * All descendants of this node named [name], in depth-first order - the escape hatch for
     * [node] (which requires exactly one match) when a subtree legitimately has several, e.g.
     * every "Button" in a dialog's action row.
     */
    fun nodes(name: String): List<LayoutNode> = context.computeOnClient { node.findAllNodes(name) }

    /**
     * Waits for a descendant named [name] within this node's subtree (not the whole layer) to
     * appear, then runs [block] against it. Fails if [name] doesn't appear within [timeout] ticks.
     */
    fun <R> node(
        name: String,
        timeout: Int = ClientGameTestContext.DEFAULT_TIMEOUT,
        block: TestNodeScope.() -> R,
    ): R {
        context.waitFor({ _ -> node.findNode(name) != null }, timeout)
        val resolved = context.computeOnClient { node.findNode(name) }
            ?: context.fail("Node '$name' not found under '${node.name}' (testId=${context.testId})")
        return TestNodeScope(context, resolved).block()
    }

    operator fun <R> LayoutNode.invoke(block: TestNodeScope.() -> R): R
    {
        return TestNodeScope(context, this).block()
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

    /** Wraps an already-resolved [LayoutNode] (e.g. one indexed out of [TestNodeScope.nodes]) for interaction, without constructing a [TestNodeScope] by hand. */
    operator fun <R> LayoutNode.invoke(block: TestNodeScope.() -> R): R = TestNodeScope(context, this).block()
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
