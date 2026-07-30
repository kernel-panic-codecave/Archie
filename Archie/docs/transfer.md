# Transfer & Inventory

Archie provides cross-platform item and fluid storage abstractions, built on top of
[Common Storage Lib](https://github.com/Fuzss/CommonStorageLib)'s resource/slot model, that work
on both Fabric and NeoForge without platform-specific code. They're designed to back a block
entity's inventory/tank and expose it through Archie's [GUI framework](gui.md) — the slot types
below plug directly into `ComposeContainerMenu`.

---

## ArchieItemStorage

`ArchieItemStorage` is Archie's platform-agnostic item container: a fixed-size list of
`ArchieItemSlot`s that implements Common Storage Lib's `CommonStorage<ItemResource>` (so it
supports resource-based `insert`/`extract` for capability interop) and Archie's `NbtTag`
serialization for save/load. Create one via `NBTHolder`:

```kotlin
class MyBlockEntity(pos, state) : NBTBlockEntity(TYPE, pos, state) {
    val items by nbt.itemField(9)  // 9-slot ArchieItemStorage
}
```

Public surface is deliberately small — `size(): Int` and `get(slot: Int): ArchieItemSlot`. Read
or mutate the `ItemStack` in a slot through the `ArchieItemSlot` it returns (see below), not
directly on the storage.

There's a fluid equivalent, `ArchieFluidStorage`, backed by `ArchieFluidSlot`s (each with a
capacity `limit`), following the same shape.

---

## ArchieItemSlot / ArchieFluidSlot

`ArchieItemSlot` is one resource-backed slot inside an `ArchieItemStorage`. It tracks an
`ItemResource` + amount internally and exposes plain `ItemStack` access:

```kotlin
val slot = myStorage[0]
val current: ItemStack = slot.getItem()
slot.set(ItemStack(Items.DIAMOND, 4))
```

`ArchieFluidSlot` is the fluid analogue, backed by `FluidResource` and a per-slot capacity limit.

---

## Wiring storage into a menu

You don't usually build `net.minecraft.world.inventory.Slot`s by hand. `ComposeContainerMenu`
(see [`gui.md`](gui.md)) takes a declarative approach instead: implement `registerSlotHandlers()`
and register each storage under a named group with `handler(group, storage, filter)`.

```kotlin
class MyMenu(
    id: Int, playerInventory: Inventory, blockEntity: MyBlockEntity,
) : ComposeContainerMenu<MyBlockEntity, MyMenu>(MY_MENU_TYPE, id, playerInventory, blockEntity) {
    override fun registerSlotHandlers() {
        handler("inventory", tile.items)   // ties the "inventory" group to the block entity's storage
    }
}
```

A `Slot` composable placed in the screen's layout (matched by group name) is what actually
determines each slot's on-screen position and how many slots from the group are shown — the menu
creates and manages the underlying `ArchieItemMenuSlot`/`VanillaMenuSlot` instances for you based
on that layout. `ArchieItemMenuSlot` bridges an `ArchieItemStorage` slot; `VanillaMenuSlot` does
the same for a Common Storage Lib `AbstractVanillaContainer`, for adapting an existing
vanilla-style container instead of an `ArchieItemStorage`. Both accept an `ItemStack -> Boolean`
filter for `mayPlace`.

See [`gui.md`](gui.md) for how `ComposeContainerMenu`/`ComposeContainerScreen` fit together.

---

## Slot composable

The `Slot` composable (`gui/Slot.kt`) renders an item-slot widget that visually tracks the
container menu's slot positions, enabling accurate item rendering inside a `ComposeContainerScreen`.
