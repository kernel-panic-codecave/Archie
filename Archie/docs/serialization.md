# Serialization

Archie provides a multi-layered serialization stack built on top of
[kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization) and Mojang's
[`Codec`](https://github.com/Mojang/DataFixerUpper) system.

> This page covers the general-purpose NBT/Codec/kotlinx.serialization mechanisms (contextual
> Minecraft-type serializers, the Codec bridge, `NBTHolder`, `@Sync`). Config file persistence
> (`IConfigSerializer` and the JSON/JSON5/TOML/no-op implementations) is a separate, config-specific
> layer documented in [config.md](config.md#formats).

---

## NBT helpers (`serialization/NBT.kt`)

### `NBT` object

A pre-configured `Nbt` instance (Java variant, no compression):

```kotlin
NBT.encodeToNbtTag(MyData.serializer(), myData)
NBT.decodeFromNbtTag(MyData.serializer(), tag)
```

### Builder functions

```kotlin
val tag: CompoundTag = buildCompoundTag {
    put("count", NbtInt(42))
    put("label", NbtString("hello"))
}

val listTag: ListTag = buildListTag<NbtInt> {
    add(NbtInt(1)); add(NbtInt(2))
}

mergeToCompoundTag(existingTag) { put("extra", NbtString("value")) }

forEachTag(listTag) { nbt -> println(nbt) }
```

### Extension conversions

```kotlin
// knbt ↔ Minecraft Tag
val mcTag: Tag = myNbtTag.toMinecraft
val knbtTag: NbtTag? = mcTag.fromMinecraft

// Compound round-trip
val compound: CompoundTag = nbtCompound.toMinecraft
val back: NbtCompound = compound.fromMinecraft
```

---

## NBTHolder

`NBTHolder` is an interface that lets you declare NBT-backed fields via Kotlin property delegation.

```kotlin
class MyBlockEntity(pos, state) : NBTBlockEntity(TYPE, pos, state) {
    var count  by nbt.intField()
    var label  by nbt.stringField { "default" }
    val items  by nbt.itemField(9)                        // 9-slot inventory
    val tank   by nbt.fluidField(FluidStack.bucketAmount() * 4) // 1 tank slot, 4 buckets
    val energy by nbt.energyField(10_000)                 // a single energy buffer

    // Generic field with custom serializer
    var pos    by nbt.field(BlockPos.CODEC.serializer()) { BlockPos.ZERO }
}
```

Available field types: `boolean`, `byte`, `ubyte`, `short`, `ushort`, `int`, `uint`, `long`,
`ulong`, `float`, `double`, `string`, `item`, `fluid`, `energy`, plus a generic
`field(serializer, default)`. See [transfer.md](transfer.md) for the storage types `item`/`fluid`/
`energy` fields produce.

---

## Codec ↔ KSerializer bridge

### `Codec<T>.serializer()` / `CodecSerializer`

Convert a Mojang `Codec` into a `KSerializer` so it can be used with kotlinx.serialization:

```kotlin
@Serializable
data class MyData(
    val biome: @Serializable(with = BiomeCodecSerializer::class) ResourceKey<Biome>,
)

object BiomeCodecSerializer : CodecSerializer<ResourceKey<Biome>>(Biome.CODEC)
// or simply:
val ser: KSerializer<ResourceKey<Biome>> = Biome.CODEC.serializer()
```

### `KSerializer<T>.codec()` / `SerializerCodec`

Convert a `KSerializer` into a Mojang `Codec`:

```kotlin
@Serializable
data class Config(val value: Int, val name: String)

val CONFIG_CODEC: Codec<Config> = Config.serializer().codec()
// Equivalent to RecordCodecBuilder.create { ... } but derived from @Serializable
```

### `KSerializer<T>.streamCodec`

An extension property that produces a `StreamCodec<RegistryFriendlyByteBuf, T>` backed by CBOR:

```kotlin
val STREAM_CODEC = MyPacket.serializer().streamCodec
```

---

## KOps

`KOps` provides `DynamicOps` implementations for kotlinx.serialization's `JsonElement`,
Toml's `TomlElement`, and knbt's `NbtTag`. These are used internally by `SerializerCodec`
and `CodecSerializer` for Codec interop.

---

## Minecraft type serializers

Register contextual serializers for common Minecraft types by including `MinecraftSerializersModule`:

| Type | Alias |
|------|-------|
| `BlockPos` | `SBlockPos` |
| `ChunkPos` | `SChunkPos` |
| `GlobalPos` | `SGlobalPos` |
| `Vec3` | `SVec3` |
| `Vec3i` | `SVec3i` |
| `BlockHitResult` | `SBlockHitResult` |
| `ResourceLocation` | `SResourceLocation` |
| `ItemStack` | `SItemStack` |
| `FriendlyByteBuf` | `SFriendlyByteBuf` |

Each alias already carries `@Contextual`, so just use it as the field's type directly:

```kotlin
@Serializable
data class MyPacket(
    val pos: SBlockPos,
    val dir: String,
)
```

---

## `@Sync`

`@Sync` is a marker annotation for `NBTHolder`-delegated properties:

```kotlin
class MyHolder : NBTHolder by NBTHolder.create() {
    var serverOnly by intField { 0 }

    @Sync
    var visibleToClient by intField { 0 }
}
```

Only `@Sync`-annotated fields are included in `NBTHolder.getSyncTag()`. `NBTBlockEntity` calls
this to build the tag sent to tracking clients, so annotate exactly the fields a block entity
needs on the client (e.g. for rendering or GUI display) — everything else stays server-only and
is only persisted via the normal save/load tag.
