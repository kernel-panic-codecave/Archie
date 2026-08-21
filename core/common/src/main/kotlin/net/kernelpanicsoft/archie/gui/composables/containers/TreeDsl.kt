package net.kernelpanicsoft.archie.gui.composables.containers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.minecraft.resources.ResourceLocation

/** Scopes every [TreeNode]/[NodeBuilder]/[TreeScope] receiver below to its own nesting. */
@DslMarker
annotation class TreeDsl

/**
 * One node built by [tree]. [data] is the caller's own arbitrary payload. [children]/[parents]
 * hold other [TreeNode]s directly (not ids) - use [TreeRegistry.get] to look one up by id later.
 * [data]/[children]/[parents] are genuine Compose state, so mutating any of them after [tree]
 * returns recomposes [NodeTreeView] automatically.
 */
@TreeDsl
class TreeNode<K : Any, V> internal constructor(val id: K, initialData: V) {
    var data: V by mutableStateOf(initialData)
    val children: MutableList<TreeNode<K, V>> = mutableStateListOf()
    val parents: MutableList<TreeNode<K, V>> = mutableStateListOf()
}

/**
 * A node's own payload builder for [tree] - subclass for a concrete [V]; only [build] is
 * abstract. [Self] is the concrete subclass itself, so every `node(id) { ... }` block's receiver
 * exposes that subclass's own fields rather than just this base class's.
 */
@TreeDsl
abstract class NodeBuilder<K : Any, V, Self : NodeBuilder<K, V, Self>>(val id: K) {
    private val childrenBlocks = mutableListOf<TreeScope<K, V, Self>.() -> Unit>()
    internal val dependsOnNodes = mutableListOf<TreeNode<K, V>>()
    internal val dependsOnIds = mutableListOf<K>()

    /** Finalizes whatever this builder accumulated into the actual payload [TreeNode.data] ends up holding. */
    abstract fun build(): V

    /** Declares this node's own children via nested [TreeScope.node] calls. Callable more than once. */
    fun children(block: TreeScope<K, V, Self>.() -> Unit) {
        childrenBlocks += block
    }

    /** Adds [node] as an extra prerequisite of this node. */
    fun dependsOn(node: TreeNode<K, V>) {
        dependsOnNodes += node
    }

    /** Adds the node registered under [id] as an extra prerequisite - may name a node declared anywhere else in the same [tree] call. */
    fun dependsOn(id: K) {
        dependsOnIds += id
    }

    /** Runs every [children] block against [node]/[registry]. */
    internal fun runChildren(node: TreeNode<K, V>, registry: TreeRegistry<K, V, Self>) {
        for (block in childrenBlocks) TreeScope(node, registry).block()
    }
}

/**
 * [tree]'s own return value - every [TreeNode] it declared, addressable by [TreeNode.id] via
 * [get], reachable structurally from [roots]. Backed by Compose state throughout.
 */
class TreeRegistry<K : Any, V, B : NodeBuilder<K, V, B>> internal constructor(internal val factory: (K) -> B) {
    private val byId: MutableMap<K, TreeNode<K, V>> = mutableStateMapOf()
    private val mutableRoots: MutableList<TreeNode<K, V>> = mutableStateListOf()
    internal val pendingEdges = mutableListOf<Pair<TreeNode<K, V>, K>>()

    /** Every top-level [TreeScope.node] call in the [tree] that built this registry. */
    val roots: List<TreeNode<K, V>> get() = mutableRoots

    /** The node registered under [id], or `null` if none was. */
    operator fun get(id: K): TreeNode<K, V>? = byId[id]

    /** Every [TreeNode] this registry holds, [roots] and descendants alike. */
    internal fun allNodes(): Collection<TreeNode<K, V>> = byId.values

    internal fun register(id: K, data: V): TreeNode<K, V> {
        require(id !in byId) { "Duplicate tree node id: $id" }
        val node = TreeNode(id, data)
        byId[id] = node
        return node
    }

    internal fun addRoot(node: TreeNode<K, V>) {
        mutableRoots += node
    }

    /** Resolves every [pendingEdges] entry once the whole builder has run. */
    internal fun resolvePendingEdges() {
        for ((child, parentId) in pendingEdges) {
            val parent = requireNotNull(byId[parentId]) { "dependsOn(\"$parentId\") on node \"${child.id}\" names an id this tree() never declared a node for" }
            if (parent !== child && parent !in child.parents) child.parents += parent
        }
    }
}

/** The receiver [tree]'s own builder block runs with, one per nesting level. [node] is its only member. */
@TreeDsl
class TreeScope<K : Any, V, B : NodeBuilder<K, V, B>> internal constructor(private val owner: TreeNode<K, V>?, private val registry: TreeRegistry<K, V, B>) {
    /**
     * Declares one node, [id]'d for [NodeBuilder.dependsOn]/dedup purposes. [configure] runs
     * against a fresh [B]; [TreeNode.parents] ends up holding both the nesting [owner] and any
     * extra [NodeBuilder.dependsOn] edges.
     */
    fun node(id: K, configure: B.() -> Unit = {}): TreeNode<K, V> {
        val builder = registry.factory(id)
        builder.configure()
        val data = builder.build()
        val node = registry.register(id, data)
        owner?.children?.add(node)
        for (parent in builder.dependsOnNodes) if (parent !== node && parent !in node.parents) node.parents += parent
        for (parentId in builder.dependsOnIds) registry.pendingEdges += node to parentId
        builder.runChildren(node, registry)
        if (owner == null)
            registry.addRoot(node)
        else
            node.parents += owner
        return node
    }
}

