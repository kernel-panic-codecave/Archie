![Banner](https://cdn.modrinth.com/data/cached_images/fe4ae38fa51aedc5f0035b1eb6ee85bd16046274.png)

[![Modrinth downloads](https://badges.moddingx.org/modrinth/downloads/archie?style=flat)](https://modrinth.com/mod/archie)
[![CurseForge downloads](https://badges.moddingx.org/curseforge/downloads/1029738?style=flat)](https://www.curseforge.com/minecraft/mc-mods/archie)
[![MC versions](https://badges.moddingx.org/modrinth/versions/archie?style=flat)](https://modrinth.com/mod/archie)

<!-- Badges from https://github.com/ModdingX/ModBadges. CurseForge takes the numeric project id,
     Modrinth the slug. -->

**Archie** is a Kotlin-first library mod for Fabric and NeoForge, built on Architectury.

It carries the parts every mod ends up writing for itself — screens, config, packets, saved state,
storage capabilities — as one set of Kotlin APIs that behave the same on both loaders.

Here because another mod asked for it? Install it alongside and it does its work out of sight.

---

## Screens are Compose

Archie runs a real Compose runtime inside Minecraft, so a screen is a function of its state and
redraws itself when that state changes.

```kotlin
class MyScreen(menu: MyMenu, inventory: Inventory, title: Component) :
	ComposeContainerScreen<MyMenu>(menu, inventory, title) {
	init { start { content() } }

	@Composable
	fun content() {
		var amount by remember { mutableStateOf(0) }
		ContainerPanel(contentWidth = 18 * 9) {
			Column(verticalArrangement = Arrangement.spacedBy(6)) {
				Label(Component.literal("Amount: $amount"))
				Button(onClick = { amount++ }) { Text(Component.literal("More")) }
				Slots("input")
			}
		}
	}
}
```

The widget set covers what a machine GUI needs: **Text**, **Icon**, **Texture**, **Divider**,
**Spacer**, **ProgressBar**, **FluidTank**, **EnergyBar**; **Button**, **Checkbox**, **Switch**,
**Radio**, **Slider**, **Dropdown**, **ColorPicker**, **TextField**; and containers from **Panel**
and **TabContainer** through **Scrollable**, **Collapsible**, **Table**, **PannableCanvas** and a
**NodeTreeView** for graph-shaped data. Dialogs and multi-step **Wizard** flows come as primitives
to build from.

Modifiers chain the way they do in Compose proper — `Modifier.size(...).tooltip(...).onScroll { }` —
and layout is `Row`/`Column`/`Box` with real arrangement and alignment. Anything drawn inside a
**layer** — a modal, a popup editor — participates fully: it takes input first, and it declares its
own tooltip without the host screen knowing it exists.

Themes are data. A `ComposableTheme` names the textures and colours each widget draws with, loaded
from resource packs, so a pack restyles every Archie screen at once and a mod ships its own look
without forking a widget.

## Menus that sync themselves

`ComposeBlockContainerMenu` pairs a block entity with its screen, and `BlockEntityStateManager`
keeps every field marked `@Sync` current on the client as it changes. Slots come from real storages
— `ArchieItemSlot`, `ArchieFluidSlot`, and menu slots backed by Common Storage Lib — so a slot shows
what the storage holds.

## State that persists itself

`NBTHolder` gives a block entity or an item stack typed, delegated fields that read and write NBT on
their own:

```kotlin
class MyBlockEntity(pos: BlockPos, state: BlockState) : NBTBlockEntity(TYPE, pos, state) {
	@Sync var progress: Int by field(Int.serializer()) { 0 }
	val inventory: ArchieItemStorage by itemField(9)
	val jobs: ObservableList<Job> by listField(Job.serializer()) { emptyList() }
}
```

Fields nest, so a whole sub-object round-trips as one field. `ObservableList` and `ObservableMap`
persist every mutation made through them. `@Sync` marks what the client needs. Data attachments
cover state belonging to something you do not own.

## Config with a screen for free

A config is a Kotlin object of typed fields. Archie writes the file, generates the Cloth Config
screen, and — for a server spec — pushes the values to each client as it joins.

```kotlin
object MyConfig : ConfigContainer(MOD) {
	object Gameplay : ConfigSpec.Server(MOD, Component.literal("Gameplay")) {
		object Machines : CategorySpec(Component.literal("Machines")) {
			var speed by intSlider(
				Component.literal("Speed"),
				Component.literal("Ticks per operation."),
				min = 1, max = 200, default = 20,
			)
		}
	}
}
```

Fields cover booleans, numbers with or without sliders, strings, enums and arbitrary selectors,
colours, keybinds, lists, maps, registry entries, and nested specs.

## Packets are data classes

`NetworkChannel` registers `@Serializable` Kotlin classes, so a packet is its own wire format.

```kotlin
object MyChannel : NetworkChannel(MOD % "main") {
	fun init() {
		serverbound(SetSpeedPacket::class) { packet, context -> packet.handleOnServer(context) }
		onClient { clientbound(StatusPacket::class) { packet, _ -> packet.handleOnClient() } }
	}
}
```

## Storage and capabilities

One set of item, fluid and energy storages that expose themselves to whichever capability system the
loader has: `exposeItemStorage`, `exposeFluidStorage` and their siblings take a block entity type
and a selector, and the platform difference ends there. Registries, creative tabs and custom model
registration work the same way — declare once, get both loaders.

## Datagen and GameTest

Two further mods, dev-time only, never on a player's classpath:

- **`archie-datagen`** — a Kotlin DSL for models, blockstates, lang, tags, recipes, loot tables and
  advancements, with builders that read as the JSON they produce.
- **`archie-gametest`** — a GameTest harness with assertions, a JUnit bridge, a client-side harness
  for screens, and screenshot comparison for testing what a GUI actually draws.

## Using it

```kotlin
repositories {
	maven("https://maven.kernelpanicsoft.net/releases")
	maven("https://maven.kernelpanicsoft.net/snapshots")
}

dependencies {
	// per loader module
	modApi("net.kernelpanicsoft.archie:archie-core-fabric:<version>")

	// dev-time only
	modCompileOnly("net.kernelpanicsoft.archie:archie-core-gametest-fabric:<version>")
	modLocalRuntime("net.kernelpanicsoft.archie:archie-core-gametest-fabric:<version>")
}
```

Artifacts are `archie-core-{common,fabric,neoforge}`, with `archie-core-datagen-*` and
`archie-core-gametest-*` alongside them.

Full guides live at [docs.kernelpanicsoft.net/Archie](https://docs.kernelpanicsoft.net/Archie/) and
in [`docs/`](docs).

---

## Requirements

- Minecraft **1.21.1**
- **Fabric** or **NeoForge**
- [Architectury API](https://modrinth.com/mod/architectury-api),
  [Cloth Config](https://modrinth.com/mod/cloth-config) — plus Fabric API and Fabric Language
  Kotlin on Fabric

## Status

Pre-release. Everything above is built and in use by
[Boilerplate](https://github.com/kernel-panic-codecave/Boilerplate), which is what the API is
exercised against — so expect it to keep moving where that use finds rough edges.

Bug reports and design arguments are equally welcome on the
[issue tracker](https://github.com/kernel-panic-codecave/Archie/issues).

## Building

One Gradle build at the repo root. Four products - `core/` is the library, `datagen/` and
`gametest/` are the dev-time mods above, and `test/` is a playground used to exercise Archie during
development - each split `common`/`fabric`/`neoforge` and versioned by
[Stonecutter](https://stonecutter.kikugie.dev/), so a project path carries the Minecraft version it
is for:

```bash
./gradlew build
./gradlew :core:fabric:1.21.1:runClient
./gradlew :datagen:neoforge:1.21.1:runDatagen
./gradlew :gametest:fabric:1.21.1:runGametest        # server-side suite
./gradlew :gametest:fabric:1.21.1:runGametestClient  # client GUI harness
```

Published artifacts keep the flat names the project paths no longer have:
`archie-core-fabric`, `archie-core-gametest-neoforge`, and so on.

## Links

- [Source](https://github.com/kernel-panic-codecave/Archie)
- [Issues](https://github.com/kernel-panic-codecave/Archie/issues)
- [Documentation](https://docs.kernelpanicsoft.net/Archie/)

Licensed under [GPL-3.0-or-later](LICENSE).
