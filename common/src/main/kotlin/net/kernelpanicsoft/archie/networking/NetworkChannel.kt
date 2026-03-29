@file:OptIn(InternalSerializationApi::class)

package net.kernelpanicsoft.archie.networking

import dev.architectury.networking.NetworkManager
import dev.architectury.utils.GameInstance
import kotlinx.serialization.*
import net.kernelpanicsoft.archie.serialization.SerializationManager
import net.kernelpanicsoft.archie.serialization.streamCodec
import net.minecraft.core.RegistryAccess
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.network.protocol.game.ClientboundBundlePacket
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerChunkCache
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.ChunkPos
import kotlin.reflect.KClass

/**
 * A function type that handles a received packet of type [T] along with its [IPacketContext].
 *
 * @param T The packet data class type.
 */
typealias PacketHandler<T> = (T, IPacketContext) -> Unit

/**
 * Internal payload wrapper that carries an index into the registered packet list plus the
 * CBOR-encoded packet bytes. Using a single payload type per channel keeps the number of
 * registered Architectury payload types small.
 */
@Serializable
internal data class Payload(
    @kotlinx.serialization.Contextual
    val id: ResourceLocation,
    val index: Int,
    val data: ByteArray,
) : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload?> =
        CustomPacketPayload.Type(id)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Payload
        if (index != other.index) return false
        if (!data.contentEquals(other.data)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = index
        result = 31 * result + data.contentHashCode()
        return result
    }
}

internal val PayloadCodec = Payload.serializer().streamCodec

/**
 * Manages the registration and sending of strongly-typed, serialization-backed network packets.
 *
 * A single [NetworkChannel] can handle any number of server-bound and client-bound packet
 * types. All packets are serialized with CBOR via kotlinx.serialization.
 *
 * Packet classes **must** be Kotlin data classes annotated with `@Serializable`.
 *
 * ### Example
 * ```kotlin
 * val CHANNEL = NetworkChannel(Archie["main"])
 *
 * @Serializable
 * data class SyncDataPacket(val value: Int)
 *
 * // During mod init:
 * CHANNEL.clientbound(SyncDataPacket::class) { packet, ctx ->
 *     // handle on client
 * }
 * CHANNEL.register()
 *
 * // Sending:
 * CHANNEL.toPlayer(player, SyncDataPacket(42))
 * ```
 *
 * @param id The unique [ResourceLocation] identifier for this channel.
 */
@Suppress("unused")
@OptIn(ExperimentalSerializationApi::class)
class NetworkChannel(private val id: ResourceLocation) {
    private val clientPacketId = CustomPacketPayload.Type<Payload>(id.withSuffix("_client"))
    private val serverPacketId = CustomPacketPayload.Type<Payload>(id.withSuffix("_server"))

    private val serverClasses = mutableListOf<KClass<*>>()
    private val clientClasses = mutableListOf<KClass<*>>()

    private val serverboundHandlers = mutableListOf<PacketHandler<*>>()
    private val clientboundHandlers = mutableListOf<PacketHandler<*>>()

    /**
     * Registers a server-bound packet type and its handler.
     *
     * The handler is invoked on the server when a client sends a packet of class [klass].
     *
     * @param T The packet data class type.
     * @param klass The [KClass] of the packet. Must be a data class with `@Serializable`.
     * @param handler The handler invoked on the receiving side.
     * @throws IllegalArgumentException if [klass] is not a data class, lacks a serializer, or is already registered.
     */
    fun <T : Any> serverbound(klass: KClass<T>, handler: PacketHandler<T>) {
        require(klass.isData) { "Only data classes can be used as packets" }
        require(klass.serializerOrNull() != null) { "Data class doesn't have a serializer. Did you forget to add @Serializable?" }
        require(serverClasses.find { it == klass } == null) { "Packet is already registered" }
        serverboundHandlers.add(handler)
        serverClasses.add(klass)
    }

