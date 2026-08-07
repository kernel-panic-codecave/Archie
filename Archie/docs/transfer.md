# Transfer & Inventory

Archie provides cross-platform item and fluid storage abstractions, built on top of
[Common Storage Lib](https://github.com/Fuzss/CommonStorageLib)'s resource/slot model, that work
on both Fabric and NeoForge without platform-specific code. They're designed to back a block
entity's or an item stack's inventory/tank and expose it through Archie's [GUI framework](gui.md)
— the slot types below plug directly into `ComposeContainerMenuBase` (via either
`ComposeBlockContainerMenu` or `ComposeItemContainerMenu`).

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

---

## ArchieFluidStorage

The fluid equivalent of `ArchieItemStorage`: a fixed-size list of `ArchieFluidSlot`s (each capped
at a shared `limit`), implementing Common Storage Lib's `CommonStorage<FluidResource>` and
Archie's NBT serialization, following the exact same shape:

```kotlin
class MyBlockEntity(pos, state) : NBTBlockEntity(TYPE, pos, state) {
    val tank by nbt.fluidField(FluidStack.bucketAmount() * 4)  // 1 tank slot, 4 buckets
}
```

Display it in a screen with the [`FluidTank`](gui.md#progress-energy-and-fluid-indicators)
composable, which renders the real fluid texture and tint (not a placeholder color) via a
per-loader render bridge — Fabric's `FluidRenderHandlerRegistry` and NeoForge's
`IClientFluidTypeExtensions` expose a fluid's client appearance through unrelated APIs, so this
one spot needed platform-specific code rather than a single cross-loader call.

---

## ArchieEnergyStorage

Implements Common Storage Lib's `ValueStorage` — the energy analogue of the
`CommonStorage<ItemResource>`/`CommonStorage<FluidResource>` `ArchieItemStorage`/
`ArchieFluidStorage` implement — plus Archie's NBT serialization, mirroring their shape:
a `capacity`-capped `Long` buffer with resource-style `insert`/`extract`.

Archie doesn't register this with Common Storage Lib's `EnergyApi.BLOCK`/`ITEM`/`ENTITY` lookups
for you, the same way it doesn't for `ArchieItemStorage`/`ArchieFluidStorage` today — wire that
lookup registration, and any platform-specific capability bridge (NeoForge's `IEnergyStorage`,
Fabric's Team Reborn Energy API) you still want on top of it, in your own mod. Read/write through
`getStoredAmount()`/`getCapacity()`/`insert()`/`extract()`.

```kotlin
class MyBlockEntity(pos, state) : NBTBlockEntity(TYPE, pos, state) {
    val energy by nbt.energyField(10_000)
}

val accepted = tile.energy.insert(amount = 500, simulate = false)
```

Display it with the [`EnergyBar`](gui.md#progress-energy-and-fluid-indicators) composable:
`EnergyBar(storage = tile.energy)`.

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

You don't usually build `net.minecraft.world.inventory.Slot`s by hand. `ComposeContainerMenuBase`
(see [`gui.md`](gui.md)) takes a declarative approach instead: implement `registerSlotHandlers()`
and register each storage under a named group with `handler(group, storage, filter)`. The same
mechanism works identically whether the menu is backed by a block entity
(`ComposeBlockContainerMenu`) or an item stack (`ComposeItemContainerMenu`) — only how you get at
the storage in the first place differs.

```kotlin
// Block-entity-backed
class MyMenu(
    id: Int, playerInventory: Inventory, blockEntity: MyBlockEntity,
) : ComposeBlockContainerMenu<MyBlockEntity, MyMenu>(MY_MENU_TYPE, id, playerInventory, blockEntity) {
    override fun registerSlotHandlers() {
        handler("inventory", tile.items)   // ties the "inventory" group to the block entity's storage
    }
}

// Item-backed (e.g. a backpack) - see gui.md's ComposeItemContainerMenu section for the full shape
class MyBackpackMenu(
    id: Int, playerInventory: Inventory, access: ItemContainerAccess,
) : ComposeItemContainerMenu<MyBackpackMenu>(MY_BACKPACK_MENU_TYPE, id, playerInventory, access) {
    override fun registerSlotHandlers() {
        handler("inventory", holder.itemField(27))
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

See [`gui.md`](gui.md) for how `ComposeContainerMenuBase`/`ComposeContainerScreen` fit together.

---

## Slot composable

The `Slot` composable (`gui/Slot.kt`) renders an item-slot widget that visually tracks the
container menu's slot positions, enabling accurate item rendering inside a `ComposeContainerScreen`.
