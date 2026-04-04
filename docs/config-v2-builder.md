# Archie Config V2

Production-ready UI-decoupled config system with type-safe nested categories, constrained pickers, and multi-adapter support.

## Layout

- `model/` - UI-agnostic schema objects (`ConfigDocument`, `ConfigCategory`, `ConfigField`)
- `runtime/` - mutable runtime state and validation (`ConfigState`, `ConfigV2Engine`)
- `serializer/` - v2 load/save serializers and value codec
- `ui/` - adapter SPI (`ConfigUiAdapter`, `ConfigUiAdapters`)
- `ui/cloth/` - Cloth Config adapter implementation (`ClothConfigUiAdapter`)
- `ui/yacl/` - YACL adapter with reflective screen building (`YaclConfigUiAdapter`)
- `builder/` - type-safe delegate DSL (`ConfigDocumentDsl`, `ConfigCategoryDsl`)
- `legacy/` - bridge from current `ConfigSpec`/`CategorySpec` (`LegacyCategorySpecAdapter`)
- `legacy/` - first-run migration helper (`LegacyToV2MigrationHelper`)
- `ArchieV2Config.kt` - Archie's v2 config singleton (all fields ported from v1)
- `ConfigV2Smoke.kt` - smoke test runner

## Quick verify

```zsh
cd /home/kernelpanic/IdeaProjects/Archie
./gradlew :common:compileKotlin --no-daemon
```

## Bridge coverage

`LegacyCategorySpecAdapter` maps:

- Primitives: boolean/int/long/float/double
- Sliders: int/long slider
- String/enum/selector to choice fields
- Registry-backed: registry/registryList/registryMap (constrained dropdowns when options known)
- Keycode support: keycode/keycodeLists/keycodeMap
- Collections: lists (int/long/float/double/string/color), maps (int/long/float/double/string)
- Nested: spec/specList/specMap to category snapshots
- Color fields with alpha support

All field types can be added incrementally in the adapter without changing v2 runtime contracts.

## Delegate DSL

- `ConfigDocumentDsl` + `ConfigCategoryDsl` provide v1-like delegated fields for v2.
- Keys default to property name in snake_case (with optional explicit `key` override).
- Bind once via `createEngine()` or `bind(engine.state)`, then read anywhere from delegates.
- **Type-safe nested categories**: use `nestedCategory()` for single specs, `categoryListOf()` / `categoryMapOf()` for collections.

Example usage:

```kotlin
object MyConfig : ConfigDocumentDsl("my-mod", "My Mod") {
    val general = category(GeneralCategory())
}

class GeneralCategory : ConfigCategoryDsl("general", "General") {
    val enabled by boolean("Enabled", default = true)
    val mode by choice("Mode", options = listOf("safe", "fast"), default = "safe")
    
    // Type-safe nested category
    val advanced = nestedCategory(AdvancedCategory())
    
    // Type-safe list of categories
    val profiles by categoryListOf { ProfileCategory() }
    
    // Type-safe map of categories (parametrized)
    val configs by categoryMapOf { id -> ConfigSpecCategory(id) }
}

val engine = MyConfig.createEngine(load = true)
println(MyConfig.general.enabled)
println(MyConfig.general.advanced.debugMode)
println(MyConfig.general.profiles[0].name)
```

All nested specs are fully typed with IDE support and delegate-based field access.

## Real-world usage

`ArchieV2Config` (loaded in `Archie.init()`) provides full v2 config access for Archie:

```kotlin
if (ArchieV2Config.general.enabled) { ... }
val logLevel = ArchieV2Config.general.logLevel

val screen = ArchieV2Config.buildScreen()
```

## Load/save

- `ConfigV2Engine` now owns a platform-selected serializer:
  - Fabric -> `Json5ConfigV2Serializer` (`<id>.v2.json5`)
  - NeoForge -> `TomlConfigV2Serializer` (`<id>.v2.toml`)
- Values are persisted as `Map<String, String>` where each value is typed JSON payload
  encoded/decoded by `ConfigV2ValueCodec` based on the field schema.

## First-run migration

- Use `LegacyToV2MigrationHelper.migrateIfNeeded(spec)` after loading legacy config.
- Behavior:
  - if v2 file exists -> `SkippedExisting`
  - else -> writes v2 snapshot from current legacy values and returns `Migrated`

## Adapter notes

**Cloth Adapter** (`ClothConfigUiAdapter`):
- Renders all primitive types: boolean, int/long/float/double
- Sliders: int/long slider fields with range validation
- Choice fields: simple dropdown selectors
- Registry fields: constrained dropdown when options known, string fallback otherwise
- Registry collections: list/map with dropdown pickers for each entry
- Keycode fields: proper keycode selector UI
- Keycode collections: list/map with keycode pickers
- Color fields: color picker with optional alpha support
- Collections: int/long/float/double/string lists and maps
- Nested categories: collapsible sections in UI
- Type-safe spec collections: nested category list/map rendering

**YACL Adapter** (`YaclConfigUiAdapter`):
- Registers conditionally (only if YACL is on classpath)
- Builds screens reflectively with diagnostic error tracking
- Registry/keycode/spec families use string/JSON bindings for safe reflection