/**
 * Builds a forest of [TreeNode]s as a [TreeRegistry]. A top-level [TreeScope.node] call declares
 * one of [TreeRegistry.roots]; [NodeBuilder.children] nests further nodes; [NodeBuilder.dependsOn]
 * wires an extra prerequisite. [factory] supplies a fresh [B] per [TreeScope.node] call.
 *
 * ```
 * class SkillNodeBuilder : NodeBuilder<String, SkillNode, SkillNodeBuilder>() {
 *     var label = ""
 *     var locked = false
 *     override fun build() = SkillNode(label, locked)
 * }
 *
 * val skillTree = tree(factory = { SkillNodeBuilder() }) {
 *     node("gathering") {
 *         label = "Gathering"
 *         children {
 *             node("mining") {
 *                 label = "Mining"
 *                 children {
 *                     node("smithing") { label = "Smithing"; dependsOn("gathering") }
 *                     node("rare_ore") { label = "Rare Ore"; locked = true }
 *                 }
 *             }
 *         }
 *     }
 * }
 * ```
 */
fun <K : Any, V, B : NodeBuilder<K, V, B>> tree(factory: (K) -> B, block: TreeScope<K, V, B>.() -> Unit): TreeRegistry<K, V, B> {
    val registry = TreeRegistry(factory)
    TreeScope(null, registry).block()
    registry.resolvePendingEdges()
    return registry
}

/** Shorthand for `remember { tree(factory, block) }` - builds [block]'s own forest once per composable lifetime. */
@Composable
fun <K : Any, V, B : NodeBuilder<K, V, B>> rememberTree(factory: (K) -> B, block: TreeScope<K, V, B>.() -> Unit): TreeRegistry<K, V, B> =
    remember { tree(factory, block) }

/**
 * [NodeTreeView]'s general-purpose `List<T>`/`children:`/`parents:`/`key:` surface, specialized for
 * a forest already built as a [TreeRegistry]. Every structural parameter reads [TreeNode.children]/
 * [TreeNode.parents]/[TreeNode.id] directly; a caller only supplies cosmetic per-node callbacks and
 * [content]. `dedupe` is always on, since a [TreeNode]'s own id already identifies it.
 *
 * @param roots The [TreeRegistry] [tree] returned.
 * @param visible See [NodeTreeView]'s own `visible` parameter.
 * @param backgroundParallax/[backgroundTint]/[panelTexture]/[panelVariant]/[panelContentPadding]
 *   Forwarded straight to the general [NodeTreeView] overload's own parameters of the same names.
 */
@Composable
fun <K : Any, V, B : NodeBuilder<K, V, B>> NodeTreeView(
    roots: TreeRegistry<K, V, B>,
    modifier: Modifier = Modifier,
    columnGap: Int = 26,
    rowGap: Int = 6,
    connectorStyle: (TreeNode<K, V>) -> ConnectorStyle = { ConnectorStyle.SOLID },
    connectorColor: (TreeNode<K, V>) -> Int = { 0xFF808080.toInt() },
    connectorThickness: (TreeNode<K, V>) -> Int = { 1 },
    connectorAnimation: (TreeNode<K, V>) -> ConnectorAnimation = { ConnectorAnimation.NONE },
    connectorShape: (TreeNode<K, V>) -> ConnectorShape = { ConnectorShape.ELBOW },
    connectorDirection: (TreeNode<K, V>) -> ConnectorDirection = { ConnectorDirection.CHILD_TO_PARENT },
    nodeAnimation: (TreeNode<K, V>) -> NodeAnimation = { NodeAnimation.NONE },
    rootAlignment: RootAlignment = RootAlignment.START,
    visible: (TreeNode<K, V>) -> Boolean = { true },
    onVisibilityChanged: (TreeNode<K, V>, Boolean) -> Unit = { _, _ -> },
    state: PannableCanvasState = rememberPannableCanvasState(),
    backgroundTexture: ResourceLocation? = null,
    backgroundTextureSize: Int = 32,
    backgroundTint: Int = -1,
    backgroundParallax: Float = 1f,
    panelTexture: String = "surface",
    panelVariant: String? = "inset_transparent",
    panelContentPadding: Int = 5,
    content: @Composable (TreeNode<K, V>) -> Unit,
) = NodeTreeView<TreeNode<K, V>>(
    roots = roots.roots,
    children = { it.children },
    parents = { it.parents },
    modifier = modifier,
    columnGap = columnGap,
    rowGap = rowGap,
    connectorStyle = connectorStyle,
    connectorColor = connectorColor,
    connectorThickness = connectorThickness,
    connectorAnimation = connectorAnimation,
    connectorShape = connectorShape,
    connectorDirection = connectorDirection,
    nodeAnimation = nodeAnimation,
    rootAlignment = rootAlignment,
    dedupe = true,
    key = { it.id },
    visible = visible,
    onVisibilityChanged = { node, isVisible -> onVisibilityChanged(node, isVisible) },
    state = state,
    backgroundTexture = backgroundTexture,
    backgroundTextureSize = backgroundTextureSize,
    backgroundTint = backgroundTint,
    backgroundParallax = backgroundParallax,
    panelTexture = panelTexture,
    panelVariant = panelVariant,
    panelContentPadding = panelContentPadding,
    content = content,
)
