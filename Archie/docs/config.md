# Config System

Archie's config system provides a hierarchical, multi-format configuration API with automatic
Cloth Config UI generation and optional client↔server sync. Configs are organized into
`ConfigContainer` (per-mod, one or more configs) → `ConfigSpec` (a config's load timing/lifecycle)
→ `CategorySpec` (sections and nested subsections) → individual field delegates.

---

## Formats

Three serializers are built in:

| Class | Format | File extension |
|-------|--------|---------------|
| `JsonConfigSerializer` | JSON | `.json` |
| `Json5ConfigSerializer` | JSON5 (comments allowed) | `.json5` |
| `TomlConfigSerializer` | TOML | `.toml` |

Fabric defaults to JSON5, NeoForge defaults to TOML. Override `fileSerializer` on a `ConfigSpec`
to force a specific format.

---

## Defining a config

Every mod declares exactly one `ConfigContainer` — a singleton `object` that aggregates one or
more `ConfigSpec`s (discovered automatically from nested objects, same as categories below).
Each `ConfigSpec` subclasses one of four types depending on when it should load:

| Type | Loads | Where |
|------|-------|-------|
| `ConfigSpec.Startup` | Eagerly, synchronously, inside `init()` | Global config folder |
| `ConfigSpec.Common` | `LifecycleEvent.SETUP` (both sides) | Global config folder |
| `ConfigSpec.Client` | `ClientLifecycleEvent.CLIENT_SETUP` (client only) | Global config folder |
| `ConfigSpec.Server` | `LifecycleEvent.SERVER_BEFORE_START` | Per-world `serverconfig/`, synced to clients over the network |

```kotlin
object Config : ConfigContainer(MyMod.MOD) {
    object MyConfig : ConfigSpec.Common(MyMod.MOD, Component.literal("My Config")) {
        object General : CategorySpec(Component.literal("General"), "general") {
            val enableFeature by boolean(title = Component.literal("Enable Feature"), default = true)
            val maxItems      by int(title = Component.literal("Max Items"), default = 64)
            val prefix        by string(title = Component.literal("Prefix"), default = "prefix")
        }

        object Advanced : CategorySpec(Component.literal("Advanced"), "advanced") {
            override val isEnabled get() = General.enableFeature  // hide when parent is off

            val threshold by float(title = Component.literal("Threshold"), default = 0.5f)
            val mode      by enumSelector(
                title  = Component.literal("Mode"),
                kclass = Mode::class,
                default = Mode.FAST,
            )

            enum class Mode { FAST, SAFE }
        }
    }
}

// In mod init (common side, both physical sides):
Config.init()
```

`categories` (on `ConfigSpec`) and `configs` (on `ConfigContainer`) are **not** overridden lists —
they're discovered by reflecting over nested objects that subclass `CategorySpec` / `ConfigSpec`
respectively. Just declare the nested `object`s; there's nothing else to wire up.

`ConfigContainer.init()` is the one entry point to call from common mod init. It initializes every
nested `ConfigSpec` and, on the client, registers the Cloth Config screen(s) — there is no separate
client-side init step.

### Multiple configs in one container

A container can hold more than one spec, e.g. separate `Common`/`Client`/`Server` configs:

```kotlin
object Config : ConfigContainer(MyMod.MOD) {
    object Common : ConfigSpec.Common(MyMod.MOD) { /* ... */ }
    object Client : ConfigSpec.Client(MyMod.MOD) { /* ... */ }
    object Server : ConfigSpec.Server(MyMod.MOD) { /* ... */ }
}
```

If the container has exactly one `ConfigSpec`, its screen opens directly. With more than one, the
container screen shows an "Edit" entry per spec that drills into that spec's own screen (a
`Server` spec's entry is hidden while not in a world, since it has nothing to load yet).

### Server configs sync over the network

A `ConfigSpec.Server` lives in the world save's `serverconfig/` folder rather than the shared
config folder, and is `synchronized`: the server pushes it to each joining player (skipped in
singleplayer), and a client saving changes sends them back to the server to persist and apply,
instead of writing a local file. No extra wiring is needed for this — it falls out of subclassing
`ConfigSpec.Server`.

---

## Field types

Every field-declaration function also takes a `needsRestart: Boolean = false` parameter, wired to
Cloth Config's "requires restart" indicator on that field.

| Method | Type |
|--------|------|
| `boolean` | `Boolean` |
| `int` | `Int` |
| `long` | `Long` |
| `float` | `Float` |
| `double` | `Double` |
| `string` | `String` |
| `intSlider` | `Int` (with min/max) |
| `longSlider` | `Long` (with min/max) |
| `color` | `Color` (ARGB) |
| `keycode` | `ModifierKeyCode` |
| `registry` | Any registry entry |
| `enumSelector` | Any `enum class` |
| `selector` | Arbitrary list of values |
| `spec` | Nested `DataSpec` |

Most types also have list and map variants: `intList`/`intMap`, `longList`/`longMap`,
`floatList`/`floatMap`, `doubleList`/`doubleMap`, `stringList`/`stringMap`, `specList`/`specMap`,
`registryList`/`registryMap`, `keycodeList`/`keycodeMap`, `colorList`/`colorMap`. `boolean`,
`intSlider`, `longSlider`, `enumSelector`, and `selector` don't have list/map variants.

---

## Reading values

Config values are plain Kotlin properties — just access them directly:

```kotlin
if (MyConfig.General.enableFeature) {
    processItems(MyConfig.General.maxItems)
}
```

---

## `CategorySpec` vs `DataSpec`

`DataSpec` is the base class that actually owns the field-declaration DSL (`boolean()`, `int()`,
`spec()`, ...). `CategorySpec` is a thin `DataSpec` subclass that additionally supports
`subcategories` for UI grouping under a `ConfigSpec`.

Use `DataSpec` directly (not `CategorySpec`) for nested value types passed to `spec`/`specList`/
`specMap` — these aren't top-level config sections, so they don't need `subcategories`:

```kotlin
class Entry : DataSpec(Component.literal("Entry")) {
    val amount by int(title = Component.literal("Amount"), default = 1)
}

object General : CategorySpec(Component.literal("General"), "general") {
    val entries by specList(title = Component.literal("Entries"), factory = ::Entry)
}
```

---

## Subcategories

Use nested `CategorySpec` objects for nested grouping in the UI — like `categories`, `subcategories`
is discovered automatically, not overridden:

```kotlin
object Parent : CategorySpec(Component.literal("Parent"), "parent") {
    object Child : CategorySpec(Component.literal("Child"), "child") {
        val value by boolean(title = Component.literal("Toggle"))
    }
}
```

---

## Cloth Config UI

You don't build the settings screen yourself. `ConfigContainer.client` lazily builds a
`ClientConfigContainer`, which builds a `ClientConfigSpec` per config (and each `DataSpec` a
matching `ClientDataSpec`) that mirrors your spec into a Cloth Config `ConfigBuilder` — one
category per enabled entry. Saving routes through `ConfigSpec.save()` for `Common`/`Client`/
`Startup` configs, or over the network via `ConfigSpec.Server`'s channel for `Server` configs.
`ConfigContainer.init()` registers the resulting screen via Architectury's
`Mod.registerConfigurationScreen`, which surfaces it wherever the platform normally exposes a mod's
config screen — Mod Menu's mod list on Fabric, the vanilla mod list's "Config" button on NeoForge.
There's no more Archie-specific Mod Menu/Catalogue entrypoint to register yourself; the dedicated
`ArchieModMenu`/`ArchieCatalogue` bridge classes were retired along with the old
`AConfigPlatform.registerScreenHandler` mechanism they depended on, and Catalogue no longer has a
working integration path as a result.
