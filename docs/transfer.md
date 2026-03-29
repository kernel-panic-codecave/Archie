# Transfer & Inventory

Archie provides cross-platform item storage abstractions that work on both Fabric and
NeoForge without platform-specific code.

---

## ArchieItemStorage

`ArchieItemStorage` is Archie's platform-agnostic item container. Create one via `NBTHolder`:

```kotlin
class MyBlockEntity(pos, state) : NBTBlockEntity(TYPE, pos, state) {
    val items by nbt.itemField(9)  // 9 slots
}
```

The storage implements standard `get(slot)`, `set(slot, stack)`, and `size` semantics.

---

## ArchieItemSlot

`ArchieItemSlot` wraps an `ArchieItemStorage` slot for use in Minecraft's menu/container system.
It handles slot validation (can place, can take) and integrates with Quick Move logic.

---

## ArchieItemMenuSlot

A `Slot` subclass that bridges `ArchieItemSlot` with `AbstractContainerMenu`, enabling
your Archie-backed storage to participate in vanilla container interactions (shift-click,
quick-move, etc.).

```kotlin
class MyMenu(id: Int, playerInventory: Inventory, val te: MyBlockEntity) :
    AbstractContainerMenu(MY_MENU_TYPE, id) {

    init {
        // Add your block entity's slots
        te.items.slots.forEachIndexed { i, slot ->
            addSlot(ArchieItemMenuSlot(slot, 8 + i * 18, 20))
        }
        // Add player inventory slots (standard layout)
        addPlayerInventory(playerInventory, 8, 84)
    }
}
```

---

## Slot composable

The `Slot` composable in `gui/Slot.kt` renders an item slot widget that visually tracks
the container menu's slot positions, enabling accurate item rendering inside a `ComposeContainerScreen`.
