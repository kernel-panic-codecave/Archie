# Data Generation

Archie's datagen system is a Kotlin DSL over vanilla's `DataProvider` machinery (blockstates,
models, languages, recipes, tags) plus a cross-loader condition system for gating datapack
entries. It hooks into Fabric's and NeoForge's separate native datagen entrypoints once, in
platform code, so mod authors write one `ADatagenEventObject` and never touch either loader's
datagen API directly.

---

## Running datagen

Datagen runs as a separate Gradle run configuration per loader, from the repo root (or
`archie-test-{fabric,neoforge}` for the playground mod):

```bash
./gradlew archie-datagen-fabric:runDatagen
./gradlew archie-datagen-neoforge:runDatagen
```

This sets the `archie.datagen` system property, which `ADataGeneratorPlatform.isDataGen` reads to
decide whether to run datagen registration at all, plus `archie.datagen.client`/
`archie.datagen.server`, which `ADataGenerator.isClient`/`isServer` read to decide whether
client-only providers (models, languages) or server-only providers (tags, recipes) run in this
particular invocation — a dedicated server datagen run skips client providers and vice versa.

---

## Wiring a mod in

A mod opts into datagen the same way it opts into [events](events.md) generally: register with
`AEvents`, then subclass `ADatagenEventObject` to hook `AEvents.GATHER_DATA` and call `.init()`
once `ADataGeneratorPlatform.isDataGen` is true. `ADatagenEventObject` is a thin
`ADatagenEventObject`/`AEventObject` binding — see [events.md](events.md) for how the underlying
mod-scoped event plumbing works.

```kotlin
internal object MyModDatagen : ADatagenEventObject(MyMod.MOD) {
    override fun ADataGenerator.handler() {
        client { /* models, languages */ }
        common { /* tags, recipes */ }
    }
}

object MyMod {
    fun init() {
        AEvents += MOD
        if (ADataGeneratorPlatform.isDataGen)
            MyModDatagen.init()
    }
}
```

