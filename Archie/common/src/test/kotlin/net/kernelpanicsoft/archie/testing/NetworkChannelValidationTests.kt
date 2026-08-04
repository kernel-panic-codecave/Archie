package net.kernelpanicsoft.archie.testing

import net.kernelpanicsoft.archie.networking.NetworkChannel
import net.kernelpanicsoft.archie.util.rem
import net.minecraft.resources.ResourceLocation
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class NetworkChannelValidationTests {
    private class NotDataClass(val value: Int)
    private data class DataWithoutSerializer(val value: Int)

    @kotlinx.serialization.Serializable
    private data class ValidPacket(val value: Int)

    @Test
    fun testRejectsNonDataClass() {
        val channel = NetworkChannel("archie" % "gametest_non_data")
        assertThrows(IllegalArgumentException::class.java) {
            channel.serverbound<NotDataClass> { _, _ -> }
        }
    }

    @Test
    fun testRejectsDataWithoutSerializer() {
        val channel = NetworkChannel("archie" % "gametest_no_serializer")
        assertThrows(IllegalArgumentException::class.java) {
            channel.clientbound<DataWithoutSerializer> { _, _ -> }
        }
    }

    @Test
    fun testRejectsDuplicateRegistrations() {
        val channel = NetworkChannel("archie" % "gametest_duplicate")
        channel.clientbound<ValidPacket> { _, _ -> }
        assertThrows(IllegalArgumentException::class.java) {
            channel.clientbound<ValidPacket> { _, _ -> }
        }
    }
}
