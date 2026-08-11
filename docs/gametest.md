# GameTest

Archie has two layers of GameTest support: vanilla server-side `GameTestHelper` tests, registered
through a small mod-scoped wrapper around Architectury's event system, and a from-scratch
**client GameTest DSL** for driving and asserting against Archie's [GUI](gui.md) framework —
clicking, hovering, typing into, and reading state off of Compose screens without vanilla's
GameTest structures (which only run server-side) ever being involved.

---

## Registering tests

Both layers share one registration mechanism, built on `AEvents.REGISTER_GAME_TEST` (see
[Events](events.md) for `AEventObject`/`ABasicEventObject` in general). Subclass
`AGameTestEventObject` and declare test classes against an `AEvents.ArchieGameTestBuilder`:

```kotlin
internal object ArchieGameTest : AGameTestEventObject(Archie.MOD) {
    override fun AEvents.ArchieGameTestBuilder.handler() = archieGameTests()
}

internal fun AEvents.ArchieGameTestBuilder.archieGameTests() {
    server {
        register<BlockEntityStateManagerTests>()
    }
    client {
        register<InputComponentsGameTest>()
    }
    common {
        // registered regardless of side
    }
}
```

`server { }` and `client { }` only collect their `register<T>()` calls when `AGameTestPlatform.side`
matches (`AGameTestSide.SERVER`/`CLIENT`); `common { }` always collects. Collected classes are
handed to `AGameTestPlatform.register(clazz, mod)`, an `expect object` with Fabric/NeoForge
`actual`s that stash them in a per-mod map for the loader's GameTest bootstrap to pick up.
`AGameTestPlatform.isGameTest`/`side` read Archie-owned system properties set only on its own
`gametest`/`gametestClient` Gradle runs, so they can't leak into a plain `runClient` invocation.

A mod with zero registered test *functions* for a side crashes vanilla's `GameTestServer` boot
outright (`IllegalArgumentException: No test functions were given!`). `NoOpGameTest` is a
trivially-succeeding placeholder class Archie registers on each loader whenever a mod's suite
comes up empty for the current side — e.g. a client-only test mod's server invocation.

Every mod's `AEvents.MODS` list is shared JVM-wide, which matters for composite builds:
`AGameTestModFilter.selectMods(mods)` narrows a run down to the mod(s) named by the
`archie.gametest.modid` system property, so `Archie-Test`'s own `runGametest`/`runGametestClient`
doesn't also re-run Archie's entire internal suite in the same process. Both loaders' test
collection call this before iterating registered classes — you don't normally need to touch it
unless you're wiring up a similar composite-build setup yourself.

---

## Server-side tests

Server-side GameTests are ordinary vanilla `GameTestHelper` tests. Archie's own convention writes
each test as an **extension function on `GameTestHelper`**, not a function taking a helper
parameter — this keeps `succeed()`/`assertEquals()`/etc. callable unqualified:

```kotlin
@Suppress("unused")
class ArchieItemHandlerTests {
    @GameTest(template = EMPTY)
    fun GameTestHelper.testInsertRespectsMaxStackSize() {
        val storage = ArchieItemStorage(1)
        val stone = ItemResource.of(ItemStack(Items.STONE, 1))

        val inserted = storage.insert(stone, 80, false)

        assertEquals(64L, inserted)
        assertEquals(64, storage.get(0).getItem().count)
        succeed()
    }
}
```

(`EMPTY` is `"archie:gametest/empty"`, the empty structure template most tests that don't need
actual world geometry reference.) `assertEquals`/`assertTrue`/`expectThrows` here are Archie's own
public `GameTestHelper` extension helpers (`net.kernelpanicsoft.archie.gametest.GameTestAssertions.kt`,
import them like any other Archie API), not part of vanilla — they exist purely to make failures
read like a normal assertion library instead of manually calling `fail(...)`. Consuming mods can
(and should) use them too instead of hand-rolling `if (...) fail(...)` checks.

A consuming mod writes the same shape. `Archie-Test`'s own suite exercises a real block-entity-backed
menu end to end:

```kotlin
internal object ArchieTestGameTest : AGameTestEventObject(ArchieTest.MOD) {
    override fun AEvents.ArchieGameTestBuilder.handler() = archieTestGameTests()
}

internal fun AEvents.ArchieGameTestBuilder.archieTestGameTests() {
    client {
        register<TestScreenGameTest>()
    }
}
```

