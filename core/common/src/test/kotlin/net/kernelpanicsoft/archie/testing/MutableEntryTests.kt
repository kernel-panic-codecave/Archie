package net.kernelpanicsoft.archie.testing

import net.kernelpanicsoft.archie.util.MutableEntry
import net.kernelpanicsoft.archie.util.toMutableEntry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MutableEntryTests {
    @Test
    fun testMutableEntryCreation() {
        val entry = MutableEntry("key", "value")
        assertEquals("key", entry.key)
        assertEquals("value", entry.value)
    }

    @Test
    fun testMutableEntryKeyMutation() {
        val entry = MutableEntry("original", 42)
        entry.key = "modified"
        assertEquals("modified", entry.key)
        assertEquals(42, entry.value)
    }

    @Test
    fun testMutableEntryValueMutation() {
        val entry = MutableEntry("key", 10)
        entry.value = 20
        assertEquals("key", entry.key)
        assertEquals(20, entry.value)
    }

    @Test
    fun testPairToMutableEntry() {
        val pair = Pair("pairKey", "pairValue")
        val entry = pair.toMutableEntry()
        assertEquals("pairKey", entry.key)
        assertEquals("pairValue", entry.value)
    }

    @Test
    fun testMapEntryToMutableEntry() {
        val map = mapOf("mapKey" to 100)
        val mapEntry = map.entries.first()
        val mutableEntry = mapEntry.toMutableEntry()
        assertEquals("mapKey", mutableEntry.key)
        assertEquals(100, mutableEntry.value)
    }

    @Test
    fun testMutableEntryEquality() {
        val entry1 = MutableEntry("key", "value")
        val entry2 = MutableEntry("key", "value")
        assertEquals(entry1, entry2)
    }

    @Test
    fun testMutableEntryToString() {
        val entry = MutableEntry("test", 123)
        val str = entry.toString()
        assertTrue("test" in str && "123" in str) { "toString should contain key and value" }
    }
}

