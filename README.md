# Archie

Archie is a Kotlin-first Architectury library mod for Minecraft 1.21.1.
It provides shared utilities used by Kernel Panic mods across Fabric and NeoForge.

Full guides (config, GUI, networking, serialization, etc.) live at
[docs.kernelpanicsoft.net/Archie](https://docs.kernelpanicsoft.net/Archie/) and in [`Archie/docs/`](Archie/docs).

## Repo layout

This repository root is a Gradle **composite build** (`settings.gradle.kts`) that just wires
together two independent Gradle builds:

- [`Archie/`](Archie): the library itself — `common/`, `fabric/`, `neoforge/` subprojects, plus
  the `docs/` site sources. This is what gets published.
- [`Archie-Test/`](Archie-Test): a throwaway playground mod (`common-test`/`fabric-test`/`neoforge-test`)
  used to exercise Archie during development; it substitutes in Archie's project sources instead
  of a published artifact, so changes in `Archie/` are picked up live.

Because these are separate builds, run all Gradle commands **from inside `Archie/` (or
`Archie-Test/`)**, not the repo root — the root `build.gradle.kts` has no real tasks of its own.

Inside `Archie/`, the module layout is:

- `common/`: shared APIs and core implementation (`Archie`, networking, config, GUI, data helpers)
- `fabric/`: Fabric entrypoints, run configs, platform `actual` implementations
- `neoforge/`: NeoForge entrypoints, run configs, platform `actual` implementations

The initialization flow is:

1. Loader entrypoint (`ArchieFabric` or `ArchieNeoForge`)
2. `Archie.init()` in `common`
3. Shared systems register events/network/config and optional datagen/gametest hooks

## Common workflows

Run from inside `Archie/` (`cd Archie` first, or pass `--project-dir Archie`):

```bash
./gradlew build
./gradlew fabric:runClient
./gradlew neoforge:runClient
./gradlew fabric:runDatagen
./gradlew neoforge:runDatagen
./gradlew fabric:runGametest
./gradlew neoforge:runGametest
./gradlew fabric:runGametestClient
./gradlew neoforge:runGametestClient
```

`build` and `assemble` finalize with `fusejars` (merged Fabric+NeoForge artifact).

To run the `Archie-Test` playground mod, use the same commands from inside `Archie-Test/` instead
(e.g. `./gradlew fabric-test:runClient`).

## Conventions that matter

- Add gameplay/library logic in `common` first, then platform-specific `actual` code only when needed.
- Keep `expect/actual` triplets named `*.common.kt`, `*.fabric.kt`, `*.neoforge.kt`.
- Register packet classes and handlers before calling `register()` on `NetworkChannel`.
- Keep loader manifests tokenized (`${mod_id}`, `${versions.*}`); values come from Gradle properties/version catalog.
- Use `bundleRuntimeLibrary(...)` / `bundleMod(...)` for shipped runtime deps in loader modules.

## Key files

- `Archie/common/src/main/kotlin/net/kernelpanicsoft/archie/Archie.kt`
- `Archie/common/src/main/kotlin/net/kernelpanicsoft/archie/networking/NetworkChannel.kt`
- `Archie/fabric/src/main/kotlin/net/kernelpanicsoft/archie/ArchieFabric.kt`
- `Archie/neoforge/src/main/kotlin/net/kernelpanicsoft/archie/ArchieNeoForge.kt`
- `gradle/libs.versions.toml`

