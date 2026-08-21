package net.kernelpanicsoft.archie.gui.composables.containers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.serializer
import net.benwoodworth.knbt.NbtCompound
import net.benwoodworth.knbt.NbtTag
import net.benwoodworth.knbt.addNbtCompound
import net.benwoodworth.knbt.buildNbtCompound
import net.benwoodworth.knbt.buildNbtList
import net.kernelpanicsoft.archie.serialization.SerializationManager
import net.kernelpanicsoft.archie.serialization.decodeFromNbtTagRootless
import net.kernelpanicsoft.archie.serialization.encodeToNbtTagRootless
import net.kernelpanicsoft.archie.serialization.fromMinecraft
import net.kernelpanicsoft.archie.serialization.toMinecraft
import net.minecraft.nbt.CompoundTag

/**
 * Serializes every [TreeNode] this [TreeRegistry] holds into a [CompoundTag] - node id/data via
 * [keySerializer]/[dataSerializer], plus each node's own [TreeNode.children]/[TreeNode.parents]
 * ids and the registry's own [TreeRegistry.roots]. See [loadTree] for the reverse.
 */
fun <K : Any, V, B : NodeBuilder<K, V, B>> TreeRegistry<K, V, B>.saveToTag(keySerializer: KSerializer<K>, dataSerializer: KSerializer<V>): CompoundTag {
    val nbt = SerializationManager.nbt
    val keyListSerializer = ListSerializer(keySerializer)
    fun encodeKeys(ids: List<K>): NbtTag = nbt.encodeToNbtTagRootless(keyListSerializer, ids)

    val nodesTag = buildNbtList<NbtCompound> {
        for (node in allNodes()) {
            addNbtCompound {
                put("id", nbt.encodeToNbtTagRootless(keySerializer, node.id))
                put("data", nbt.encodeToNbtTagRootless(dataSerializer, node.data))
                put("children", encodeKeys(node.children.map { it.id }))
                put("depends_on", encodeKeys(node.parents.map { it.id }))
            }
        }
    }
    return buildNbtCompound {
        put("roots", encodeKeys(roots.map { it.id }))
        put("nodes", nodesTag)
    }.toMinecraft
}

/** Reified convenience over [TreeRegistry.saveToTag] resolving [K]/[V]'s own serializers via [SerializationManager.module]. */
inline fun <reified K : Any, reified V, B : NodeBuilder<K, V, B>> TreeRegistry<K, V, B>.saveToTag(): CompoundTag =
    saveToTag(SerializationManager.module.serializer(), SerializationManager.module.serializer())

/** [tagValue] read back as a list of decoded [K]s via [keySerializer] - a missing key reads back as an empty list. */
private fun <K : Any> decodeKeys(tagValue: NbtTag?, keySerializer: KSerializer<K>): List<K> {
    tagValue ?: return emptyList()
    return SerializationManager.nbt.decodeFromNbtTagRootless(ListSerializer(keySerializer), tagValue)
}

/**
 * Rebuilds a [TreeRegistry] from a [CompoundTag] [TreeRegistry.saveToTag] produced, wiring nodes
 * back into the same [TreeNode.children]/[TreeNode.parents]/[TreeRegistry.roots] shape. [factory]
 * is never invoked during loading itself - only kept so the registry can keep growing afterward.
 */
fun <K : Any, V, B : NodeBuilder<K, V, B>> loadTree(tag: CompoundTag, factory: (K) -> B, keySerializer: KSerializer<K>, dataSerializer: KSerializer<V>): TreeRegistry<K, V, B> {
    val nbt = SerializationManager.nbt
    val registry = TreeRegistry(factory)
    val root = tag.fromMinecraft
    val nodesTag = root["nodes"] as? Iterable<*> ?: emptyList<NbtTag>()

    val childIdsByNode = mutableMapOf<K, List<K>>()
    val dependsOnIdsByNode = mutableMapOf<K, List<K>>()
    for (entry in nodesTag) {
        val nodeCompound = entry as NbtCompound
        val id = nbt.decodeFromNbtTagRootless(keySerializer, nodeCompound.getValue("id"))
        val data = nbt.decodeFromNbtTagRootless(dataSerializer, nodeCompound.getValue("data"))
        registry.register(id, data)
        childIdsByNode[id] = decodeKeys(nodeCompound["children"], keySerializer)
        dependsOnIdsByNode[id] = decodeKeys(nodeCompound["depends_on"], keySerializer)
    }
    for ((id, childIds) in childIdsByNode) {
        val node = requireNotNull(registry[id])
        for (childId in childIds) node.children += requireNotNull(registry[childId])
    }
    for ((id, parentIds) in dependsOnIdsByNode) {
        val node = requireNotNull(registry[id])
        for (parentId in parentIds) node.parents += requireNotNull(registry[parentId])
    }
    for (rootId in decodeKeys(root["roots"], keySerializer)) registry.addRoot(requireNotNull(registry[rootId]))

    return registry
}

/** Reified convenience over [loadTree] resolving [K]/[V]'s own serializers via [SerializationManager.module]. */
inline fun <reified K : Any, reified V, B : NodeBuilder<K, V, B>> loadTree(tag: CompoundTag, noinline factory: (K) -> B): TreeRegistry<K, V, B> =
    loadTree(tag, factory, SerializationManager.module.serializer(), SerializationManager.module.serializer())
