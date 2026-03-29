# Registries

Archie provides two complementary registry abstractions: the map-backed **`ADeferredRegistryHolder`**
(Archie's original design) and the simpler **`RegistryHelper`** family (merged from klib).
Both wrap Architectury's `DeferredRegister` and support the `by register(...)` delegation idiom.

---

## ADeferredRegistryHolder

A registry holder that also exposes all entries as a `Map<ResourceLocation, RegistrySupplier<T>>`.
This is useful when you need to iterate over registered entries at runtime.

```kotlin
object MyItems : ADeferredRegistryHolder<Item>(MyMod.MOD, Registries.ITEM) {
    val MY_ITEM    by register("my_item")    { Item(Item.Properties()) }
    val OTHER_ITEM by register("other_item") { Item(Item.Properties()) }
}

// In mod init:
MyItems.init()

// Runtime lookup by string id:
val item: RegistrySupplier<out Item>? = MyItems["my_item"]
```

---

## RegistryHelper

A lighter base class that skips the `Map` overhead. Use it when you don't need runtime lookup.

```kotlin
object MyItems : RegistryHelper<Item>(DeferredRegister.create(modId, Registries.ITEM)) {
    val MY_ITEM by register("my_item") { Item(Item.Properties()) }
}

MyItems.init()
```

---

## BlockRegistryHelper

Extends `RegistryHelper` with automatic `BlockItem` registration alongside each block.

```kotlin
object MyBlocks : BlockRegistryHelper<Block>(modId) {
    // Registers the block AND a plain BlockItem with the same id
    val MY_BLOCK by block("my_block") { MyBlock(BlockBehaviour.Properties.of()) }

    // Custom BlockItem supplier
    val SPECIAL by block(
        "special",
        itemSupplier = { block, props -> MySpecialItem(block, props) },
    ) { SpecialBlock(BlockBehaviour.Properties.of()) }

    // Block without an item (pass null)
    val TECHNICAL by block("technical", itemSupplier = null) { TechnicalBlock(BlockBehaviour.Properties.of()) }
}

MyBlocks.init()
```

---

## CreativeTabRegistryHelper

Registers `CreativeModeTab`s via Architectury's `CreativeTabRegistry`.

```kotlin
object MyTabs : CreativeTabRegistryHelper<CreativeModeTab>(modId) {
    val MY_TAB by create("my_tab") {
        title(Component.translatable("itemGroup.mymod.my_tab"))
        icon { ItemStack(MyBlocks.MY_BLOCK) }
        displayItems { _, output ->
            output.accept(MyBlocks.MY_BLOCK)
            output.accept(MyItems.MY_ITEM)
        }
    }
}

MyTabs.init()
```

---

## Extensions

The `registries/extensions.kt` file provides convenience helpers for the Architectury
`DeferredRegister` API in idiomatic Kotlin style.
