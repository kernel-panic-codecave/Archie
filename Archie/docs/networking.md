# Networking

Archie's networking layer provides a type-safe, annotation-driven API for sending packets
between client and server. All packets are serialized with **CBOR** via kotlinx.serialization,
so no manual buffer reading/writing is required.

---

## NetworkChannel

`NetworkChannel` is the entry point. A single channel instance can handle any number of
server-bound and client-bound packet types.

```kotlin
// Create the channel (usually a top-level object or companion property)
val CHANNEL = NetworkChannel(MyMod.MOD % "main")
```

### Defining packets

Packet types must be Kotlin **data classes** annotated with `@Serializable`.

```kotlin
@Serializable
data class SyncEnergyPacket(val energy: Int, val pos: SBlockPos)

@Serializable
data class RequestDataPacket(val id: Int)
```

### Registering handlers

Register handlers **before** calling `register()`. The reified `serverbound<T>`/`clientbound<T>`
overloads infer the packet class from the type parameter, so you don't need to pass `::class`
yourself:

```kotlin
// Server receives this packet from the client
CHANNEL.serverbound<RequestDataPacket> { packet, ctx ->
    val player = ctx.player as ServerPlayer
    val data = fetchData(packet.id)
    CHANNEL.toPlayer(player, SyncEnergyPacket(data.energy, data.pos))
}

// Client receives this packet from the server
CHANNEL.clientbound<SyncEnergyPacket> { packet, ctx ->
    ClientEnergyCache.update(packet.pos, packet.energy)
}

// Called once during mod init
CHANNEL.register()
```

A `KClass`-based overload (`serverbound(RequestDataPacket::class) { ... }`) is also available if
you already have the class reference in hand; both forms register the same way.

### Validation

`serverbound`/`clientbound` validate the packet class as soon as you register it, throwing
`IllegalArgumentException` if:

- the class isn't a Kotlin **data class**,
- the class isn't annotated `@Serializable` (or otherwise has no serializer), or
- that exact class was already registered on that side of the channel.

Sending an unregistered packet type (via `toServer`/`toPlayer`/etc.) throws `IllegalStateException`
instead, since that failure can only be detected at send time.

### Sending packets

| Method | Description |
|--------|-------------|
| `toServer(packet)` | Client → Server |
| `toPlayer(player, packet)` | Server → specific player |
| `toPlayers(list, packet)` | Server → list of players |
| `toAllPlayers(packet)` | Server → every connected player |
| `toPlayersInDimension(level, packet)` | Server → all players in a dimension |
| `toNearPlayers(level, exclude, x, y, z, radius, packet)` | Server → players within radius |
| `toPlayersTrackingEntity(entity, self, packet)` | Server → players loading an entity |
| `toPlayersTrackingChunk(level, pos, packet)` | Server → players loading a chunk |

---

## IPacketContext

The `IPacketContext` interface is passed to every packet handler and exposes:

| Property | Type | Description |
|----------|------|-------------|
| `player` | `Player` | The player associated with the packet |
| `registryAccess` | `RegistryAccess` | Dynamic registry access |
| `minecraft` | `Minecraft` | Client-side Minecraft instance (client handlers only) |

---

## Minecraft type serializers

For serializing Minecraft types inside packets, use the provided `S`-prefixed type aliases (each
one is already `@Contextual`-annotated, so you don't add `@Contextual` yourself):

```kotlin
@Serializable
data class TeleportPacket(
    val destination: SBlockPos,
    val dimension: SResourceLocation,
)
```

Available contextual serializers: `BlockPos`, `ChunkPos`, `GlobalPos`, `Vec3`, `Vec3i`,
`BlockHitResult`, `ResourceLocation`, `ItemStack`, `FriendlyByteBuf` — see
[Serialization](serialization.md#minecraft-type-serializers) for the full alias table.

---

## Config sync

`ConfigSpec.Server` configs (see [Config](config.md)) use a `NetworkChannel` of their own,
internally, to sync per-world server config values to clients — the same channel mechanism
described above, just with the `ConfigSpec` itself as the payload instead of a hand-written
packet class. This is wired up automatically by `ConfigSpec`; you don't register anything with
it yourself. See [Config § Server configs sync over the network](config.md#server-configs-sync-over-the-network)
for how it behaves.
