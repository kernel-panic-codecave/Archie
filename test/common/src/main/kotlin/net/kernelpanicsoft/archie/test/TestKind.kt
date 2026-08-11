package net.kernelpanicsoft.archie.test

import net.minecraft.network.chat.Component

enum class TestKind(
    val title: Component,
    val subtitle: Component,
) {
    LAYOUTS(
        title = Component.literal("Layouts + Styling"),
        subtitle = Component.literal("Row/Column/Panel/Divider composition examples"),
    ),
    INPUTS(
        title = Component.literal("Input Controls"),
        subtitle = Component.literal("Buttons, toggles, slider, radio, text field, and color picker"),
    ),
    SCROLLING(
        title = Component.literal("Scrolling + Collapsible"),
        subtitle = Component.literal("Scrollable list and collapsible sections"),
    ),
    LAYERS(
        title = Component.literal("Layers + Modal"),
        subtitle = Component.literal("Layer stack, animated modal transitions, and dialog primitives"),
    ),
    TABS(
        title = Component.literal("Tabs"),
        subtitle = Component.literal("Tab container and tabbed screen"),
    )
}

