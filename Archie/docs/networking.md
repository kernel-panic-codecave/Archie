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

Register handlers **before** calling `register()`:

```kotlin
// Server receives this packet from the client
CHANNEL.serverbound(RequestDataPacket::class) { packet, ctx ->
    val player = ctx.player as ServerPlayer
    val data = fetchData(packet.id)
    CHANNEL.toPlayer(player, SyncEnergyPacket(data.energy, data.pos))
}

// Client receives this packet from the server
CHANNEL.clientbound(SyncEnergyPacket::class) { packet, ctx ->
    ClientEnergyCache.update(packet.pos, packet.energy)
}

// Called once during mod init
CHANNEL.register()
```

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
