# Config System

Archie's config system provides a hierarchical, multi-format configuration API with automatic
Cloth Config UI generation. Configs are organized into `ConfigSpec` (root) → `CategorySpec`
(sections and nested sub-sections) → individual field delegates.

---

## Formats

Three serializers are built in:

| Class | Format | File extension |
|-------|--------|---------------|
| `JsonConfigSerializer` | JSON | `.json` |
| `Json5ConfigSerializer` | JSON5 (comments allowed) | `.json5` |
| `TomlConfigSerializer` | TOML | `.toml` |

---

## Defining a config

```kotlin
object MyConfig : ConfigSpec(MyMod.MOD, Component.literal("My Config")) {
    override val categories = listOf(General, Advanced)

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

// In mod init:
MyConfig.init()
```

---

## Field types

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
| `spec` | Nested `CategorySpec` |

Each type also has list and map variants: e.g. `intList`, `intMap`, `specList`, `specMap`, `registryList`, etc.

---

## Reading values

Config values are plain Kotlin properties — just access them directly:

```kotlin
if (MyConfig.General.enableFeature) {
    processItems(MyConfig.General.maxItems)
}
```

---

## Subcategories

Use `override val subcategories` inside a `CategorySpec` for nested grouping in the UI:

```kotlin
object Parent : CategorySpec(Component.literal("Parent"), "parent") {
    override val subcategories = listOf(Child)

    object Child : CategorySpec(Component.literal("Child"), "child") {
        val value by boolean(title = Component.literal("Toggle"))
    }
}
```
