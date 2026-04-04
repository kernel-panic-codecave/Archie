package net.kernelpanicsoft.archie.testing

import net.kernelpanicsoft.archie.config.v2.model.BooleanField
import net.kernelpanicsoft.archie.config.v2.model.ChoiceField
import net.kernelpanicsoft.archie.config.v2.model.ConfigCategory
import net.kernelpanicsoft.archie.config.v2.model.ConfigDocument
import net.kernelpanicsoft.archie.config.v2.model.IntField
import net.kernelpanicsoft.archie.config.v2.model.StringField
import net.kernelpanicsoft.archie.config.v2.runtime.ConfigState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ConfigV2StateTests {
    private fun createDocument(): ConfigDocument {
        return ConfigDocument(
            id = "config-v2-state-tests",
            title = "Config V2 State Tests",
            categories = listOf(
                ConfigCategory(
                    id = "general",
                    title = "General",
                    fields = listOf(
                        BooleanField(key = "enabled", title = "Enabled", defaultValue = true),
                        IntField(key = "threshold", title = "Threshold", defaultValue = 5, min = 0, max = 10),
                        ChoiceField(key = "mode", title = "Mode", defaultValue = "safe", options = listOf("safe", "fast")),
                    ),
                    children = listOf(
                        ConfigCategory(
                            id = "nested",
                            title = "Nested",
                            fields = listOf(
                                StringField(key = "nested_value", title = "Nested Value", defaultValue = "abc"),
                            ),
                        ),
                    ),
                ),
            ),
        )
    }

    @Test
    fun testSeedsDefaultsFromNestedCategories() {
        val state = ConfigState(createDocument())
        assertEquals(true, state.boolean("enabled"))
        assertEquals(5, state.int("threshold"))
        assertEquals("safe", state.string("mode"))
        assertEquals("abc", state.string("nested_value"))
    }

    @Test
    fun testSettersAndTypedGettersRoundTrip() {
        val state = ConfigState(createDocument())
        state.setBoolean("enabled", false)
        state.setInt("threshold", 8)
        state.setString("mode", "fast")
        state.setString("nested_value", "updated")

        assertEquals(false, state.boolean("enabled"))
        assertEquals(8, state.int("threshold"))
        assertEquals("fast", state.string("mode"))
        assertEquals("updated", state.string("nested_value"))
    }

    @Test
    fun testValidateReportsRangeAndChoiceViolations() {
        val state = ConfigState(createDocument())
        state.setInt("threshold", 42)
        state.setString("mode", "turbo")

        val errors = state.validate()
        assertTrue("threshold > max 10" in errors) {
            "Expected threshold max validation error, got $errors"
        }
        assertTrue("mode is not in options" in errors) {
            "Expected mode options validation error, got $errors"
        }
    }

    @Test
    fun testTypedGetterFailsForMissingOrWrongType() {
        val state = ConfigState(createDocument())
        assertThrows(IllegalStateException::class.java) {
            state.int("missing_key")
        }

        state.setString("threshold", "not-int")
        assertThrows(ClassCastException::class.java) {
            state.int("threshold")
        }
    }
}

