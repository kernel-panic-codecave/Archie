# AGENTS Guide for Archie

## Repository shape
- The repo root is a single Gradle build (`settings.gradle.kts`) on Architectury Loom, with four
  products, each nested `<product>/<platform>` and flattened to a single-level project name
  (e.g. `core/fabric` -> `archie-core-fabric`): `core` (the library, published), `datagen`
  (Archie's datagen DSL, own separate mod `archie_datagen`, dev-time only), `gametest` (Archie's
  GameTest framework/harness, own separate mod `archie_gametest`, dev/test-time only), `test` (a
  playground mod `archie_test` that depends on the other three via plain project references, used
  to exercise the library during development). Run all Gradle commands from the repo root.
- Each product's own layout: `common` (shared API/logic), `fabric`, `neoforge`
  (`settings.gradle.kts`'s `includeCorePlatform`/`includeModule` helpers).
- `common` is the source of truth; loader modules mostly provide bootstrapping, loader deps, and `actual` implementations.
- Main entrypoint flow is `Archie.init()` -> register events/network/config, then activate
  datagen/gametest via a `ServiceLoader`-based `ArchieExtension` hook (`net.kernelpanicsoft.archie.
  ArchieExtension`, `core/common/.../ArchieExtension.kt`) if `archie-datagen`/`archie-gametest` are
  present on the classpath - `core` never has a compile-time dependency on either
  (`core/common/src/main/kotlin/net/kernelpanicsoft/archie/Archie.kt`).
- `core` never depends on `datagen`/`gametest`, even for things the datagen DSL also touches:
  condition/ingredient registration and common tags run at runtime (`Archie.kt` calls
  `ABuiltinConditions.init()`/`ABuiltinIngredients.init()`/`ACommonTags.init()` directly), so they
  live in `core`; only the datagen-only half of each (e.g. `ADatagenConditionsPlatform`'s
  `withCondition`/`fabricRecipeProvider`, which reference the datagen-only `ARecipeProvider` type)
  lives in `datagen`.

## Architecture patterns to preserve
- Cross-loader abstractions use Kotlin `expect/actual` files named `*.common.kt`, `*.fabric.kt`, `*.neoforge.kt` (example: `APlatform`, `ADataGeneratorPlatform`, `AGameTestPlatform`).
- Loader entrypoints must only delegate into common init methods:
  - Fabric: `ArchieFabric.onInitialize*` (`core/fabric/src/main/kotlin/net/kernelpanicsoft/archie/ArchieFabric.kt`)
  - NeoForge: bus listeners in `ArchieNeoForge` (`core/neoforge/src/main/kotlin/net/kernelpanicsoft/archie/ArchieNeoForge.kt`)
- Networking is centralized via `NetworkChannel`; packets must be `@Serializable data class` and registered before `register()` (`core/common/src/main/kotlin/net/kernelpanicsoft/archie/networking/NetworkChannel.kt`).
- `ArchieNetworkChannel.init()` is the canonical registration order example (register packet producers/consumers, then call `register()`).

## Build and run workflows
All commands below are run from the repo root.
- Build everything: `./gradlew build`.
- Loader-specific dev runs: `./gradlew archie-core-fabric:runClient`, `./gradlew archie-core-neoforge:runClient`.
- Datagen runs are explicit tasks: `./gradlew archie-datagen-fabric:runDatagen` / `./gradlew archie-datagen-neoforge:runDatagen`.
- GameTest runs: `./gradlew archie-gametest-fabric:runGametest` / `./gradlew archie-gametest-neoforge:runGametest` (server-side suite),
  `./gradlew archie-gametest-fabric:runGametestClient` / `./gradlew archie-gametest-neoforge:runGametestClient` (client GUI harness suite).
- Docs pipeline: `embedDokkaIntoMkDocs` then `publishDocs` (calls `mike deploy ...`); root `mkdocs.yml`
  contains `# !!! EMBEDDED DOKKA ... DO NOT COMMIT !!!` markers. CI (`.github/workflows/docs.yaml`) runs
  `./gradlew publishDocs` from the repo root.
- `./gradlew build`/`assemble` are `finalizedBy(fusejars)`, which merges only `archie-core-fabric`'s
  and `archie-core-neoforge`'s `remapJar` outputs (`fusioner { fabric { projectName =
  "archie-core-fabric" }; neoforge { projectName = "archie-core-neoforge" } }` in root
  `build.gradle.kts`) into one artifact under `build/artifacts/` - `datagen`/`gametest`/`test` each
  ship as their own separate mod and are never fused. `./gradlew publishCurseforge`/
  `publishModrinth`/`publishGitHub`/`publishMod` publish that merged jar (`publisher{}` block, same
  file).
- Every `archie-core`/`archie-datagen`/`archie-gametest` module (not `archie-test` - dev playground,
  never published) gets its own `MavenPublication` to kernelpanicsoft.net's Reposilite
  (`./gradlew publishToMavenLocal`/`publish`), wired via `extensions.configure<PublishingExtension>
  ("publishing") { ... }` inside root `build.gradle.kts`'s `subprojects{}` (not the bare `publishing
  { }` DSL accessor - that's only type-safe when the plugin is applied via a `plugins{}` block, and
  `maven-publish` here is applied imperatively). Release vs. snapshot repo URL is chosen by whether
  `version` ends in `SNAPSHOT`; credentials come from `local.properties` (`reposilite.username`/
  `reposilite.password`, gitignored, developer machines) or `REPOSILITE_USERNAME`/
  `REPOSILITE_PASSWORD` env vars (CI).

## Project-specific conventions
- Keep resource/manifests tokenized using Gradle properties (`${mod_id}`, `${versions.*}`) in `fabric.mod.json` and `neoforge.mods.toml`. `datagen`/`gametest`/`test` each ship as their own mod, so their own modId is `${mod_id}_datagen`/`${mod_id}_gametest`/`${mod_id}_test`, not the bare `${mod_id}`.
- Shared assets are merged from `common` into loader modules via `processResources`; do not duplicate `assets/archie/**` directly in loader modules unless loader-specific.
- `core/common/build.gradle.kts` intentionally uses `modImplementation(libs.fabric.loader)` only for annotations/mixin deps; avoid importing random Fabric-only classes in common code.
- Utility operators are used pervasively for IDs (`Archie % "main"`, `mod % "path"`, `"namespace" % "path"`)
  from `core/common/src/main/kotlin/net/kernelpanicsoft/archie/util/ResourceLocation.kt`.
- PR titles must follow [Conventional Commits](https://www.conventionalcommits.org/) (`feat:`, `fix:`,
  `docs:`, `refactor:`, `perf:`, `test:`, `build:`, `ci:`, `chore:`, `style:`, `revert:`, optionally
  scoped `type(scope):`) - enforced by `.github/workflows/pr-title-lint.yml`. Individual commits within a
  PR don't need to conform, but a direct push to a release branch (no PR) does, since it's read the same
  way. This isn't just style: cutting a release (`git tag vX.Y.Z`) triggers
  `.github/workflows/release-notes.yaml`, which walks merged PR titles since the last tag to generate
  `CHANGELOG.md` and a `docs/news/posts/` entry, grouped by this prefix - an unparsed title
  doesn't break anything, it just lands in the catch-all "Other Changes" section instead of a real one.

## Dependency and integration touchpoints
- Versions and plugin IDs are centralized in `gradle/libs.versions.toml` (repo root); update there first.
- `generateChangelog` (root `build.gradle.kts`) regenerates `CHANGELOG.md` synchronously from
  `.github/scripts/generate_release_notes.py` - kept separate from the reactive, tag-triggered
  `release-notes.yaml` workflow for the same reasons as before the migration (see the task's own
  comment in `build.gradle.kts`). `publishCurseforge`/`publishModrinth`/`publishGitHub`/`publishMod`
  each `dependsOn(generateChangelog)` so `modpublisher`'s changelog (read straight off disk) is
  always fresh when a publish task runs.
- Mixins are split by scope: loader mixins in `core/fabric/src/main/resources/archie.mixins.json` and
  `core/neoforge/src/main/resources/archie.mixins.json`, common mixin config in
  `core/common/src/main/resources/archie-common.mixins.json`. `datagen`/`gametest` each have their
  own small per-loader mixins.json too (`archie_datagen.mixins.json`/`archie_gametest.mixins.json`),
  since they're separate mods.

## Safe edit boundaries
- For new gameplay/library logic: start in `core/common/src/main/kotlin/...`, then add loader `actual`/bootstrap only when APIs differ.
- When adding packets/config sections, mirror existing object-singleton style (`Archie`, `ArchieNetworkChannel`) rather than introducing DI/service containers. For datagen/gametest event wiring specifically, mirror `ADatagenEvents`/`AGametestEvents` (in `datagen`/`gametest` respectively - the generic `AEventObject`/`Handler`/`HandlerConstructor` base plumbing lives in `core`).
- If adding new runtime libraries to shipped jars, use `bundleRuntimeLibrary(...)` / `bundleMod(...)` in loader `build.gradle.kts` files (not plain `implementation` only).

## GameTest structure and conventions
Archie's own self-test GameTest suite lives in `gametest/common/src/main/kotlin/net/kernelpanicsoft/
archie/gametest/internal/tests/` (a normal source dir - the old `Archie/common/src/main/gametest/`
separate-Gradle-source-set trick is gone now that `gametest` is its own module/mod entirely,
`archie_gametest`), organized by scope (`common`, `client`, `server`). Test infrastructure and
registration live in `ArchieGameTest.kt` (`gametest/common/.../gametest/internal/ArchieGameTest.kt`).
The GameTest *framework itself* (harness, assertions, junit runner) is `gametest/common/.../gametest/`
(one level up from `internal/`) - this is what `archie-test`'s own GameTests, and any consuming mod's,
build on.

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

- **Assertions**: public extension functions on `GameTestHelper` from `GameTestAssertions.kt`
  (`net.kernelpanicsoft.archie.gametest.GameTestAssertions.kt` — import them, they're not
  same-package with your test class), usable by consuming mods too, not just Archie's own suite:
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

**Client GameTests run on a virtual clock/dispatcher, not real threads.** `ComposeScreen`/
`ComposeContainerScreen` normally back their coroutine scope with `Dispatchers.Default` and real
wall-clock time, so a composable's `delay(...)` (e.g. a dialog's close animation) genuinely raced
real thread scheduling and frame delivery against the harness's tick-based polling - roughly a
1-in-5 failure rate, a different test failing each time, no logged exception. Real Jetpack
Compose's own test tooling avoids this whole class of race by backing composition with a virtual
clock/dispatcher instead of real concurrency; `AClientGameTestHarness.kt`'s `run()` now does the
same, installing a `ComposeTestClockOverride` (`common/src/main/kotlin/net/kernelpanicsoft/archie/gui/ComposeScreen.kt`)
around each test - a `StandardTestDispatcher` plus a per-frame `scheduler.advanceTimeBy(50)` pump
called from `renderNodes()`. Confirmed fixed: 8 consecutive full local `neoforge:runGametestClient`
runs (131 individual tests total, including the previously-flaky ones), zero failures.

Use `advanceTimeBy(bounded)`, never `advanceUntilIdle()`, for that pump - composables can run
legitimately infinite `delay()` loops (e.g. `TextFieldCore`'s blinking-cursor `LaunchedEffect`),
and `advanceUntilIdle()` only returns once truly nothing is scheduled anywhere, which for an
unboundedly-recurring loop is never. The first attempt at this fix used `advanceUntilIdle()` and
hung the render thread permanently the moment any screen with a focused text field was tested -
every subsequent test in that run then failed too, since the client's main-thread executor queue
never got a chance to run again (`client.screen` frozen at whatever it was when the hang started).

`kotlinx-coroutines-test` (the dependency this needs) is dev/test-only - `compileOnly` in
`core/common/build.gradle.kts` (since `ComposeScreen.kt` itself lives in `core`, which ships to
every real player, and must never resolve the symbol), plain `implementation` in
`gametest/common/build.gradle.kts` (`gametest` is dev/test-only already, no need for `compileOnly`
there) and `runtimeLibrary(...)` (present for local runs like `runGametestClient`, never bundled
into the shipped jar) in the loader modules. `ComposeScreen` itself never references
`kotlinx.coroutines.test.*` symbols directly (only `AClientGameTestHarness`'s method bodies do, and
those only run under `AGameTestPlatform.isGameTest`, and live in `gametest` not `core`) - `core`'s
`ComposeScreen` only holds a plain `CoroutineDispatcher?` and a `(() -> Unit)?` pump callback (both
already-bundled core/stdlib types) on its public `ComposeTestClockOverride` object, specifically so
a real player's game never needs to resolve the coroutines-test symbol, while `gametest` (a
different module) can still reach in and set it.

### Current test coverage
- `ArchieItemHandlerTests` (`server`) – item storage/handler behavior
- `BlockEntityNBTHolderTests` (`server`) – `NBTHolder` field defaults, save/load round-tripping, `@Sync` filtering
- `BlockEntityStateManagerTests` (`server`) – block-entity state sync manager behavior
- `ComposeRenderingTests` (`client`) – GUI framework layout/rendering via the client harness

### Adding new tests
1. Create a new test class in `gametest/common/src/main/kotlin/net/kernelpanicsoft/archie/gametest/internal/tests/`.
2. Name it `XyzTests.kt` (following existing convention).
3. For server/common tests: methods as `fun GameTestHelper.testFeatureName()` with `@GameTest(template = EMPTY)`.
   For client tests: methods as `fun ClientGameTestContext.testFeatureName()` with `@ClientGameTest`.
4. Use assertion helpers from `GameTestAssertions.kt` (server/common) or `ClientGameTestContext`'s own
   assertion methods (client).
5. Register the class in `ArchieGameTest.kt`'s `archieGameTests()` under the appropriate scope block.
6. Run tests locally with `./gradlew archie-gametest-fabric:runGametest` / `archie-gametest-neoforge:runGametest` (server/common), or
   `./gradlew archie-gametest-fabric:runGametestClient` / `archie-gametest-neoforge:runGametestClient` (client).

IMPORTANT: When applicable, prefer using intellij-index MCP tools for code navigation and refactoring.
IMPORTANT: When debugging, prefer using intellij-debugger MCP tools to interact with the IDE debugger.
IMPORTANT: When writing tests, ensure they are isolated and do not rely on external state or resources unless explicitly managed.

<!-- engram:begin -->
## Engram — durable project memory (MCP server: `engram`)

This project keeps a local, user-owned knowledge graph of its *reasoning*:
Decisions and their reasons, Principles, Cautions that bit us, Problems and
Resolutions, Insights, open Intents. Not code structure — the code holds that.
What good capture buys: the next session starts already knowing why things
are the way they are, and the user sees and curates everything in the pane.

**Recall.** Call the `brief` tool once at session start and read it before
planning — unless the session already opens with an injected "# Engram brief"
(a session-start hook provides it); then read that and skip the tool call.
Before any non-trivial decision, `search` the graph; hits carry their 1-hop
neighbors — read `conflicts-with` / `replaces` edges first, and pass
`parents`/`children` to `get_node` when you need the reasoning chain. For
history: `timeline` walks a node's replaces chain, `audit` pages the mutation
journal. For whole-graph reviews or exports (a decisions.md), page
`list_nodes` — complete nodes with full bodies; batch cleanups and multi-note
captures go through `update_nodes` / `add_notes`. A hit marked `stale: true` has decayed trust: verify it before
relying on it, and refresh it with `update_node` if it's still accurate.

**Capture.** Capture sparingly — only clearly durable, high-value knowledge: key Decisions with their reasons, stated Principles, hard-won Cautions, Problems with their Resolutions. A dozen good nodes beat fifty mirrored doc lines; when in doubt, don't write.
Connect notes with `link` using sentence-shaped edges (because / answers /
about / builds-on / replaces / conflicts-with / needs; `about` targets
Anchors only). Every write's response is a verdict, not a receipt: on
`{matched, created: false}` merge into the match with `update_node` instead
of duplicating; on `warnings` you are near contradicted or superseded canon —
check it before piling on; on `suspects` the write queued unjudged look-alike
pairs — judge each immediately with `resolve_suspect` (conflict | replaces |
dismiss) and tell the user when one genuinely contradicts standing canon (the
one exception to silent capture). When several notes share a subject,
create/reuse an Anchor and attach them — anchors never accrue by themselves.
Batch the writing at natural stopping points, never the noticing: every real
decision gets captured unprompted — a feature request usually hides one
(library picked, shape chosen, tradeoff accepted). Don't narrate writes.
Never store secrets, credentials, or volatile implementation detail (line
numbers, transient state).

**Maintain.** Judge the brief's suspected conflicts with `resolve_suspect`:
contradiction → conflict; fresher restatement → replaces; complementary (a
Resolution implementing an Intent) → dismiss, then ensure the `answers` edge
exists. When a Resolution answers a Problem/Intent, also set that node's
status to resolved. `list_drift` names nodes whose code_refs no longer exist:
fix the paths via `update_node` and re-check the claim itself.

**Trust.** `approve_node` is restricted: only on the user's explicit demand,
or after verifying a node's content word-by-word. Routine "still relevant"
signals are `update_node`, never approval.

The user sees and curates the graph at http://127.0.0.1:8787 — started with
`engram-alpha serve` in the repo root (one daemon per repo; the real port lives in
`.engram/daemon.json`). If the engram tools disconnect mid-session, don't
drop captures — the daemon serves the same operations over HTTP (`POST
/nodes` etc. with `"source": "claude"`), or tell the user to reconnect.
<!-- engram:end -->
