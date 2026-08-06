# GUI Framework

Archie includes a **Compose-for-Minecraft** UI framework that mirrors the structure of
Jetpack Compose. Screens, layouts, composables, modifiers, and an input event system work
together to build declarative, reactive GUIs without writing manual render code.

---

## Screens

### `ComposeScreen`

Base class for standalone Minecraft screens.

```kotlin
class MyScreen : ComposeScreen(Component.literal("My Screen")) {
    override fun init() {
        super.init()
        start {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(Component.literal("Hello, World!"))
            }
        }
    }
}
```

### `ComposeContainerScreen`

Base class for screens attached to a container menu (inventory, crafting grid, etc.).

```kotlin
class MyContainerScreen(
    menu: MyMenu, inventory: Inventory, title: Component,
) : ComposeContainerScreen<MyMenu>(menu, inventory, title) {
    override fun init() {
        super.init()
        start { MyContainerContent(menu) }
    }
}
```

---

## Layout containers

### `Box`

Stacks children on top of each other, aligned within the box.

```kotlin
Box(contentAlignment = Alignment.Center) {
    // child content
    Text(Component.literal("Overlay"))
}
```

### `Row`

Arranges children horizontally.

```kotlin
Row(
    horizontalArrangement = Arrangement.spacedBy(8),
    verticalAlignment = Alignment.CenterVertically,
) {
    // child content
    Text(Component.literal("Label"))
}
```

### `Column`

Arranges children vertically.

```kotlin
Column(verticalArrangement = Arrangement.spacedBy(4)) {
    repeat(5) { Text(Component.literal("Item $it")) }
}
```

---

## Built-in composables

| Composable | Description |
|---|---|
| `Text` | Renders a `Component` with optional scale and colour |
| `Spacer` | Fills available space (useful as a flex gap) |
| `Texture` | Blits a UV region from a texture or atlas sprite |
| `Icon` | Fixed-size wrapper around `Texture` for sprite icons |
| `Divider` | Horizontal/vertical separator line (`HorizontalDivider`, `VerticalDivider`) |
| `Scrollable` | Wraps a child in a scrollable viewport |
| `Collapsible` | Expand/collapse container with an animated arrow |
| `Panel` | Padded themed surface for grouping related controls |
| `Clickable` | Unstyled pointer interaction primitive (hover/pressed/click) |
| `ButtonCore` | Unstyled clickable container exposing hover/pressed state |
| `CheckboxCore` | Unstyled toggle exposing hover state |
| `Switch` | Boolean toggle switch with animated thumb |
| `Slider` | Drag-based normalized value control with optional step snapping |
| `RadioGroup` | Single-choice option group using radio buttons |
| `TextField` | Full controlled text input (single-line or multi-line) |
| `BasicTextField` | Simpler `String`-based text field |
| `ColorPicker` | HSV + alpha colour picker |
| `TabContainer` | Create World style tab bar |
| `AlertDialog` / `PromptDialog` / `ChoiceDialog` | Additional modal dialog primitives |

### `TabContainer`

Declarative tab strip that mimics the Create World screen. Tabs are defined as data and the
selected tab is held in a `TabContainerState`. The bar optionally scrolls when there are
more tabs than horizontal space.

```kotlin
val tabs = listOf(
    TabSpec("game", Component.translatable("createWorld.game")),
    TabSpec("world", Component.translatable("createWorld.world")),
    TabSpec("more", Component.translatable("createWorld.more")),
)
val tabState = rememberTabContainerState(tabs)

TabContainer(
    tabs = tabs,
    state = tabState,
    onTabSelected = { selected -> println("Selected tab: ${selected.id}") },
)
```

### Animation helpers

```kotlin
val y = animateInt(if (expanded) 0 else -6)
val rotation = animateFloat(
    targetValue = if (expanded) 90f else 0f,
    spec = AnimationSpec(durationMillis = 260, easing = Easings.OutBack),
)
```

---

## Modifiers

Modifiers are immutable, composable decorators applied to layout nodes via `Modifier.then(...)`.

### Layout modifiers

```kotlin
Modifier
    .size(80, 20)              // fixed size
    .width(100)                // width only
    .height(40)                // height only
    .fillMaxSize()             // fill all available space
    .fillMaxWidth(0.5)         // fill 50% of width
    .sizeIn(minWidth = 50, maxWidth = 200)
    .padding(horizontal = 8, vertical = 4)
    .margin(all = 4)
    .offset(x = 10, y = 0)
    .zIndex(2f)
```

### Appearance modifiers

```kotlin
Modifier
    .background(KColor.DARK_GRAY)
    .background(KColor.BLACK, KColor.GRAY, GradientDirection.TOP_TO_BOTTOM)
    .border(KColor.WHITE, thickness = 1)
    .texture(myResourceLocation)
    .tooltip(myTooltipComponent)
    .debug("key=value")        // shown in Ctrl+Shift debug overlay
```

