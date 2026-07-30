# AGENTS Guide for Archie

## Repository shape
- The repo root is a Gradle **composite build** (`settings.gradle.kts`) that includes two independent
  builds: `Archie/` (the library — this is what's published) and `Archie-Test/` (a playground mod that
  substitutes in `Archie`'s project sources, used to exercise the library during development). Run all
  Gradle commands from inside `Archie/` or `Archie-Test/`, not the repo root.
- Inside `Archie/`: multi-module Architectury mod: `common` (shared API/logic), `fabric`, `neoforge`
  (`Archie/settings.gradle.kts`).
- `common` is the source of truth; loader modules mostly provide bootstrapping, loader deps, and `actual` implementations.
- Main entrypoint flow is `Archie.init()` -> register events/network/config/datagen/gametest gates (`Archie/common/src/main/kotlin/net/kernelpanicsoft/archie/Archie.kt`).

## Architecture patterns to preserve
- Cross-loader abstractions use Kotlin `expect/actual` files named `*.common.kt`, `*.fabric.kt`, `*.neoforge.kt` (example: `APlatform`, `ADataGeneratorPlatform`, `AGameTestPlatform`).
- Loader entrypoints must only delegate into common init methods:
  - Fabric: `ArchieFabric.onInitialize*` (`Archie/fabric/src/main/kotlin/net/kernelpanicsoft/archie/ArchieFabric.kt`)
  - NeoForge: bus listeners in `ArchieNeoForge` (`Archie/neoforge/src/main/kotlin/net/kernelpanicsoft/archie/ArchieNeoForge.kt`)
- Networking is centralized via `NetworkChannel`; packets must be `@Serializable data class` and registered before `register()` (`Archie/common/src/main/kotlin/net/kernelpanicsoft/archie/networking/NetworkChannel.kt`).
- `ArchieNetworkChannel.init()` is the canonical registration order example (register packet producers/consumers, then call `register()`).

## Build and run workflows
All commands below are run from inside `Archie/` (`cd Archie` first).
- Build all modules + merged artifact: `./gradlew build` (`build`/`assemble` finalize with `fusejars` in `Archie/build.gradle.kts`).
- Loader-specific dev runs: `./gradlew fabric:runClient`, `./gradlew neoforge:runClient`.
- Datagen runs are explicit tasks: `./gradlew fabric:runDatagen` / `./gradlew neoforge:runDatagen`.
- GameTest runs: `./gradlew fabric:runGametest` / `./gradlew neoforge:runGametest` (server-side suite),
  `./gradlew fabric:runGametestClient` / `./gradlew neoforge:runGametestClient` (client GUI harness suite).
- Docs pipeline: `embedDokkaIntoMkDocs` then `publishDocs` (calls `mike deploy ...`); `Archie/mkdocs.yml`
  contains `# !!! EMBEDDED DOKKA ... DO NOT COMMIT !!!` markers. CI (`.github/workflows/docs.yaml`) runs
  `./gradlew publishDocs` with `working-directory: Archie`.

## Project-specific conventions
- Keep resource/manifests tokenized using Gradle properties (`${mod_id}`, `${versions.*}`) in `fabric.mod.json` and `neoforge.mods.toml`.
- Shared assets are merged from `common` into loader modules via `processResources`; do not duplicate `assets/archie/**` directly in loader modules unless loader-specific.
- `common/build.gradle.kts` intentionally uses `modImplementation(libs.fabric.loader)` only for annotations/mixin deps; avoid importing random Fabric-only classes in common code.
- Utility operators are used pervasively for IDs (`Archie % "main"`, `mod % "path"`, `"namespace" % "path"`)
  from `Archie/common/src/main/kotlin/net/kernelpanicsoft/archie/util/ResourceLocation.kt`.

## Dependency and integration touchpoints
- Versions and plugin IDs are centralized in `gradle/libs.versions.toml` (repo root); update there first.
- Packaging/publishing is configured at the `Archie/` build root via `modfusioner` (`fusejars`) and
  `modpublisher` (CurseForge/Modrinth IDs and required deps) in `Archie/build.gradle.kts`.
- Mixins are split by scope: loader mixins in `Archie/fabric/src/main/resources/archie.mixins.json` and
  `Archie/neoforge/src/main/resources/archie.mixins.json`, common mixin config in
  `Archie/common/src/main/resources/archie-common.mixins.json`.

## Safe edit boundaries
- For new gameplay/library logic: start in `Archie/common/src/main/kotlin/...`, then add loader `actual`/bootstrap only when APIs differ.
- When adding packets/events/config sections, mirror existing object-singleton style (`Archie`, `AEvents`, `ArchieNetworkChannel`) rather than introducing DI/service containers.
- If adding new runtime libraries to shipped jars, use `bundleRuntimeLibrary(...)` / `bundleMod(...)` in loader `build.gradle.kts` files (not plain `implementation` only).

## GameTest structure and conventions
GameTests are located in `Archie/common/src/main/gametest/` (a separate Gradle source set from
`src/main/kotlin`) and organized by scope (`common`, `client`, `server`). Test infrastructure and
registration live in `ArchieGameTest.kt` (`.../gametest/internal/ArchieGameTest.kt`).

### Test file organization
- **Test discovery**: Each test class must be registered in `ArchieGameTest.kt`'s `archieGameTests()`
  function via `register<TestClass>()`, inside the `common { }`, `client { }`, or `server { }` block
  matching its scope.
- **Test placement**:
  - `common`: neutral logic that doesn't need a live client or dedicated server (currently empty)
  - `client`: GUI/rendering tests driven through the client harness, e.g. `ComposeRenderingTests`
  - `server`: everything else — e.g. `ArchieItemHandlerTests`, `BlockEntityNBTHolderTests`, `BlockEntityStateManagerTests`
- **Template requirement**: server/common `@GameTest`-annotated methods must use
  `@GameTest(template = EMPTY)`; `EMPTY` is a constant defined in `ArchieGameTest.kt` pointing to an
  empty test structure. Client-harness tests use `@ClientGameTest` instead (see below) and don't need it.

### Writing server/common GameTests
Test methods are **extension functions on `GameTestHelper`**, not functions that take a helper parameter:

```kotlin
@GameTest(template = EMPTY)
fun GameTestHelper.testFieldDefaultsAndPersistenceRoundTrip() {
    assertEquals(1, holder.counter)
    assertTrue(holder.counter > 0) { "counter should be positive" }
}
```

- **Assertions**: internal extension functions on `GameTestHelper` from `GameTestAssertions.kt`
  (`.../gametest/internal/tests/GameTestAssertions.kt`), called unqualified inside a test method:
  - `assertEquals(expected, actual)` – check equality, with an optional custom `message` lambda
  - `assertTrue(condition) { message }` – check a boolean condition
  - `expectThrows<ExceptionType> { block }` – assert `block` throws `ExceptionType`, returns the caught exception
- **Completion**: the vanilla GameTest framework auto-succeeds a test method that returns normally
  without calling `helper.fail(...)`; call `fail(message)` (or an assertion above) to fail explicitly.
- Function names must match the pattern `test*`; tests are discovered via reflection.

### Writing client GameTests (GUI harness)
Client-scope tests use a separate backport harness (`AClientGameTestHarness.kt`,
`ComposeScreenTestContext.kt`) built around `@ClientGameTest` and `ClientGameTestContext`, not the
vanilla `@GameTest`/`GameTestHelper` API:

```kotlin
@ClientGameTest
fun ClientGameTestContext.testComposeScreenMeasuresRenderableNode() {
    setScreen { RenderProbeScreen() }
    waitForScreen<RenderProbeScreen> {
        waitForLayer(0) {
            assertTrue(hasNode(RENDER_PROBE_NAME)) { "Expected render probe node to exist" }
        }
    }
}
```

See `ComposeRenderingTests.kt` for a full example. Registered the same way, via `register<TestClass>()`
inside the `client { }` block.

### Current test coverage
- `ArchieItemHandlerTests` (`server`) – item storage/handler behavior
- `BlockEntityNBTHolderTests` (`server`) – `NBTHolder` field defaults, save/load round-tripping, `@Sync` filtering
- `BlockEntityStateManagerTests` (`server`) – block-entity state sync manager behavior
- `ComposeRenderingTests` (`client`) – GUI framework layout/rendering via the client harness

### Adding new tests
1. Create a new test class in `Archie/common/src/main/gametest/net/kernelpanicsoft/archie/gametest/internal/tests/`.
2. Name it `XyzTests.kt` (following existing convention).
3. For server/common tests: methods as `fun GameTestHelper.testFeatureName()` with `@GameTest(template = EMPTY)`.
   For client tests: methods as `fun ClientGameTestContext.testFeatureName()` with `@ClientGameTest`.
4. Use assertion helpers from `GameTestAssertions.kt` (server/common) or `ClientGameTestContext`'s own
   assertion methods (client).
5. Register the class in `ArchieGameTest.kt`'s `archieGameTests()` under the appropriate scope block.
6. Run tests locally with `./gradlew fabric:runGametest` / `neoforge:runGametest` (server/common), or
   `./gradlew fabric:runGametestClient` / `neoforge:runGametestClient` (client).

IMPORTANT: When applicable, prefer using intellij-index MCP tools for code navigation and refactoring.
IMPORTANT: When debugging, prefer using intellij-debugger MCP tools to interact with the IDE debugger.
IMPORTANT: When writing tests, ensure they are isolated and do not rely on external state or resources unless explicitly managed.