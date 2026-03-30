package net.kernelpanicsoft.archie.test

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import net.kernelpanicsoft.archie.Archie
import net.kernelpanicsoft.archie.gui.ComposeContainerScreen
import net.kernelpanicsoft.archie.gui.Slots
import net.kernelpanicsoft.archie.gui.blockentity.observeProperty
import net.kernelpanicsoft.archie.gui.composables.basic.Text
import net.kernelpanicsoft.archie.gui.composables.containers.ContainerScreenLayout
import net.kernelpanicsoft.archie.gui.composables.input.Button
import net.kernelpanicsoft.archie.gui.composables.input.textfield.BasicTextField
import net.kernelpanicsoft.archie.gui.composables.modal.ConfirmDialog
import net.kernelpanicsoft.archie.gui.layer.LocalLayerManager
import net.kernelpanicsoft.archie.gui.layout.Column
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.modifiers.size
import net.kernelpanicsoft.archie.gui.modifiers.sizeIn
import net.kernelpanicsoft.archie.gui.theme.LocalTheme
import net.kernelpanicsoft.archie.gui.theme.Theme
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory

class TestScreen(menu: TestMenu, playerInventory: Inventory, title: Component) : ComposeContainerScreen<TestMenu, TestTile>(menu, playerInventory,
	title
)
{
	private val rows = menu.rows

	init
	{
//		ArchieTest.LOGGER.info("Screen: $title")
//		this.imageHeight = 114 + rows * 18
//		this.inventoryLabelY = this.imageHeight - 94
		start {
			content()
		}
	}

	@Composable
	fun content()
	{
		val layerManager = LocalLayerManager.current
		var test by observeProperty("test", "")
		Archie.LOGGER.info("Test: $test")
		Theme {
			ContainerScreenLayout {
				Column {
					Text(
						text = Component.literal(test.toString()),
						color = LocalTheme.current.darkTextColor,
						dropShadow = false
					)
					var text by remember { mutableStateOf(test.toString()) }
					Button(
						modifier = Modifier.size(18 * 9, 20),
						onClick = {
							layerManager.confirmDialog(
								onConfirm = {
									test = text
								}
							) {
								BasicTextField(
									value = text,
									modifier = Modifier.sizeIn(maxWidth = 200),
									onValueChange = { text = it }
								)
							}
						}) { Text(text = Component.literal("Set Test")) }
					Slots(
						"inventory",
						9,
						rows
					)
				}
			}
		}
	}

//	override fun renderBg(guiGraphics: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int)
//	{
//		val i = (this.width - this.imageWidth) / 2
//		val j = (this.height - this.imageHeight) / 2
//		guiGraphics.blit(
//			CONTAINER_BACKGROUND, i, j, 0, 0,
//			this.imageWidth,
//			rows * 18 + 17
//		)
//		guiGraphics.blit(
//			CONTAINER_BACKGROUND, i, j + rows * 18 + 17, 0, 126,
//			this.imageWidth, 96
//		)
//	}

//	companion object
//	{
//		private val CONTAINER_BACKGROUND = ResourceLocation.parse("textures/gui/container/generic_54.png")
//	}
}