(That particular suite is client-side — see [`TestScreenGameTest`](#lightweight-probe-screens-vs-world-backed-tests)
below.) A server-side suite registers the same way under `server { }`.

---

## Client GameTest DSL

Vanilla's `GameTest` framework only runs on a dedicated/integrated server — there's no equivalent
for driving a real client `Screen`. Archie's client harness fills that gap: it boots a client
(optionally with a world), executes annotated test methods against a `ClientGameTestContext`, and
reports pass/fail without any pixel-perfect rendering assumptions.

### Writing a test

Mark a method `@ClientGameTest`, written as an extension function on `ClientGameTestContext`:

```kotlin
class InputComponentsGameTest {
    @ClientGameTest
    fun ClientGameTestContext.testCheckboxHoverAndClickRenderState() {
        setScreen { InputComponentsProbeScreen() }
        waitForScreen<InputComponentsProbeScreen> {
            waitForLayer(0) {
                node("Checkbox") {
                    assertRenderState(TextureStates.DEFAULT)

                    hover()
                    waitForComposeIdle()
                    assertRenderState(TextureStates.FOCUSED)

                    click()
                    waitForComposeIdle()
                    assertRenderState(TextureStates.CLICKED_AND_FOCUSED)
                }
            }
        }
    }
}
```

Register the class under `client { register<InputComponentsGameTest>() }` in your
`AGameTestEventObject`, same as any other test class (see [Registering tests](#registering-tests)).
`AClientGameTestHarness.run` collects every `@ClientGameTest` method across the registered
classes and runs them sequentially on the client thread, logging `[ClientGameTest] PASS/FAIL`
lines per test and returning the client to the title screen afterward.

### `ClientGameTestContext`

The context passed to (or, idiomatically, received by) every client test method:

| Member | Purpose |
|---|---|
| `setScreen { MyScreen() }` | Opens a screen and waits for the client to report it active |
| `waitForScreen<S> { ... }` / `waitForScreen(Class)` | Waits for a `LayerManagerProvider` screen of type `S`, then runs a [`ComposeScreenTestContext<S>`](#finding-nodes) block against it |
| `getInput()` | Raw [`TestInput`](#raw-input-testinput) — most tests use [`TestNodeScope`](#interacting-with-a-node) instead |
| `waitForComposeIdle()` | Waits for async Compose recomposition triggered by a prior input action to settle |
| `waitFor { client -> ... }` / `waitTick()` / `waitTicks(n)` | Polls a predicate, or advances the client a fixed number of ticks |
| `computeOnClient { }` / `runOnClient { }` | Runs arbitrary code on the client thread and returns its result |
| `withWorld { }` | Opens a `TestWorldBuilder` for a singleplayer world or dedicated server (see below) |
| `takeScreenshot(name)` / `assertScreenshotEquals(...)` / `assertScreenshotContains(...)` | Pixel screenshot capture/comparison (see [Screenshot comparison](#screenshot-comparison)) |
| `assertTrue` / `assertEquals` / `fail` | Plain assertions, same shape as the server-side helpers |

### Finding nodes

Every Archie composable creates a `LayoutNode` (see [GUI](gui.md) for the layout system itself).
Nodes are located by their `name` (e.g. `"Checkbox"`, `"Button"`, `"Column"`) via
`LayoutNode.findNode`/`findAllNodes`, which walk the subtree depth-first. `ComposeScreenTestContext<S>`
(returned into `waitForScreen<S> { ... }`) is the entry point for that lookup:

```kotlin
waitForScreen<ModalComponentsProbeScreen> {
    assertEquals(1, layerCount)                    // number of layers on the LayerManager stack
    val triggers = baseLayer.rootNode { nodes("Button") }

    triggers[0] { click() }
    waitFor { _ -> layerCount == 2 }                // a modal pushed a second layer

    node("Surface", layer = LayerSelector.Top) {
        val buttons = nodes("Button")
        buttons[0] { click() }
    }
}
```

- `node(name, layer = LayerSelector.Top, timeout = ...) { ... }` waits for a descendant named
  `name` to appear on the selected layer, then runs the block against it as a `TestNodeScope`.
- `Layer.node(name) { ... }` does the same scoped to an already-resolved `Layer`.
- `layerCount`, `topLayer`, `baseLayer`, `layer(index)`, `waitForLayer(index) { ... }` navigate the
  `LayerManager` stack directly — `LayerSelector.Top` is the frontmost layer (a modal if one is
  open), `LayerSelector.Base` is always the screen's original layer.
- `hasNode(name)` checks existence without waiting or failing.

`TestNodeScope.node(name) { ... }` (and `nodes(name)`, for the rare case a subtree has more than
one match — e.g. every "Button" in a dialog's action row) do the same lookup scoped to a node's
own subtree instead of a whole layer, so nested lookups read as plain nesting:

```kotlin
node("Column") {
    node("Row") {
        assertChildNames("Box", "Text")
        node("Box") { assertHasDescendant("RadioButton") }
    }
}
```

Both `ComposeScreenTestContext` and `TestNodeScope` also support `someResolvedNode { ... }` —
`operator fun invoke` on an already-resolved `LayoutNode` — for wrapping a node pulled out of
`nodes(name)` without constructing a `TestNodeScope` by hand (as `triggers[0] { click() }` above).

### Interacting with a node

`TestNodeScope` wraps one resolved `LayoutNode` plus the enclosing `ClientGameTestContext`:

| Method | Effect |
|---|---|
| `click(button = 0)` | Clicks the node's on-screen center |
| `hover()` | Moves the cursor to the node's center without clicking |
| `pressKey(keyCode, ...)` / `type(value)` | Dispatches key/char input to the active screen (not scoped to the node — click/hover it first if it needs focus) |
| `scroll(x, y)` | Moves the cursor to the node's center, then scrolls |
| `renderState` / `assertRenderState(expected)` | Reads/asserts the node's [render-state hook](#render-state-assertions) |
| `childNames()` / `assertChildNames(vararg)` | This node's direct children's names, in composition order |
| `hasDescendant(name)` / `assertHasDescendant(name)` | Whether a named descendant exists anywhere in the subtree |
| `assertAllDescendantsSized()` | Fails if this node or any descendant has non-positive width/height |
| `nodes(name)` | Every descendant named `name`, depth-first |
| `describeTree()` | A recursive dump of the subtree, useful in custom failure messages |

`centerCoords()` (used internally by `click`/`hover`/`scroll`) calls `waitForComposeIdle()` first —
a node's very first interaction right after it's found can otherwise read a transient pre-layout
position and miss it silently.

### Raw input (`TestInput`)

`ClientGameTestContext.getInput()` exposes the lower-level primitives `TestNodeScope` builds on:
`click`/`holdMouse`/`releaseMouse`, `pressKey`/`holdKey`/`releaseKey`, `holdControl`/`holdShift`/
`holdAlt` (and their `release*` counterparts), `charTyped`/`typeChars`, `scroll`, `setCursor`/
`moveCursor`, and `clearInputs()`. Reach for this directly only when a test needs input that isn't
scoped to a single node — e.g. holding a modifier key across several node interactions, as in:

```kotlin
node("Slider") {
    hover()
    context.getInput().holdMouse(0)
    waitForComposeIdle()
    assertRenderState(TextureStates.CLICKED)
    context.getInput().releaseMouse(0)
}
```

### Render-state assertions

Stateful renderers (`Checkbox`, `Switch`, `Radio.kt`'s `RadioButton`, and similar theme-driven
composables) set `UINode.renderState` — a test-only hook — to the `TextureStates` key they most
recently resolved (e.g. `"focused"`, `"clicked_and_focused"`) just before drawing. The framework
never reads it back; it exists purely so a test can assert *which visual state a component
resolved to* without a pixel comparison:

```kotlin
node("Switch") {
    assertRenderState(TextureStates.CLICKED)   // probe's initial `switched = true`

    click()
    waitForComposeIdle()
    assertRenderState(TextureStates.DEFAULT)
}
```

This is the primary way component tests verify visual/interaction state in this codebase — it's
cheaper and far less flaky than screenshot comparison, and it fails with a readable
`expected/got` message instead of an opaque image diff.

### Lightweight probe screens vs. world-backed tests

Most component/widget tests run against a small, purpose-built `ComposeScreen` — a "probe screen"
with no world, menu, or player, just enough composition to exercise the widget under test:

```kotlin
private class InputComponentsProbeScreen(
    private val onButtonClick: () -> Unit = {},
) : ComposeScreen(Component.literal("Input Components Probe")) {
    override fun init() {
        super.init()
        start {
            Theme {
                Column {
                    Checkbox(checked = false, onCheckedChange = {})
                    Button(onClick = onButtonClick) { Text(Component.literal("Click me")) }
                }
            }
        }
    }
}
```

This is enough for anything that doesn't depend on a container menu's slot contents or a block
entity's synced state — hierarchy shape, hover/click/type behavior, render-state transitions,
scroll offsets, modal stacking.

Slot/menu rendering and block-entity-backed sync genuinely need a world instead:
`ComposeBlockContainerMenu<T : BlockEntity, SELF>` is hard-typed to a real `BlockEntity` (its
item-backed sibling, `ComposeItemContainerMenu<SELF>`, needs a real player inventory instead), and
`ComposeContainerScreen<T : ComposeContainerMenuBase<T>>` works uniformly across both, so there's
no probe-screen shortcut for either. Those tests open a real world and place a real block:

```kotlin
class TestScreenGameTest {
    @ClientGameTest
    fun ClientGameTestContext.testShowcaseScreenOpensAndConfirmDialogRoundTrips() {
        withWorld {
            withSingleplayer {
                val player = waitForPlayer()
                val pos = player.blockPosition().above()
                placeTileAndWaitForScreen<TestTile, TestScreen>(pos, BlockRegistry.TestBlock.defaultBlockState()) {
                    node("Button", layer = LayerSelector.Base) { click() }
                    waitFor { _ -> layerCount == 2 }
                    node("Button", layer = LayerSelector.Top) { click() }
                    waitFor { _ -> layerCount == 1 }
                }
            }
        }
    }
}
```

`withWorld { withSingleplayer { ... } }` opens a real singleplayer world via `TestWorldBuilder`;
`placeTileAndWaitForScreen<T, S>(pos, state)` (a `TestSingleplayerContext` extension) places the
block, waits for its block entity, opens its menu server-side, and waits for the client screen —
one call replacing what would otherwise be several manual `runOnServer`/`waitFor` steps.

For a same-JVM dedicated server instead of an integrated singleplayer server (needed for anything
that depends on a real client↔server boundary), use `withWorld { withServer(serverProperties) { ... } }`
— backed by the loader-specific `ADedicatedServerPlatform.start`/`stop`. This is a heavier,
slower path than `withSingleplayer` and only worth it when the integrated server's shortcuts
(same JVM, same thread scheduling) would mask what the test is actually checking.

### Screenshot comparison

`ClientGameTestContext.takeScreenshot(name)` captures the client's main render target to
`build/gametests/screenshots/captures/`; `assertScreenshotEquals`/`assertScreenshotContains`
compare it against a template resolved from `build/gametests/screenshots/templates/` via
`ScreenshotManager`, using either exact pixel matching (`ExactScreenshotComparisonAlgorithm`) or
fuzzy matching within a mean-squared-difference tolerance (`MeanSquaredDifferenceAlgorithm`,
`ScreenshotComparer.findInImageFuzzy`). This plumbing is real and functional, but Archie's own
suite doesn't currently ship any committed template images — there's no baseline directory checked
into the repo. In practice, node-tree assertions (`assertRenderState`, `assertChildNames`,
`assertAllDescendantsSized`) are the primary way this codebase verifies UI correctness: they're
deterministic across displays/GUI scales and fail with a specific, readable diff, where a
screenshot diff would just say "doesn't match" and require guessing which pixel region changed.
Reach for screenshot comparison only when a node-tree assertion genuinely can't express what
you're checking (e.g. a custom `Renderer` painting something that isn't a theme-state texture).

### Test layers, don't conflate

Client GameTests (`@ClientGameTest`, run via `AClientGameTestHarness`) and server GameTests
(`@GameTest`) both run inside a real launched Minecraft process, under `runGametest`/
`runGametestClient`. Separately, `common/src/test/kotlin/.../testing/GameTests.kt` and
`GuiClientHarnessTests.kt` are plain JVM-level JUnit 5 tests (`./gradlew test`), a different layer
entirely:

- `GuiClientHarnessTests` unit-tests pure helper functions (slider normalization, scrollable axis
  resolution, and similar) with no client, world, or screen involved at all.
- `GameTests` uses `GameTestRunner.tests(...)` to shell out to the loader's `runGametest`/
  `runGametestClient` Gradle tasks per `loader:side` and report each declared test as its own
  JUnit `DynamicTest`, parsed from the launched process's log output — a way to surface the real
  in-game suite's pass/fail inside a JUnit run/report. It's disabled by default (opt in with
  `-Darchie.junit.gametest=true`) since it boots a full Minecraft process per matrix entry.

---

## Running the tests

From the repo root (or `archie-test-{fabric,neoforge}` for the playground mod's own suite):

```bash
./gradlew archie-gametest-fabric:runGametest
./gradlew archie-gametest-neoforge:runGametest
./gradlew archie-gametest-fabric:runGametestClient
./gradlew archie-gametest-neoforge:runGametestClient
```

`runGametest` runs server-side `@GameTest`s; `runGametestClient` runs `@ClientGameTest`s via
`AClientGameTestHarness`. Both fail the Gradle task if any test fails.
