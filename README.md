# Archie

Archie is a Kotlin-first Architectury library mod for Minecraft 1.21.1.
It provides shared utilities used by Kernel Panic mods across Fabric and NeoForge.

## Module layout

- `common/`: shared APIs and core implementation (`Archie`, networking, config, GUI, data helpers)
- `fabric/`: Fabric entrypoints, run configs, platform `actual` implementations
- `neoforge/`: NeoForge entrypoints, run configs, platform `actual` implementations

The initialization flow is:

1. Loader entrypoint (`ArchieFabric` or `ArchieNeoForge`)
2. `Archie.init()` in `common`
3. Shared systems register events/network/config and optional datagen/gametest hooks

## Common workflows

```bash
./gradlew build
./gradlew fabric:runClient
./gradlew neoforge:runClient
./gradlew fabric:runDatagen
./gradlew neoforge:runDatagen
./gradlew fabric:runGametest
./gradlew neoforge:runGametest
```

`build` and `assemble` finalize with `fusejars` (merged Fabric+NeoForge artifact).

## Conventions that matter

- Add gameplay/library logic in `common` first, then platform-specific `actual` code only when needed.
- Keep `expect/actual` triplets named `*.common.kt`, `*.fabric.kt`, `*.neoforge.kt`.
- Register packet classes and handlers before calling `register()` on `NetworkChannel`.
- Keep loader manifests tokenized (`${mod_id}`, `${versions.*}`); values come from Gradle properties/version catalog.
- Use `bundleRuntimeLibrary(...)` / `bundleMod(...)` for shipped runtime deps in loader modules.

## Key files

- `common/src/main/kotlin/net/kernelpanicsoft/archie/Archie.kt`
- `common/src/main/kotlin/net/kernelpanicsoft/archie/networking/NetworkChannel.kt`
- `fabric/src/main/kotlin/net/kernelpanicsoft/archie/ArchieFabric.kt`
- `neoforge/src/main/kotlin/net/kernelpanicsoft/archie/ArchieNeoForge.kt`
- `gradle/libs.versions.toml`

