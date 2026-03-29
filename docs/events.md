# Events

Archie provides a lightweight event bus built on top of Architectury's event system.

---

## AEvents

`AEvents` is the central event registry. It is initialized automatically in `Archie.init()`
using the mod descriptor so all Architectury events are wired up for the correct mod.

```kotlin
// Accessing the event registry:
AEvents += MyMod.MOD   // registers mod-level events during init
```

---

## AEventObject

`AEventObject` is a base interface for Archie's typed event wrapper objects. Each event
object encapsulates an Architectury event and provides a concise API for registering
listeners and firing events.

### Listening to events

```kotlin
AEvents.SOMETHING += { param ->
    // handle event
}
```

### Firing events

Event firing is handled internally by Archie or platform-specific mod entry points.
External code generally only needs to register listeners.

---

## Custom events

You can create your own event objects following the `AEventObject` pattern:

```kotlin
object MyCustomEvent : AEventObject<(String) -> Unit>() {
    operator fun invoke(message: String) = fire(message)
}

// Register a listener:
MyCustomEvent += { msg -> println("Received: $msg") }

// Fire:
MyCustomEvent("Hello!")
```
