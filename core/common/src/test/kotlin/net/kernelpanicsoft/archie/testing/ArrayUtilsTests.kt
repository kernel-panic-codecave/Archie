package net.kernelpanicsoft.archie.testing

import net.kernelpanicsoft.archie.util.buildArray
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ArrayUtilsTests {
    @Test
    fun testBuildArrayCreatesCorrectArray() {
        val array = buildArray<Int> {
            add(1)
            add(2)
            add(3)
        }
        assertEquals(3, array.size)
        assertEquals(1, array[0])
        assertEquals(2, array[1])
        assertEquals(3, array[2])
    }

    @Test
    fun testBuildArrayWithCapacity() {
        val array = buildArray(5) {
            add("a")
            add("b")
        }
        assertEquals(2, array.size)
        assertEquals("a", array[0])
        assertEquals("b", array[1])
    }

    @Test
    fun testBuildArrayWithEmptyList() {
        val array = buildArray<String> {
            // Empty
        }
        assertEquals(0, array.size)
    }

    @Test
    fun testBuildArrayPreservesOrder() {
        val array = buildArray {
            add(5)
            add(4)
            add(3)
            add(2)
            add(1)
        }
        assertEquals(5, array[0])
        assertEquals(4, array[1])
        assertEquals(3, array[2])
        assertEquals(2, array[3])
        assertEquals(1, array[4])
    }
}

