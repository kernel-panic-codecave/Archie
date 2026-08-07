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
- PR titles must follow [Conventional Commits](https://www.conventionalcommits.org/) (`feat:`, `fix:`,
  `docs:`, `refactor:`, `perf:`, `test:`, `build:`, `ci:`, `chore:`, `style:`, `revert:`, optionally
  scoped `type(scope):`) - enforced by `.github/workflows/pr-title-lint.yml`. Individual commits within a
  PR don't need to conform, but a direct push to a release branch (no PR) does, since it's read the same
  way. This isn't just style: cutting a release (`git tag vX.Y.Z`) triggers
  `.github/workflows/release-notes.yaml`, which walks merged PR titles since the last tag to generate
  `Archie/CHANGELOG.md` and a `Archie/docs/news/posts/` entry, grouped by this prefix - an unparsed title
  doesn't break anything, it just lands in the catch-all "Other Changes" section instead of a real one.

## Dependency and integration touchpoints
- Versions and plugin IDs are centralized in `gradle/libs.versions.toml` (repo root); update there first.
- Packaging/publishing is configured at the `Archie/` build root via `modfusioner` (`fusejars`) and
  `modpublisher` (CurseForge/Modrinth/GitHub IDs and required deps, tasks `publishCurseforge`/
  `publishModrinth`/`publishGitHub`/`publishMod`) in `Archie/build.gradle.kts` - `modpublisher` reads its
  changelog text straight off disk from `Archie/CHANGELOG.md` when a publish task runs, so those four
  tasks `dependsOn` a `generateChangelog` task (same file, same script, same `Archie/build.gradle.kts`)
  that regenerates it synchronously first. This is deliberately *not* left to the reactive, tag-triggered
  `release-notes.yaml` workflow: if `modpublisher` auto-tags as part of the same `./gradlew publish*`
  invocation, that workflow can't possibly have generated this release's entry yet by the time
  `changelog` is read, and if that invocation runs in CI under the default `GITHUB_TOKEN`, the tag it
  creates won't even fire the workflow (GitHub's anti-recursion rule for that token). `release-notes.yaml`
  still owns the `Archie/docs/news/posts/` blog entry, which has no such ordering requirement.
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

**Known open issue - client GameTests are intermittently flaky (~1-in-5), cause not confirmed.**
Repeated local runs of `neoforge:runGametestClient` show a different click/animation-driven test
failing each time (`ConfirmDialog`, `RadioGroup`, ...) with `IllegalStateException: Predicate did
not become true within 200 ticks` and no logged exception - not a per-test logic bug. The specific
`isComposeIdle()` TOCTOU gap documented in `ComposeScreen.kt`'s KDoc is already fixed (it now uses
`Recomposer.hasPendingWork`, not the old `recomposeJob`-based check), so that's not the live cause.
The likely remaining gap: `ComposeScreen`'s coroutine scope (`CoroutineScope(Dispatchers.Default) +
BroadcastFrameClock`) runs on **real** threads/wall-clock time, so a composable's `delay(...)` (e.g.
`ConfirmDialog`'s close animation) genuinely races the harness's tick-based polling and the real
render loop's frame delivery. Real Jetpack Compose's own test tooling
(`ComposeTestRule`/`runComposeUiTest`) avoids this whole class of race by backing the composition
with a *virtual* clock/dispatcher (`TestMonotonicFrameClock` over `StandardTestDispatcher`) that
`waitForIdle()` drives forward deterministically, instead of polling real concurrency - Archie's
harness has no equivalent. A real fix likely means a test-only virtual-clock/dispatcher swap for
`ComposeScreen` during GameTests, not another polling tweak. Not yet attempted - would need live
instrumentation to confirm before changing anything.

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
