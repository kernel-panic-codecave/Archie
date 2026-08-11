# Archie

Archie is a Kotlin-first Architectury library mod for Minecraft 1.21.1.
It provides shared utilities used by Kernel Panic mods across Fabric and NeoForge.

Full guides (config, GUI, networking, serialization, etc.) live at
[docs.kernelpanicsoft.net/Archie](https://docs.kernelpanicsoft.net/Archie/) and in [`docs/`](docs).

## Repo layout

This repository root is a single Gradle build (`settings.gradle.kts`) using Architectury Loom,
with four products, each split into `common`/`fabric`/`neoforge` subprojects nested under one
directory per product and flattened to single-level project names (e.g. `core/fabric` →
`archie-core-fabric`), matching `terrarium-earth/Common-Storage-Lib`'s layout:

- [`core/`](core) (`archie-core-{common,fabric,neoforge}`): the library itself - `Archie`,
  networking, config, GUI, transfer, registries, resource packs. This is what gets published.
- [`datagen/`](datagen) (`archie-datagen-{common,fabric,neoforge}`): Archie's datagen DSL. Ships as
  its own separate mod (`archie_datagen`), dev-time only - never on a real player's classpath.
- [`gametest/`](gametest) (`archie-gametest-{common,fabric,neoforge}`): Archie's GameTest
  framework/harness. Also its own separate mod (`archie_gametest`), dev/test-time only.
- [`test/`](test) (`archie-test-{common,fabric,neoforge}`): a throwaway playground mod
  (`archie_test`) used to exercise Archie during development - depends on `core`/`datagen`/
  `gametest` via plain project references, so changes there are picked up live.

Run all Gradle commands from the repo root.

Each product's module layout is the same shape:

- `common/`: shared APIs and core implementation
- `fabric/`: Fabric entrypoints, run configs, platform `actual` implementations
- `neoforge/`: NeoForge entrypoints, run configs, platform `actual` implementations

The initialization flow is:

1. Loader entrypoint (`ArchieFabric` or `ArchieNeoForge`, in `core/{fabric,neoforge}`)
2. `Archie.init()` in `core/common`
3. Shared systems register events/network/config; datagen/gametest activate via a
   `ServiceLoader`-based `ArchieExtension` hook if `archie-datagen`/`archie-gametest` are present

## Common workflows

```bash
./gradlew build
./gradlew archie-core-fabric:runClient
./gradlew archie-core-neoforge:runClient
./gradlew archie-datagen-fabric:runDatagen
./gradlew archie-datagen-neoforge:runDatagen
./gradlew archie-gametest-fabric:runGametest
./gradlew archie-gametest-neoforge:runGametest
./gradlew archie-gametest-fabric:runGametestClient
./gradlew archie-gametest-neoforge:runGametestClient
```

To run the `archie-test` playground mod, use the same task names against its own modules instead
(e.g. `./gradlew archie-test-fabric:runClient`).

## Conventions that matter

- Add gameplay/library logic in `common` first, then platform-specific `actual` code only when needed.
- Keep `expect/actual` triplets named `*.common.kt`, `*.fabric.kt`, `*.neoforge.kt`.
- Register packet classes and handlers before calling `register()` on `ArchieNetworkChannel`.
- Keep loader manifests tokenized (`${mod_id}`, `${versions.*}`); values come from Gradle properties/version catalog.
- Use `bundleRuntimeLibrary(...)` / `bundleMod(...)` for shipped runtime deps in loader modules.
- Runtime-needed-by-`Archie.kt` code (conditions, ingredients, common tags) lives in `core`, even
  though the datagen DSL that also touches it lives in `datagen` - `core` never depends on
  `datagen`/`gametest`.

## Key files

- `core/common/src/main/kotlin/net/kernelpanicsoft/archie/Archie.kt`
- `core/common/src/main/kotlin/net/kernelpanicsoft/archie/networking/ArchieNetworkChannel.kt`
- `core/fabric/src/main/kotlin/net/kernelpanicsoft/archie/ArchieFabric.kt`
- `core/neoforge/src/main/kotlin/net/kernelpanicsoft/archie/ArchieNeoForge.kt`
- `gradle/libs.versions.toml`
