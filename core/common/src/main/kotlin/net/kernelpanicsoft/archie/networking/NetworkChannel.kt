@file:OptIn(InternalSerializationApi::class)

package net.kernelpanicsoft.archie.networking

import dev.architectury.networking.NetworkManager
import dev.architectury.utils.Env
import dev.architectury.utils.EnvExecutor
import dev.architectury.utils.GameInstance
import kotlinx.coroutines.Runnable
import kotlinx.serialization.*
import net.kernelpanicsoft.archie.serialization.SerializationManager
import net.kernelpanicsoft.archie.serialization.serializers.SResourceLocation
import net.kernelpanicsoft.archie.serialization.streamCodec
import net.minecraft.core.RegistryAccess
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
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
    val id: SResourceLocation,
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
 * Packet classes **must** be Kotlin data classes annotated with `@Serializable` when registered
 * via the reflective [serverbound]/[clientbound] overloads; pass an explicit [KSerializer]
 * instead (see the two-argument-serializer overloads, or [serverboundLazy]) for a type that
 * can't carry that annotation.
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
open class NetworkChannel(private val id: ResourceLocation) {
    private val clientPacketId = CustomPacketPayload.Type<Payload>(id.withSuffix("_client"))
    private val serverPacketId = CustomPacketPayload.Type<Payload>(id.withSuffix("_server"))

    private val serverClasses = mutableListOf<KClass<*>>()
    private val clientClasses = mutableListOf<KClass<*>>()

    private val serverSerializers = mutableListOf<KSerializer<Any>>()
    private val clientSerializers = mutableListOf<KSerializer<Any>>()

    private val serverboundHandlers = mutableListOf<(ByteArray, IPacketContext) -> Unit>()
    private val clientboundHandlers = mutableListOf<(ByteArray, IPacketContext) -> Unit>()

    /**
     * Registers a server-bound packet type and its handler, decoding with [klass]'s own
     * `@Serializable`-generated [KSerializer].
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
        val serializer = klass.serializerOrNull()
            ?: throw IllegalArgumentException("Data class doesn't have a serializer. Did you forget to add @Serializable?")
        serverbound(klass, serializer, handler)
    }

    /**
     * Registers a server-bound packet type and its handler, inferring the packet class from the
     * reified type parameter [T] instead of requiring `T::class` to be passed explicitly.
     *
     * Equivalent to `serverbound(T::class, handler)`.
     *
     * @param T The packet data class type.
     * @param handler The handler invoked on the receiving side.
     * @throws IllegalArgumentException if [T] is not a data class, lacks a serializer, or is already registered.
     */
    inline fun <reified T : Any> serverbound(noinline handler: PacketHandler<T>) = serverbound(T::class, handler)

    /**
     * Registers a server-bound packet type and its handler with an explicit [serializer], for a
     * type that can't carry `@Serializable` (e.g. a Kotlin `object` singleton with a hand-built
     * [KSerializer]). The payload is always decoded before [handler] runs; use [serverboundLazy]
     * instead when decoding must be conditional on something the handler checks first.
     *
     * @throws IllegalArgumentException if [klass] is already registered.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> serverbound(klass: KClass<T>, serializer: KSerializer<T>, handler: PacketHandler<T>) {
        require(serverClasses.find { it == klass } == null) { "Packet is already registered" }
        serverClasses.add(klass)
        serverSerializers.add(serializer as KSerializer<Any>)
        serverboundHandlers.add { bytes, ctx -> handler(SerializationManager.cbor.decodeFromByteArray(serializer, bytes), ctx) }
    }

    /**
     * Registers a server-bound packet type whose handler controls exactly when (or whether) the
     * payload gets decoded, unlike [serverbound] (the two-argument-serializer overload), which
     * always decodes before invoking its handler. Useful when decoding unconditionally would have
     * a side effect that must stay conditional on something only the handler can check (e.g. a
     * permission check that must happen before an edit is applied).
     *
     * @throws IllegalArgumentException if [klass] is already registered.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> serverboundLazy(klass: KClass<T>, serializer: KSerializer<T>, handler: (decode: () -> T, IPacketContext) -> Unit) {
        require(serverClasses.find { it == klass } == null) { "Packet is already registered" }
        serverClasses.add(klass)
        serverSerializers.add(serializer as KSerializer<Any>)
        serverboundHandlers.add { bytes, ctx -> handler({ SerializationManager.cbor.decodeFromByteArray(serializer, bytes) }, ctx) }
    }

    /**
     * Registers a client-bound packet type and its handler, decoding with [klass]'s own
     * `@Serializable`-generated [KSerializer].
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
        val serializer = klass.serializerOrNull()
            ?: throw IllegalArgumentException("Data class doesn't have a serializer. Did you forget to add @Serializable?")
        clientbound(klass, serializer, handler)
    }

    /**
     * Registers a client-bound packet type and its handler, inferring the packet class from the
     * reified type parameter [T] instead of requiring `T::class` to be passed explicitly.
     *
     * Equivalent to `clientbound(T::class, handler)`.
     *
     * @param T The packet data class type.
     * @param handler The handler invoked on the receiving side.
     * @throws IllegalArgumentException if [T] is not a data class, lacks a serializer, or is already registered.
     */
    inline fun <reified T : Any> clientbound(noinline handler: PacketHandler<T>) = clientbound(T::class, handler)

