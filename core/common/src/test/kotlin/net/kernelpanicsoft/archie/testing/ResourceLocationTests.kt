package net.kernelpanicsoft.archie.testing

import net.kernelpanicsoft.archie.util.div
import net.kernelpanicsoft.archie.util.rem
import net.minecraft.resources.ResourceLocation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ResourceLocationTests {
    @Test
    fun testNamespacePathOperator() {
        val id = "archie_test" % "path/value"
        assertEquals("archie_test", id.namespace)
        assertEquals("path/value", id.path)
    }

    @Test
    fun testArchieNamespaceOperatorEquivalent() {
        val id = "archie" % "main"
        assertEquals("archie", id.namespace)
        assertEquals("main", id.path)
    }

    @Test
    fun testResourceLocationDivisionOperators() {
        val base = ResourceLocation.fromNamespaceAndPath("archie", "root")
        val byString = base / "child"
        val byResource = base / ResourceLocation.fromNamespaceAndPath("other", "leaf")
        val withPrefix = "prefix" / base

        assertEquals("root/child", byString.path)
        assertEquals("root/leaf", byResource.path)
        assertEquals("prefix/root", withPrefix.path)
    }

    @Test
    fun testDivisionOperatorPreservesNamespace() {
        val base = ResourceLocation.fromNamespaceAndPath("archie", "root")
        val result = base / "child"
        assertEquals("archie", result.namespace)
    }

    @Test
    fun testDivisionOperatorWithNestedPaths() {
        val base = ResourceLocation.fromNamespaceAndPath("archie", "a")
        val result = base / "b" / "c" / "d"
        assertEquals("archie", result.namespace)
        assertEquals("a/b/c/d", result.path)
    }

    @Test
    fun testRemOperatorWithEmptyPath() {
        val id = "namespace" % ""
        assertEquals("namespace", id.namespace)
        assertEquals("", id.path)
    }

    @Test
    fun testRemOperatorWithComplexPaths() {
        val id = "my_mod" % "blocks/custom_block"
        assertEquals("my_mod", id.namespace)
        assertEquals("blocks/custom_block", id.path)
    }

    @Test
    fun testResourceLocationCombinations() {
        val id1 = "archie" % "test"
        val id2 = id1 / "sub"
        val id3 = "prefix" / id2

        assertEquals("archie", id1.namespace)
        assertEquals("test", id1.path)
        assertEquals("archie", id2.namespace)
        assertEquals("test/sub", id2.path)
        assertEquals("archie", id3.namespace)
        assertEquals("prefix/test/sub", id3.path)
    }
}
