package net.kernelpanicsoft.archie.networking

import net.minecraft.client.Minecraft
import net.minecraft.core.RegistryAccess
import net.minecraft.world.entity.player.Player

/**
 * Provides contextual information available when handling a received network packet.
 *
 * Implementations are created by [NetworkChannel] when dispatching received payloads to
 * their registered handlers. Both server-bound and client-bound handlers receive an instance
 * of this interface, giving them access to the receiving player, the registry, and (on the
 * client side) the [Minecraft] instance.
 */
interface IPacketContext {
    /**
     * The player associated with the packet. On the server this is the sending [net.minecraft.server.level.ServerPlayer];
     * on the client this is the local player.
     */
    val player: Player

    /**
     * The [RegistryAccess] for the current connection, providing access to dynamic registries.
     */
    val registryAccess: RegistryAccess

    /**
     * The client-side [Minecraft] instance.
     *
     * Only safe to access from client-bound packet handlers. Calling this from a server-bound
     * handler will throw an [IllegalStateException] because the dedicated server has no
     * [Minecraft] instance.
     */
    val minecraft: Minecraft get() = Minecraft.getInstance()
}
