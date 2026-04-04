package net.kernelpanicsoft.archie.testing

import net.kernelpanicsoft.archie.util.getReflection
import net.kernelpanicsoft.archie.util.setReflection
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ReflectionUtilsTests {
    private class TestClass {
        @Suppress("unused")
        private var privateField: String = "initial"

        @Suppress("unused")
        private var numberField: Int = 42

        fun getPrivateField(): String = privateField
    }

    @Test
    fun testGetReflection() {
        val obj = TestClass()
        val value: String = obj.getReflection("privateField")
        assertEquals("initial", value)
    }

    @Test
    fun testSetReflection() {
        val obj = TestClass()
        obj.setReflection("privateField", "modified")
        val retrieved: String = obj.getReflection("privateField")
        assertEquals("modified", retrieved)
    }

    @Test
    fun testReflectionWithDifferentType() {
        val obj = TestClass()
        val value: Int = obj.getReflection("numberField")
        assertEquals(42, value)
    }

    @Test
    fun testSetReflectionWithDifferentType() {
        val obj = TestClass()
        obj.setReflection("numberField", 99)
        val retrieved: Int = obj.getReflection("numberField")
        assertEquals(99, retrieved)
    }

    @Test
    fun testReflectionModifiesObjectState() {
        val obj = TestClass()
        assertEquals("initial", obj.getPrivateField())
        obj.setReflection("privateField", "changed")
        assertEquals("changed", obj.getPrivateField())
    }

    @Test
    fun testReflectionNonExistentFieldThrows() {
        val obj = TestClass()
        assertThrows(NoSuchFieldException::class.java) {
            obj.getReflection<TestClass, String>("nonexistent")
        }
    }
}

