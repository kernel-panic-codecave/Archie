# Config V2 (UI-Decoupled)

This is the first phase of a full config rewrite to decouple model/runtime from UI toolkits.

## What is in place

- `common` now has a UI-agnostic config model under `config/v2/model`.
- Delegate DSL exists under `config/v2/builder` for v1-style property access.
- Runtime state/validation is in `config/v2/runtime`.
- Platform load/save serializers are in `config/v2/serializer`.
- UI adapters are abstracted by `config/v2/ui/ConfigUiAdapter`.
- First adapter implementation exists at `config/v2/ui/cloth/ClothConfigUiAdapter`.
- YACL scaffold exists at `config/v2/ui/yacl/YaclConfigUiAdapter` with runtime classpath detection.
- A legacy bridge exists in `config/v2/legacy/LegacyCategorySpecAdapter`.

Persistence status:

- `ConfigV2Engine.load()` / `save()` are implemented.
- Fabric writes `<documentId>.v2.json5`.
- NeoForge writes `<documentId>.v2.toml`.
- Stored values are schema-aware encoded strings via `ConfigV2ValueCodec`.

Migration status:

- `LegacyToV2MigrationHelper.migrateIfNeeded(spec)` is available.
- It performs first-run migration from loaded legacy values to v2 files.
- Existing v2 files are preserved unless migration is forced.

## Why this exists

The old system directly references Cloth Config classes from common code (`CategorySpec`).
That creates classloading risk and blocks alternate UIs like YACL.

## Current scope

Phase 1 intentionally supports a subset in the legacy adapter:

- boolean/int/long/float/double
- string
- enum/selector mapped to choices
- registry/registryList/registryMap mapped to resource location strings
- spec/specList/specMap mapped to nested snapshot payloads
- lists: int/long/float/double/string/color
- maps: int/long/float/double/string
- color (converted to `KColor`)

Current Cloth adapter widget coverage:

- boolean/int/long/float/double
- int/long sliders
- enum/choice dropdown
- registry field (dropdown when options exist, string fallback otherwise)
- registry list/map via string list/map editors
- spec via map editor
- specList/specMap via JSON-backed string editors
- lists: int/long/float/double/string
- maps: int/long/float/double/string
- color + color-list editing

YACL status:

- adapter id + preferred-order integration is in place
- registration is conditional via `registerIfAvailable()`
- reflective builder is in place with diagnostics
- registry/spec families are editable via string/JSON bindings

Additional field types can be added incrementally without changing the v2 runtime API.

## Delegate Builder (v1-style ergonomics)

- Use `ConfigDocumentDsl` and `ConfigCategoryDsl` to define fields as delegated properties.
- Property names default to snake_case config keys unless `key` is provided.
- After `createEngine()` (or `bind(engine.state)`), delegates read directly from bound `ConfigState`.

```kotlin
object MyConfig : ConfigDocumentDsl("my-mod", "My Mod") {
	val general = category(object : ConfigCategoryDsl("general", "General") {
		val enabled by boolean(title = "Enabled", default = true)
		val retries by int(title = "Retries", default = 2)
		val mode by choice(title = "Mode", options = listOf("safe", "fast"), default = "safe")
	})
}

val engine = MyConfig.createEngine(load = true)
println(MyConfig.general.enabled)
```

## Real-World Usage: ArchieV2Config

The main Archie config is now available as `ArchieV2Config` for v2 migration testing and usage:

```kotlin
// Access config values from anywhere in the mod:
if (ArchieV2Config.general.enabled) {
	println("Archie is enabled")
}

val logLevel = ArchieV2Config.general.logLevel

// Build UI screen (in client code):
val screen = ArchieV2Config.buildScreen()
if (screen != null) {
	Minecraft.getInstance().setScreen(screen as Screen)
}
```

Config is loaded and delegates are bound during `Archie.init()`, so it's ready immediately.

## Nested Categories (Specs)

The delegate DSL supports nested categories for organizing complex configs:

```kotlin
// Access nested spec values
if (ArchieV2Config.test.testSpec.test) {
	println("TestSpec is enabled")
}

// Access deeply nested structures
val childValue = ArchieV2Config.test.testNestedSpec.childrenList.firstChild.childValue
val childName = ArchieV2Config.test.testNestedSpec.childrenList.firstChild.childName

// Access subcategories
val subTest = ArchieV2Config.test.testSub.test
val subRegistry = ArchieV2Config.test.testSub.testRegistry
```

Nested categories are rendered as collapsible sections in the UI and persist their values alongside top-level fields.

## Type-Safe Category Collections

Define strongly-typed collections of nested specs for repeatable structured data:

```kotlin
class MyCategory : ConfigCategoryDsl("my_category", "My Category") {
    // List of type-safe nested categories
    val itemsList by categoryListOf { ItemSpec() }
    
    // Map of type-safe nested categories (keyed by string)
    val itemsMap by categoryMapOf { id -> ItemSpec(id) }
}

// Define the nested spec type
class ItemSpec(id: String = "") : ConfigCategoryDsl("item_$id", "Item $id") {
    val name by string(title = "Name", default = id)
    val enabled by boolean(title = "Enabled", default = true)
    val value by int(title = "Value", default = 0)
}

// Access from gameplay code with full type safety
val items: MutableList<ItemSpec> = MyCategory.itemsList
items.add(ItemSpec("first"))
items.add(ItemSpec("second"))

val namedItems: MutableMap<String, ItemSpec> = MyCategory.itemsMap
namedItems["config_a"] = ItemSpec("a")
namedItems["config_b"] = ItemSpec("b")

// Access nested fields with full IDE support
MyCategory.itemsList[0].name    // String
MyCategory.itemsList[0].enabled // Boolean
MyCategory.itemsMap["config_a"].value // Int
```

Each category in the list/map is a **fully typed** `ConfigCategoryDsl` instance with all its fields accessible via delegates. This gives you complete type safety and IDE autocomplete for deeply nested structures.

## Next phases

1. Add remaining field conversions in `LegacyCategorySpecAdapter`.
2. Implement a Fabric/NeoForge client adapter for Cloth Config.
3. Implement a YACL adapter.
4. Flip `AConfigPlatform` screen registration to route through adapter selection.
5. Deprecate old UI-coupled builder path.