This mirrors Archie's own wiring in `Archie.kt` (`if (ADataGeneratorPlatform.isDataGen)
ArchieDatagen.init()`) and the test mod's `ArchieTest.init()`.

---

## The `ADataGenerator` DSL

`handler()` receives an `ADataGenerator` and organizes providers into `client { }` and
`common { }` scopes, since client-only and server-only providers must be skippable independently
(see [Running datagen](#running-datagen)). Each DSL method registers a provider (gated by
`isClient`/`isServer`) and returns it, so later providers can depend on earlier ones (e.g. item
tags copying from block tags):

| Scope | Method | Registers |
|---|---|---|
| `client` | `blockStates { }` | `ABlockStateProvider` |
| `client` | `itemModels { }` | `AItemModelProvider` |
| `client` | `blockModels { }` | `ABlockModelProvider` |
| `client` | `languages { }` | `ALanguageProvider` |
| `common` | `blockTags { }` | `ATagsProvider.BlockTagsProvider` |
| `common` | `itemTags { }` | `ATagsProvider.ItemTagsProvider` |
| `common` | `biomeTags { }` | `ATagsProvider.BiomeTagsProvider` |
| `common` | `entityTags { }` | `ATagsProvider.EntityTypeTagsProvider` |
| `common` | `fluidTags { }` | `ATagsProvider.FluidTagsProvider` |
| `common` | `recipes { }` | `ARecipeProvider` |

For anything not covered above, `addProvider { output -> MyProvider(output) }` (or the
`HolderLookup.Provider`-aware overload) registers an arbitrary `DataProvider`, gated by an
explicit `run: Boolean` you pass yourself.

Every Archie data provider implements `IADataProvider`, which adds `mod`, `exitOnError`, and the
`modLoc`/`mcLoc` resource-location helpers used throughout the DSL below. When `generate()` throws,
a provider logs the error and continues (or calls `exitProcess(-1)` if `exitOnError` is set)
instead of crashing the whole datagen run.

---

## Client model data

`ABlockStateProvider` builds `blockstates/*.json`, and owns an embedded `ABlockModelProvider` and
`AItemModelProvider` (reachable via `blockModels { }`/`itemModels { }` inside it) so a block's
state, model, and item model can be declared together. Register variants with `getVariantBuilder`
(a `variants` blockstate) or `getMultipartBuilder` (a `multipart` blockstate); both return a
builder keyed by block property combinations. Vanilla-shape helpers — `simpleBlock`,
`simpleBlockWithItem`, `axisBlock`/`logBlock`, `stairsBlock`, `slabBlock`, `fenceBlock`,
`fenceGateBlock`, `wallBlock`, `paneBlock`, `doorBlock`, `trapdoorBlock`, `buttonBlock`,
`pressurePlateBlock`, `signBlock` — mirror NeoForge's vanilla `BlockStateProvider` datagen helpers
1:1 in name and parameters. Blocks passed in are typically ones registered through a
[registry helper](registries.md).

```kotlin
client {
    blockStates { output ->
        object : ABlockStateProvider(output, MyMod.MOD, false) {
            override fun generate() {
                simpleBlockWithItem(MyBlocks.MY_BLOCK)
                stairsBlock(MyBlocks.MY_STAIRS, blockTexture(MyBlocks.MY_BLOCK))
            }
        }
    }
    itemModels { output ->
        object : AItemModelProvider(output, MyMod.MOD, false) {
            override fun generate() {
                withExistingParent("my_item", "item/handheld")
            }
        }
    }
}
```

Below the blockstate layer, `AModelProvider<T>` (subclassed as `ABlockModelProvider`/
`AItemModelProvider`) builds the individual model JSONs via `getBuilder`/`withExistingParent` plus
vanilla-shape helpers (`cubeAll`, `cubeColumn`, `orientable`, `stairs`, `slab`, `fenceGate`,
`trapdoorBottom`, ...), again matching NeoForge's `ModelProvider` 1:1. Each model builder
(`AModelBuilder`, via `ABlockModelBuilder`/`AItemModelBuilder`) configures `parent`, `texture`,
`renderType`, inline `element`s, display `transforms`, and (on Forge-like loaders) a
`customLoader` (`ACustomLoaderBuilder`) replacing vanilla geometry entirely. `AConfiguredModel`
wraps a model reference with rotation/weight/uvlock for use in a blockstate variant.
`AModelFile` is just a resource-location reference to a model, usable as a `parent` or a variant's
model without requiring the model be built by the same provider. `AVariantBlockStateBuilder`
requires every possible `BlockState` of the owning block to be covered before it serializes —
use `forAllStates`/`forAllStatesExcept` to cover every combination at once, or `partialState()` +
`setModels`/`addModels` one combination at a time. `AMultiPartBlockStateBuilder`'s parts apply
their models when their `condition`/`nestedGroup` (AND/OR) clauses match instead.

Root-level model transforms and the `"TRSR"` transform JSON format used by `rootTransforms` are
backed by `TransformationHelper` (interpolation, quaternion, and Gson-deserializer helpers for
`Transformation`) — most model code never needs to touch it directly.

---

## Language and translations

`ALanguageProvider` writes `assets/<mod_id>/lang/<locale>.json`. Call `add` (or the
`Block`/`Item`/`ItemStack`/`MobEffect`/`EntityType` convenience overloads, which translate the
target's vanilla `descriptionId`) inside `generate()`; a duplicate key throws.

```kotlin
client {
    languages { // defaults to "en_us"
        add(MyBlocks.MY_BLOCK, "My Block")
        add(MyItems.MY_ITEM, "My Item")
        add("mymod.some.key", "Some Text")
    }
}
```

---

## Recipes

`ARecipeProvider` wraps vanilla's `RecipeProvider`. Build recipes with the `shaped`/`shapeless`/
`smelting`/`blasting`/`smoking`/`cooking` DSL builders (or vanilla's own `RecipeBuilder`s
directly), then `save` into the given `RecipeOutput`. `unlockedBy(ingredient: ItemLike)`/
`unlockedBy(tag: TagKey<Item>)` extension functions add a criterion named after the
ingredient/tag automatically.

```kotlin
common {
    recipes { recipeOutput ->
        shaped {
            category = RecipeCategory.MISC
            result = MyItems.MY_ITEM
            count = 4
            pattern {
                +"XXX"
                +"X X"
                +"XXX"
            }
            key { 'X' to MyItems.INGREDIENT }
        }.unlockedBy(MyItems.INGREDIENT).save(recipeOutput)

        shapeless {
            category = RecipeCategory.MISC
            result = MyItems.OTHER_ITEM
            ingredients {
                2 of MyItems.INGREDIENT
                1 of ItemTags.PLANKS
            }
        }.save(recipeOutput)
    }
}
```

`IARecipeBuilder.save(recipeOutput, id = null) { ... }` attaches an `IACondition` (built with
`AConditionBuilder` in scope) to the saved recipe — see [Conditions](#conditions).

### Custom ingredients

`IACustomIngredient` lets a mod define recipe-matching behavior beyond vanilla `Ingredient`
(ported from Fabric's custom ingredient API to work cross-loader). Implement `test`,
`matchingStacks`, `requiresTesting`, and `serializer`, then convert to a vanilla `Ingredient` via
the `.vanilla` property so it can be used anywhere an `Ingredient` is expected. Archie registers
its own built-ins on init (`ABuiltinIngredients.init()`, called from `Archie.init()`):

| Type | Matches |
|---|---|
| `AAllIngredient` | Every sub-ingredient matches (AND) |
| `AAnyIngredient` | At least one sub-ingredient matches (OR) |
| `AComponentsIngredient` | A base ingredient, plus a required data-component patch |
| `ACustomDataIngredient` | A base ingredient, plus a partial `minecraft:custom_data` NBT match |

`ACombinedIngredient` is the shared base that `AAllIngredient`/`AAnyIngredient` build on.

```kotlin
val onlyStrippedLogs: Ingredient = AAllIngredient.of(
    Ingredient.of(ItemTags.LOGS),
    Ingredient.of(MyItemTags.STRIPPED),
)
```

---

## Tags

`ATagsProvider<T>` wraps vanilla's `TagsProvider`; use the `BlockTagsProvider`/`ItemTagsProvider`/
`FluidTagsProvider`/`EntityTypeTagsProvider`/`BiomeTagsProvider` subclasses registered via the
`blockTags`/`itemTags`/`fluidTags`/`entityTags`/`biomeTags` DSL methods. An `ItemTagsProvider`
constructed with a `blockTagsProvider` (which the `ADataGenerator.Common.itemTags` DSL wires up
automatically when `blockTags { }` was called first in the same `common { }` block) gets `copy`,
mirroring a block tag into an item tag.

Inside `generate`, calling a `TagKey<T>` builds (or reuses) its `IATagBuilder<T>` — either
explicitly via `invoke`/`invoke { }`, or through operator shorthand:

| Operator | Effect |
|---|---|
| `tag += element` / `tag += ResourceKey` / `tag += ResourceLocation` | `add` |
| `tag += otherTag` (`TagKey<T>`) | `addTag` — a nested tag reference |
| `tag *= element` / `tag *= otherTag` | `addOptional`/`addOptionalTag` |

```kotlin
val MY_LOGS: TagKey<Block> = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(MyMod.MOD_ID, "my_logs"))
val MY_LOGS_ITEM: TagKey<Item> = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(MyMod.MOD_ID, "my_logs"))

common {
    blockTags { registries ->
        MY_LOGS += MyBlocks.MY_LOG
        MY_LOGS += BlockTags.LOGS // nests a reference to the vanilla tag, not its elements
    }
    itemTags { registries ->
        copy(MY_LOGS, MY_LOGS_ITEM)
    }
}
```

`ACommonTags` holds `TagKey` constants for the `c:` (common) convention tags shared across the
modding ecosystem, grouped by registry (`ACommonTags.Blocks`, `.Items`, `.Fluids`,
`.EntityTypes`, `.Biomes`) — use these directly instead of redeclaring the same conventional tags,
e.g. `ACommonTags.Items.INGOTS_COPPER`.

---

## Conditions

`IACondition` is a cross-loader condition, evaluated at datapack load time, that gates whether the
entry it's attached to (currently: a recipe, via `IARecipeBuilder.save`) is active — mirroring
Fabric's/NeoForge's native condition systems but decoded through one shared `IACondition.CODEC` so
the same condition classes work on both loaders. `AConditionBuilder` is a DSL for building
condition trees with infix combinators; it's the implicit receiver inside a `save { }` condition
block or `buildCondition { }`.

| Function/operator | Condition |
|---|---|
| `mod("modid", ...)` | `AModLoadedCondition` — every mod id is loaded |
| `registry(registryKey, id, ...)` | `ARegistryCondition` — every id is registered |
| `platform(FABRIC \| NEOFORGE)` | `APlatformCondition` — running loader matches |
| `TRUE` / `FALSE` | `ATrueCondition` / `AFalseCondition` |
| `a and b`, `a or b`, `a xor b`, `a eql b` | `AAndCondition`/`AOrCondition`/`AXorCondition`/`AEqualsCondition` |
| `!a`, `a nand b`, `a nor b`, `a xnor b`, `a neql b` | Negated forms (`ANotCondition` wrapping the above) |

```kotlin
shaped {
    category = RecipeCategory.MISC
    result = MyItems.MY_ITEM
    pattern { +"X" }
    key { 'X' to Items.DIAMOND }
}.save(recipeOutput) {
    mod("architectury") and platform(FABRIC)
}
```

Register a custom condition type with `IACondition.register(identifier, codec)`, mirroring how
`ABuiltinConditions.init()` registers Archie's own set during `Archie.init()`.