    /**
     * Registers a client-bound packet type and its handler.
     *
     * The handler is invoked on the client when the server sends a packet of class [klass].
     *
     * @param T The packet data class type.
     * @param klass The [KClass] of the packet. Must be a data class with `@Serializable`.
     * @param handler The handler invoked on the receiving side.
     * @throws IllegalArgumentException if [klass] is not a data class, lacks a serializer, or is already registered.
     */
    fun <T : Any> clientbound(klass: KClass<T>, handler: PacketHandler<T>) {
        require(klass.isData) { "Only data classes can be used as packets." }
        require(klass.serializerOrNull() != null) { "Data class doesn't have a serializer. Did you forget to add @Serializable?" }
        require(clientClasses.find { it == klass } == null) { "Packet is already registered" }
        clientboundHandlers.add(handler)
        clientClasses.add(klass)
    }

    /**
     * Sends one or more packets from the client to the server.
     *
     * @param packets The packets to send. All must have been registered via [serverbound].
     * @throws IllegalArgumentException if no packets are provided.
     * @throws IllegalStateException if a packet type was not registered.
     */
    fun <T : Any> toServer(vararg packets: T) {
        require(packets.isNotEmpty()) { "You need to specify one or more packets to send" }
        packets.map {
            @Suppress("UNCHECKED_CAST")
            val klass = serverClasses.find { x -> x == it::class } as? KClass<T>
                ?: throw IllegalStateException("Trying to send a packet to server but it hasn't registered the packet and its handler")
            val index = serverClasses.indexOf(klass)
            val bytes = SerializationManager.cbor.encodeToByteArray(klass.serializer(), it)

            Payload(id.withSuffix("_client"), index, bytes)
        }.forEach { NetworkManager.sendToServer(it) }
    }

    /**
     * Sends one or more packets from the server to a specific [player].
     *
     * @param player The target [ServerPlayer].
     * @param packets The packets to send. All must have been registered via [clientbound].
     * @throws IllegalArgumentException if no packets are provided.
     */
    fun <T : Any> toPlayer(player: ServerPlayer, vararg packets: T) {
        require(packets.isNotEmpty()) { "You need to specify one or more packets to send" }
        createPayloads(packets).forEach { NetworkManager.sendToPlayer(player, it) }
    }

    /**
     * Sends one or more packets from the server to a list of [players].
     *
     * @param players The list of target [ServerPlayer]s.
     * @param packets The packets to send.
     * @throws IllegalArgumentException if no packets are provided.
     */
    fun <T : Any> toPlayers(players: List<ServerPlayer>, vararg packets: T) {
        require(packets.isNotEmpty()) { "You need to specify one or more packets to send" }
        createPayloads(packets).forEach { NetworkManager.sendToPlayers(players, it) }
    }

    /**
     * Sends one or more packets from the server to **all** connected players.
     *
     * @param packets The packets to send.
     * @throws IllegalArgumentException if no packets are provided.
     * @throws IllegalStateException if called from the client side.
     */
    fun <T : Any> toAllPlayers(vararg packets: T) {
        require(packets.isNotEmpty()) { "You need to specify one or more packets to send" }
        val server = GameInstance.getServer()
            ?: throw IllegalStateException("Cannot send clientbound payloads on the client")
        createPayloads(packets).forEach { NetworkManager.sendToPlayers(server.playerList.players, it) }
    }

    /**
     * Sends one or more packets to all players currently in the given [level] (dimension).
     *
     * @param level The [ServerLevel] whose players should receive the packets.
     * @param packets The packets to send.
     * @throws IllegalArgumentException if no packets are provided.
     */
    fun <T : Any> toPlayersInDimension(level: ServerLevel, vararg packets: T) {
        require(packets.isNotEmpty()) { "You need to specify one or more packets to send" }
        createPayloads(packets).forEach { NetworkManager.sendToPlayers(level.players(), it) }
    }

    /**
     * Sends one or more packets to all players within [radius] blocks of the given coordinates
     * in [level], optionally excluding [exclude].
     *
     * @param level The [ServerLevel] to broadcast within.
     * @param exclude A [ServerPlayer] to exclude, or `null` to include all nearby players.
     * @param x The X coordinate of the broadcast origin.
     * @param y The Y coordinate of the broadcast origin.
     * @param z The Z coordinate of the broadcast origin.
     * @param radius The broadcast radius in blocks.
     * @param packets The packets to send.
     * @throws IllegalArgumentException if no packets are provided.
     */
    fun <T : Any> toNearPlayers(
        level: ServerLevel,
        exclude: ServerPlayer? = null,
        x: Double,
        y: Double,
        z: Double,
        radius: Double,
        vararg packets: T,
    ) {
        require(packets.isNotEmpty()) { "You need to specify one or more packets to send" }
        createPayloads(packets).also {
            level.server.playerList.broadcast(
                exclude, x, y, z, radius, level.dimension(),
                makeClientboundPacket(it as CustomPacketPayload),
            )
        }
    }

