package net.kernelpanicsoft.archie.gametest.internal.tests

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import net.kernelpanicsoft.archie.gametest.ClientGameTest
import net.kernelpanicsoft.archie.gametest.ClientGameTestContext
import net.kernelpanicsoft.archie.gui.composables.containers.NodeBuilder
import net.kernelpanicsoft.archie.gui.composables.containers.loadTree
import net.kernelpanicsoft.archie.gui.composables.containers.saveToTag
import net.kernelpanicsoft.archie.gui.composables.containers.tree

@Serializable
private data class TreeSerializationTestNode(val label: String, val locked: Boolean = false)

private class TreeSerializationTestNodeBuilder(id: String) : NodeBuilder<String, TreeSerializationTestNode, TreeSerializationTestNodeBuilder>(id) {
    var label: String = ""
    var locked: Boolean = false
    override fun build() = TreeSerializationTestNode(label, locked)
}

private data class RoundTripResult(
    val rootIds: List<String>,
    val miningChildIds: List<String>,
    val smithingParentIds: List<String>,
    val labels: Map<String, String>,
    val lockedFlags: Map<String, Boolean>,
)

/**
 * GameTest coverage for [net.kernelpanicsoft.archie.gui.composables.containers.TreeRegistry.saveToTag]/
 * [loadTree] - round-trips a small forest through a real [net.minecraft.nbt.CompoundTag] and
 * asserts its structure/data survived exactly.
 */
@Suppress("unused")
class TreeSerializationGameTest {
    @ClientGameTest
    fun ClientGameTestContext.testSaveAndLoadRoundTripsStructureAndData() {
        val result = computeOnClient {
            val original = tree(::TreeSerializationTestNodeBuilder) {
                node("gathering") {
                    label = "Gathering"
                    children {
                        node("mining") {
                            label = "Mining"
                            children {
                                node("smithing") {
                                    label = "Smithing"
                                    dependsOn("gathering")
                                }
                                node("rare_ore") {
                                    label = "Rare Ore"
                                    locked = true
                                }
                            }
                        }
                    }
                }
                node("combat") {
                    label = "Combat"
                }
            }

            val tag = original.saveToTag(String.serializer(), TreeSerializationTestNode.serializer())
            val loaded = loadTree(tag, ::TreeSerializationTestNodeBuilder, String.serializer(), TreeSerializationTestNode.serializer())

            val ids = listOf("gathering", "mining", "smithing", "rare_ore", "combat")
            RoundTripResult(
                rootIds = loaded.roots.map { it.id }.sorted(),
                miningChildIds = loaded["mining"]!!.children.map { it.id }.sorted(),
                smithingParentIds = loaded["smithing"]!!.parents.map { it.id }.sorted(),
                labels = ids.associateWith { loaded[it]!!.data.label },
                lockedFlags = ids.associateWith { loaded[it]!!.data.locked },
            )
        }

        assertEquals(listOf("combat", "gathering"), result.rootIds) { "Expected both top-level nodes to survive as roots" }
        assertEquals(listOf("rare_ore", "smithing"), result.miningChildIds) { "Expected mining's own two children to survive" }
        assertEquals(listOf("gathering", "mining"), result.smithingParentIds) { "Expected smithing's own nesting parent (mining) and its extra dependsOn(\"gathering\") edge to both survive" }
        assertEquals("Gathering", result.labels["gathering"])
        assertEquals("Mining", result.labels["mining"])
        assertEquals("Smithing", result.labels["smithing"])
        assertEquals("Rare Ore", result.labels["rare_ore"])
        assertEquals("Combat", result.labels["combat"])
        assertTrue(result.lockedFlags["rare_ore"] == true) { "Expected rare_ore's own locked flag to survive as true" }
        assertTrue(result.lockedFlags["gathering"] == false) { "Expected gathering's own locked flag to survive as false" }
    }
}
