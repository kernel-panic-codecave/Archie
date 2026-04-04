package net.kernelpanicsoft.archie.testing

import net.kernelpanicsoft.archie.Archie
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CommonTests {
    @Test
    fun testModIdConstant() {
        assertEquals("archie", Archie.MOD_ID)
    }

    @Test
    fun testModIdNotBlank() {
        assertTrue(Archie.MOD_ID.isNotBlank())
    }
}