    /**
     * Sends one or more packets to all players tracking [entity] (i.e., the entity is loaded
     * on their client).
     *
     * @param entity The entity being tracked.
     * @param self Whether to also send the packet to the entity itself if it is a [ServerPlayer].
     * @param packets The packets to send.
     * @throws IllegalArgumentException if no packets are provided.
     * @throws IllegalStateException if called from the client side.
     */
    fun <T : Any> toPlayersTrackingEntity(entity: Entity, self: Boolean = false, vararg packets: T) {
        require(packets.isNotEmpty()) { "You need to specify one or more packets to send" }
        createPayloads(packets).also {
            val chunk = entity.level().chunkSource as? ServerChunkCache
                ?: throw IllegalStateException("Cannot send clientbound payloads on the client")
            if (self) chunk.broadcastAndSend(entity, makeClientboundPacket(it as CustomPacketPayload))
            else chunk.broadcast(entity, makeClientboundPacket(it as CustomPacketPayload))
        }
    }

    /**
     * Sends one or more packets to all players tracking chunk [pos] in [level].
     *
     * @param level The [ServerLevel] containing the chunk.
     * @param pos The [ChunkPos] of the chunk being tracked.
     * @param packets The packets to send.
     */
    fun <T : Any> toPlayersTrackingChunk(level: ServerLevel, pos: ChunkPos, vararg packets: T) =
        toPlayers(level.chunkSource.chunkMap.getPlayers(pos, false), *packets)

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> createPayloads(packets: Array<out T>): List<Payload> {
        return packets.map {
            val klass = clientClasses.find { x -> x == it::class } as? KClass<T>
                ?: throw IllegalStateException("Trying to send a packet to clients but client hasn't registered the packet and its handler")
            val index = clientClasses.indexOf(klass)
            val bytes = SerializationManager.cbor.encodeToByteArray(klass.serializer(), it)
            Payload(id.withSuffix("_server"), index, bytes)
        }
    }

    private fun makeClientboundPacket(vararg payloads: CustomPacketPayload): Packet<*> {
        return if (payloads.size == 1) ClientboundCustomPayloadPacket(payloads.first())
        else ClientboundBundlePacket(payloads.map { ClientboundCustomPayloadPacket(it) })
    }

    /**
     * Registers this channel with the Architectury networking layer.
     *
     * Must be called once during mod initialization (before any packets are sent or received).
     * Both [serverbound] and [clientbound] handlers should be registered before calling this.
     */
    @Suppress("UNCHECKED_CAST")
    fun register() {
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, serverPacketId, PayloadCodec) { payload, ctx ->
            val klass = clientClasses.getOrNull(payload.index)
                ?: throw NoSuchElementException("No class was found on the clientside. Did you forget to do clientbound?")
            val handler = clientboundHandlers.getOrNull(payload.index) as? PacketHandler<Any>
                ?: throw NoSuchElementException("No handler was found on the clientside. Did you forget to do clientbound?")
            val msg = SerializationManager.cbor.decodeFromByteArray(klass.serializer(), payload.data)
            handler(msg, object : IPacketContext {
                override val player: Player get() = ctx.player
                override val registryAccess: RegistryAccess get() = ctx.registryAccess()
            })
        }

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, clientPacketId, PayloadCodec) { payload, ctx ->
            val klass = serverClasses.getOrNull(payload.index)
                ?: throw NoSuchElementException("No class was found on the serverside. Did you forget to do serverbound?")
            val handler = serverboundHandlers.getOrNull(payload.index) as? PacketHandler<Any>
                ?: throw NoSuchElementException("No handler was found on the serverside. Did you forget to do serverbound?")
            val msg = SerializationManager.cbor.decodeFromByteArray(klass.serializer(), payload.data)
            handler(msg, object : IPacketContext {
                override val player: Player get() = ctx.player
                override val registryAccess: RegistryAccess get() = ctx.registryAccess()
            })
        }
    }
}