    /**
     * Registers a client-bound packet type and its handler with an explicit [serializer], for a
     * type that can't carry `@Serializable`. See the server-bound overload of the same shape for
     * why this exists.
     *
     * @throws IllegalArgumentException if [klass] is already registered.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> clientbound(klass: KClass<T>, serializer: KSerializer<T>, handler: PacketHandler<T>) {
        require(clientClasses.find { it == klass } == null) { "Packet is already registered" }
        clientClasses.add(klass)
        clientSerializers.add(serializer as KSerializer<Any>)
        clientboundHandlers.add { bytes, ctx -> handler(SerializationManager.cbor.decodeFromByteArray(serializer, bytes), ctx) }
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
            createPayload(
                packet = it,
                classes = serverClasses,
                serializers = serverSerializers,
                payloadId = id.withSuffix("_client"),
                missingMessage = "Trying to send a packet to server but it hasn't registered the packet and its handler",
            )
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
     * @param players The list of target [ServerPlayer]s. A no-op if empty - skipped before
     *   [createPayloads] even runs, so this is also safe to call with no client loaded at all (a
     *   server-only GameTest run, or a dedicated server with nobody online), not just an empty
     *   nearby-players list on an otherwise-normal server.
     * @param packets The packets to send.
     * @throws IllegalArgumentException if no packets are provided.
     */
    fun <T : Any> toPlayers(players: List<ServerPlayer>, vararg packets: T) {
        require(packets.isNotEmpty()) { "You need to specify one or more packets to send" }
        if (players.isEmpty()) return
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
        val radiusSq = radius * radius
        val players = level.players().filter { it !== exclude && it.distanceToSqr(x, y, z) <= radiusSq }
        toPlayers(players, *packets)
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
        val chunk = entity.level().chunkSource as? ServerChunkCache
            ?: throw IllegalStateException("Cannot send clientbound payloads on the client")
        val access = entity.level().registryAccess()
        for (payload in createPayloads(packets)) {
            val packet = NetworkManager.toPacket(NetworkManager.Side.S2C, payload, access)
            if (self) chunk.broadcastAndSend(entity, packet) else chunk.broadcast(entity, packet)
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

    private fun <T : Any> createPayloads(packets: Array<out T>): List<Payload> {
        return packets.map {
            createPayload(
                packet = it,
                classes = clientClasses,
                serializers = clientSerializers,
                payloadId = id.withSuffix("_server"),
                missingMessage = "Trying to send a packet to clients but client hasn't registered the packet and its handler",
            )
        }
    }

    private fun <T : Any> createPayload(
        packet: T,
        classes: List<KClass<*>>,
        serializers: List<KSerializer<Any>>,
        payloadId: ResourceLocation,
        missingMessage: String,
    ): Payload {
        val index = classes.indexOfFirst { it == packet::class }
        if (index < 0) throw IllegalStateException(missingMessage)
        val bytes = SerializationManager.cbor.encodeToByteArray(serializers[index], packet)
        return Payload(payloadId, index, bytes)
    }

    private fun decodeDispatchData(
        payload: Payload,
        handlers: List<(ByteArray, IPacketContext) -> Unit>,
        missingHandlerMessage: String,
        ctx: NetworkManager.PacketContext,
    ) {
        val handler = handlers.getOrNull(payload.index)
            ?: throw NoSuchElementException(missingHandlerMessage)
        handler(payload.data, object : IPacketContext {
            override val player: Player get() = ctx.player
            override val registryAccess: RegistryAccess get() = ctx.registryAccess()
        })
    }

    /**
     * Registers this channel with the Architectury networking layer.
     *
     * Must be called once during mod initialization (before any packets are sent or received).
     * Both [serverbound] and [clientbound] handlers should be registered before calling this.
     */
    fun register() {
        EnvExecutor.runInEnv(Env.SERVER) {
            Runnable {
                NetworkManager.registerS2CPayloadType(serverPacketId, PayloadCodec)
            }
        }
        EnvExecutor.runInEnv(Env.CLIENT) {
            Runnable {
                NetworkManager.registerReceiver(NetworkManager.Side.S2C, serverPacketId, PayloadCodec) { payload, ctx ->
                    decodeDispatchData(
                        payload = payload,
                        handlers = clientboundHandlers,
                        missingHandlerMessage = "No handler was found on the clientside. Did you forget to do clientbound?",
                        ctx = ctx,
                    )
                }
            }
        }

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, clientPacketId, PayloadCodec) { payload, ctx ->
            decodeDispatchData(
                payload = payload,
                handlers = serverboundHandlers,
                missingHandlerMessage = "No handler was found on the serverside. Did you forget to do serverbound?",
                ctx = ctx,
            )
        }
    }
}
