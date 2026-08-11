# Welcome to Archie

**Archie** is a Kotlin-first library mod for Minecraft (1.21.x) that provides a comprehensive
set of utilities for [Kernel Panic's](https://github.com/kernel-panic-codecave) mods.
It targets both **Fabric** and **NeoForge** via [Architectury](https://github.com/architectury/architectury-api).

---

## Feature Overview

| Area | Description |
|---|---|
| **Networking** | Strongly-typed, CBOR-serialized packet channels |
| **Registries** | Deferred-register helpers for blocks, items, and creative tabs |
| **Serialization** | Kotlinx serialization + Mojang Codec bridge; NBT holders; data attachments; Minecraft type serializers |
| **Config** | Hierarchical, multi-format config system (JSON5, TOML, JSON) with Cloth Config UI and optional client↔server sync |
| **GUI** | Compose-for-Minecraft UI framework with layout, modifiers, composables, and themes |
| **Events** | Architectury event wrappers |
| **Data Gen** | Data generation provider utilities |
| **Transfer** | Cross-platform item storage and inventory slot helpers |
| **Block Entities** | NBT-backed block entity base class |
| **Resource Packs** | Deserialization-based resource reload listeners |
| **GameTest** | Server-side `GameTestHelper` registration plus a client GameTest DSL for driving/asserting Compose screens |

---

## Quick Start

### Gradle (Fabric)

```kotlin
repositories {
    maven("https://maven.kernelpanicsoft.net/releases")
}

dependencies {
    modImplementation("net.kernelpanicsoft:archie-fabric:<version>")
}
```

### Gradle (NeoForge)

```kotlin
dependencies {
    implementation("net.kernelpanicsoft:archie-neoforge:<version>")
}
```

### Mod initialization

```kotlin
object MyMod {
    const val MOD_ID = "mymod"

    fun init() {
        MyItems.init()
        MyBlocks.init()
        MyChannel.register()
    }
}
```

---

## Where to add new code

- Put shared logic in `core/common/src/main/kotlin/...` first.
- Add loader differences with `expect/actual` triplets: `*.common.kt`, `*.fabric.kt`, `*.neoforge.kt`.
- Keep Fabric and NeoForge entrypoints thin (`ArchieFabric`, `ArchieNeoForge`) and delegate to `Archie.init*()`.
- Register packet handlers before calling `register()` on your `NetworkChannel`.

