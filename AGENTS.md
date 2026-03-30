# AGENTS Guide for Archie

## Repository shape
- Multi-module Architectury mod: `common` (shared API/logic), `fabric`, `neoforge` (`settings.gradle.kts`).
- `common` is the source of truth; loader modules mostly provide bootstrapping, loader deps, and `actual` implementations.
- Main entrypoint flow is `Archie.init()` -> register events/network/config/datagen/gametest gates (`common/src/main/kotlin/net/kernelpanicsoft/archie/Archie.kt`).

## Architecture patterns to preserve
- Cross-loader abstractions use Kotlin `expect/actual` files named `*.common.kt`, `*.fabric.kt`, `*.neoforge.kt` (example: `APlatform`, `ADataGeneratorPlatform`, `AGameTestPlatform`).
- Loader entrypoints must only delegate into common init methods:
  - Fabric: `ArchieFabric.onInitialize*` (`fabric/src/main/kotlin/net/kernelpanicsoft/archie/ArchieFabric.kt`)
  - NeoForge: bus listeners in `ArchieNeoForge` (`neoforge/src/main/kotlin/net/kernelpanicsoft/archie/ArchieNeoForge.kt`)
- Networking is centralized via `NetworkChannel`; packets must be `@Serializable data class` and registered before `register()` (`common/src/main/kotlin/net/kernelpanicsoft/archie/networking/NetworkChannel.kt`).
- `ArchieNetworkChannel.init()` is the canonical registration order example (register packet producers/consumers, then call `register()`).

## Build and run workflows
- Build all modules + merged artifact: `./gradlew build` (root `build`/`assemble` finalize with `fusejars` in `build.gradle.kts`).
- Loader-specific dev runs: `./gradlew fabric:runClient`, `./gradlew neoforge:runClient`.
- Datagen runs are explicit tasks: `./gradlew fabric:runDatagen` / `./gradlew neoforge:runDatagen`.
- GameTest runs: `./gradlew fabric:runGametest` / `./gradlew neoforge:runGametest`.
- Docs pipeline: `embedDokkaIntoMkDocs` then `publishDocs` (calls `mike deploy ...`); `mkdocs.yml` contains `# !!! EMBEDDED DOKKA ... DO NOT COMMIT !!!` markers.

## Project-specific conventions
- Keep resource/manifests tokenized using Gradle properties (`${mod_id}`, `${versions.*}`) in `fabric.mod.json` and `neoforge.mods.toml`.
- Shared assets are merged from `common` into loader modules via `processResources`; do not duplicate `assets/archie/**` directly in loader modules unless loader-specific.
- `common/build.gradle.kts` intentionally uses `modImplementation(libs.fabric.loader)` only for annotations/mixin deps; avoid importing random Fabric-only classes in common code.
- Utility operators are used pervasively for IDs (`Archie % "main"`, `"namespace" % "path"`) from `common/src/main/kotlin/net/kernelpanicsoft/archie/util/ResourceLocation.kt`.

## Dependency and integration touchpoints
- Versions and plugin IDs are centralized in `gradle/libs.versions.toml`; update there first.
- Packaging/publishing is configured at root via `modfusioner` (`fusejars`) and `modpublisher` (CurseForge/Modrinth IDs and required deps) in `build.gradle.kts`.
- Mixins are split by scope: loader mixins in `fabric/src/main/resources/archie.mixins.json` and `neoforge/src/main/resources/archie.mixins.json`, common mixin config in `common/src/main/resources/archie-common.mixins.json`.

## Safe edit boundaries
- For new gameplay/library logic: start in `common/src/main/kotlin/...`, then add loader `actual`/bootstrap only when APIs differ.
- When adding packets/events/config sections, mirror existing object-singleton style (`Archie`, `AEvents`, `ArchieNetworkChannel`) rather than introducing DI/service containers.
- If adding new runtime libraries to shipped jars, use `bundleRuntimeLibrary(...)` / `bundleMod(...)` in loader `build.gradle.kts` files (not plain `implementation` only).