### Input modifiers

```kotlin
Modifier
    .onPointerEvent<AUINode>(PointerEventType.PRESS) { node, event ->
        println("Pressed at ${event.mouseX}, ${event.mouseY}")
        event.consume()
    }
    .onPointerEvent<AUINode>(PointerEventType.ENTER) { _, _ -> hovered = true }
    .onPointerEvent<AUINode>(PointerEventType.EXIT)  { _, _ -> hovered = false }
    .onScroll<AUINode> { _, event -> scrollOffset += event.scrollY }
    .onDrag<AUINode>   { _, event -> dx += event.dragX }
    .onKeyEvent        { _, event -> if (event.keyCode == GLFW.GLFW_KEY_ESCAPE) close() }
    .onCharTyped       { _, event -> buffer += event.codePoint }
    .combinedClickable(
        onClick      = { _, _ -> println("click") },
        onDoubleClick = { _, _ -> println("double") },
        onLongClick  = { _, _ -> println("held") },
    )
```

---

## Custom rendering helpers

All built-in composables render via a `Layout(... renderer = object : Renderer { ... })`
callback that receives a `GuiGraphics`. When writing your own `Renderer` (for a custom
composable), a few extension helpers on `GuiGraphics` remove the usual boilerplate:

```kotlin
renderer = object : Renderer {
    override fun render(
        node: UINode, x: Int, y: Int,
        guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float,
    ) = guiGraphics {                  // GuiGraphics.invoke - `this` is the GuiGraphics below
        pose {                         // pushes/pops the pose stack around the block
            translate(x.toDouble(), y.toDouble(), 0.0)
            scale(1.5f, 1.5f, 1.5f)
        }
        scissor(x, y, x + node.width, y + node.height) {  // enable/disableScissor around the block
            drawString(minecraftClient.font, "Hi", x, y, KColor.WHITE.argb)
        }
    }
}
```

`pose { }` and `scissor(minX, minY, maxX, maxY) { }` (also overloaded to take an `IntRect`)
always restore the previous pose/scissor state afterwards, even if the block throws.

---

## Layer system

The layer stack enables floating overlays (modals, dropdowns, tooltips) that render on top
of the base screen and receive input first.

```kotlin
val layerManager = LocalLayerManager.current

// Generic layer
val dismiss = layerManager.push { dismiss ->
    MyDropdown(onClose = dismiss)
}

// Modal (blocks input to layers below)
layerManager.modal(
    alignment = Alignment.Center,
    dismissOnClickOutside = true,
) {
    ConfirmDialog(onConfirm = { dismiss() })
}

// New helpers
layerManager.confirmDialog(onConfirm = { doDelete() }) { Text(Component.literal("Delete this?")) }
layerManager.alertDialog(message = Component.literal("Saved"))
layerManager.promptDialog(onConfirm = { value -> println(value) })
layerManager.choiceDialog(
    choices = listOf(ModalChoice("a", Component.literal("A")), ModalChoice("b", Component.literal("B"))),
    onSelected = { choice -> println(choice) },
)

// Custom modal with built-in enter/exit animation + backdrop fade
layerManager.modal(transitionSpec = ModalTransitionSpec(durationMillis = 220)) {
    MyCustomModal()
}
```

---

## Automated GUI Testing

Archie has a from-scratch client GameTest harness for driving and asserting against composables
and screens built with this framework — clicking, hovering, typing, and reading component state
back out of a live screen. See [GameTest § Client GameTest DSL](gametest.md#client-gametest-dsl)
for the full guide.

---

## Colours

### `KColor`

```kotlin
val red    = KColor.RED
val custom = KColor.ofRgb(0xFF8000)
val semi   = KColor.ofArgb(0x80FF0000L)
val hsv    = KColor.ofHsv(0.33f, 1f, 0.8f)
val argb   = custom.argb    // Int: 0xAARRGGBB
val text   = custom.toTextColor()  // net.minecraft.network.chat.TextColor (RGB only, no alpha)
```

### `HsvColor`

Used with `ColorPicker`:

```kotlin
var color by remember { mutableStateOf(HsvColor.from(KColor.CYAN)) }
ColorPicker(
    color = color,
    modifier = Modifier.size(180, 120),
    onColorChanged = { color = it },
)
val kColor = color.toKColor()
```

---

## Debug overlay

Press **Ctrl + Shift + D** while a Compose screen is open to toggle the layout debug overlay.
Hold **Shift** (in debug mode) to inspect individual node dimensions, coordinates, and
any `debug(...)` modifier annotations.
