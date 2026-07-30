# Events

`AEvents` is Archie's internal event-registration hub, built on top of Architectury's event
system. It is **not** a general-purpose event bus for arbitrary mod events — it's the plumbing
that lets Archie's own datagen and GameTest systems discover per-mod hooks. If you're looking to
fire your own custom gameplay events, use Architectury's `EventFactory` directly.

---

## Opting a mod in

Every mod that uses Archie's datagen or GameTest support must register itself once, typically in
`init()`:

```kotlin
AEvents += MyMod.MOD   // equivalent to AEvents.register(MyMod.MOD)
```

This adds the mod to `AEvents.MODS` so the two built-in events below know which mods to invoke
handlers for.

---

## Built-in events

`AEvents` exposes two Architectury `Event`s:

| Event | Fired when | Handler type |
|---|---|---|
| `GATHER_DATA` | A datagen run starts | `GatherDataHandler` |
| `REGISTER_GAME_TEST` | GameTest classes are being collected for registration | `RegisterGameTestHandler` |

You don't normally interact with these directly — Archie's own `ADataGeneratorPlatform` and
`AGameTestPlatform` systems invoke them on the platform (fabric/neoforge) side, once per
registered mod.

---

## `AEventObject` / `ABasicEventObject`

These are the base classes Archie itself uses to wire a mod-scoped Architectury event into a
concrete handler:

- `ABasicEventObject<T>` — the simple case: register a single fixed `handler` against an `event`.
- `AEventObject<T, H, C>` — the mod-scoped case used for `GATHER_DATA`/`REGISTER_GAME_TEST`:
  `H` is a `Handler<T>` type and `C` its `HandlerConstructor`, so the same event can carry
  independent handlers for multiple mods without them stepping on each other.

Archie's own `ADatagenEventObject` and `AGameTestEventObject` (in the `data` and `gametest`
packages) are thin `AEventObject` subclasses that bind to `GATHER_DATA` and `REGISTER_GAME_TEST`
respectively. `ArchieDatagen` and `ArchieGameTest` (internal singletons) subclass those in turn to
register Archie's own datagen providers and internal GameTest suite. These base classes are mostly
useful as a reference if you need to build similar mod-scoped Architectury event plumbing of your
own — most consumers will never need to touch them.
