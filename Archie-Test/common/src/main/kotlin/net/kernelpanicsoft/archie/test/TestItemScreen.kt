package net.kernelpanicsoft.archie.test

import net.kernelpanicsoft.archie.gui.ComposeScreen
import net.kernelpanicsoft.archie.gui.composables.basic.HorizontalDivider
import net.kernelpanicsoft.archie.gui.composables.basic.Text
import net.kernelpanicsoft.archie.gui.composables.containers.Panel
import net.kernelpanicsoft.archie.gui.composables.containers.Scrollable
import net.kernelpanicsoft.archie.gui.theme.LocalTheme
import net.kernelpanicsoft.archie.gui.theme.Theme
import net.kernelpanicsoft.archie.gui.layout.Arrangement
import net.kernelpanicsoft.archie.gui.layout.Column
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.modifiers.size
import net.minecraft.network.chat.Component

class TestItemScreen : ComposeScreen(Component.literal("Test Item Compose Screen")) {
    override fun init() {
        super.init()
        start {
            Theme {
                Panel(modifier = Modifier.size(220, 160)) {
                    Column(verticalArrangement = Arrangement.spacedBy(4)) {
                        Text(
                            text = Component.literal("Test Item Compose Screen"),
                            color = LocalTheme.current.darkTextColor,
                            dropShadow = false,
                        )
                        Text(
                            text = Component.literal("Scrollable rows (text rendered without shadow)"),
                            color = LocalTheme.current.darkTextColor,
                            dropShadow = false,
                            fontScale = 0.85f,
                        )
                        HorizontalDivider()

                        Scrollable(modifier = Modifier.size(208, 118)) {
                            Column(verticalArrangement = Arrangement.spacedBy(2)) {
                                repeat(40) { index ->
                                    Text(
                                        Component.literal("Item Screen Row ${index + 1}"),
                                        color = LocalTheme.current.darkTextColor,
                                        dropShadow = false,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

