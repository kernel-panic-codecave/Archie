package net.kernelpanicsoft.archie.test

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import net.kernelpanicsoft.archie.gui.ComposeContainerScreen
import net.kernelpanicsoft.archie.gui.Slots
import net.kernelpanicsoft.archie.gui.blockentity.observeProperty
import net.kernelpanicsoft.archie.gui.composables.basic.Text
import net.kernelpanicsoft.archie.gui.composables.containers.Collapsible
import net.kernelpanicsoft.archie.gui.composables.containers.Panel
import net.kernelpanicsoft.archie.gui.composables.containers.Scrollable
import net.kernelpanicsoft.archie.gui.composables.containers.Surface
import net.kernelpanicsoft.archie.gui.composables.containers.TabContainerPanel
import net.kernelpanicsoft.archie.gui.composables.containers.TabPanel
import net.kernelpanicsoft.archie.gui.composables.input.Checkbox
import net.kernelpanicsoft.archie.gui.composables.input.Button
import net.kernelpanicsoft.archie.gui.composables.input.ColorPicker
import net.kernelpanicsoft.archie.gui.composables.input.RadioGroup
import net.kernelpanicsoft.archie.gui.composables.input.RadioOption
import net.kernelpanicsoft.archie.gui.composables.input.Slider
import net.kernelpanicsoft.archie.gui.composables.input.Switch
import net.kernelpanicsoft.archie.gui.composables.input.textfield.BasicTextField
import net.kernelpanicsoft.archie.gui.composables.modal.ModalChoice
import net.kernelpanicsoft.archie.gui.layer.LocalLayerManager
import net.kernelpanicsoft.archie.gui.layout.Alignment
import net.kernelpanicsoft.archie.gui.layout.Arrangement
import net.kernelpanicsoft.archie.gui.layout.Box
import net.kernelpanicsoft.archie.gui.layout.Column
import net.kernelpanicsoft.archie.gui.layout.Row
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.modifiers.fillMaxWidth
import net.kernelpanicsoft.archie.gui.modifiers.height
import net.kernelpanicsoft.archie.gui.modifiers.size
import net.kernelpanicsoft.archie.gui.modifiers.sizeIn
import net.kernelpanicsoft.archie.gui.modifiers.width
import net.kernelpanicsoft.archie.gui.theme.LocalTheme
import net.kernelpanicsoft.archie.gui.theme.Theme
import net.kernelpanicsoft.archie.gui.util.HsvColor
import net.kernelpanicsoft.archie.gui.util.KColor
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory

class TestScreen(menu: TestMenu, playerInventory: Inventory, title: Component) : ComposeContainerScreen<TestMenu>(menu, playerInventory,
	title
)
{
	private val contentWidth = 18 * 9
	private val rows = menu.rows

	init
	{
		start {
			content()
		}
	}

	@Composable
	fun content()
	{
		val layerManager = LocalLayerManager.current
		var syncedValue by observeProperty("test", "")
		val test = syncedValue ?: ""
		Theme {
			Box(modifier = Modifier.width(contentWidth + 16)) {
				TabContainerPanel(contentWidth) {
					for (showcase in TestKind.entries) {
						tab(showcase.name, showcase.title) {
							Column(verticalArrangement = Arrangement.spacedBy(6)) {
								Text(
									text = showcase.title,
									color = LocalTheme.current.darkTextColor,
									dropShadow = false,
								)
								Text(
									text = showcase.subtitle,
									color = LocalTheme.current.darkTextColor,
									dropShadow = false,
									fontScale = 0.85f,
								)

								when (showcase) {
									TestKind.LAYOUTS -> LayoutsShowcase(test)
									TestKind.INPUTS -> InputsShowcase(test) { syncedValue = it }
									TestKind.SCROLLING -> ScrollingShowcase()
									TestKind.LAYERS -> LayerShowcase(test) { updated ->
										syncedValue = updated
									}
									TestKind.TABS -> TabShowcase()
								}

								Button(onClick = {
									layerManager.confirmDialog(
										title = Component.literal("Update Synced Value"),
										onConfirm = { syncedValue = "Saved @ ${System.currentTimeMillis() % 100000}" },
									) {
										Text(Component.literal("This writes to the synced block-entity property."), dropShadow = false)
									}
								}, modifier = Modifier.width(contentWidth)) {
									Text(Component.literal("Sync Value (ConfirmDialog)"), dropShadow = false)
								}

								Scrollable(modifier = Modifier.width(contentWidth).sizeIn(maxHeight = 3 * 18)) {
									Slots(
										"inventory_" + showcase.name.lowercase(),
										9,
										rows
									)
								}
							}
						}
					}
				}
			}
		}
	}

	@Composable
	private fun LayoutsShowcase(test: String) {
		Panel(modifier = Modifier.width(contentWidth)) {
			Column(verticalArrangement = Arrangement.spacedBy(4)) {
				Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
					Text(Component.literal("Left aligned"), dropShadow = false)
					Text(Component.literal("Right aligned"), dropShadow = false)
				}
				Collapsible(title = Component.literal("Current Synced Value"), initiallyExpanded = true) {
					Text(Component.literal(test.ifEmpty { "<empty>" }), dropShadow = false)
				}
			}
		}
	}

	@Composable
	private fun InputsShowcase(test: String, onTestChanged: (String) -> Unit) {
		var checked by remember { mutableStateOf(false) }
		var input by remember(test) { mutableStateOf(test) }
		var color by remember { mutableStateOf(HsvColor.from(KColor.CYAN)) }
		var switched by remember { mutableStateOf(true) }
		var slider by remember { mutableStateOf(0.35f) }
		var radio by remember { mutableStateOf("alpha") }

		Panel(variant = "inset", modifier = Modifier.width(contentWidth)) {
			Column(verticalArrangement = Arrangement.spacedBy(4)) {
				Row(horizontalArrangement = Arrangement.spacedBy(6), verticalAlignment = Alignment.CenterVertically) {
					Checkbox(checked = checked, onCheckedChange = { checked = it })
					Text(Component.literal(if (checked) "Checkbox: ON" else "Checkbox: OFF"), dropShadow = false)
				}
				Row(horizontalArrangement = Arrangement.spacedBy(6), verticalAlignment = Alignment.CenterVertically) {
					Switch(checked = switched, onCheckedChange = { switched = it })
					Text(Component.literal(if (switched) "Switch: ON" else "Switch: OFF"), dropShadow = false)
				}
				Slider(
					value = slider,
					onValueChange = { slider = it },
					steps = 10,
					modifier = Modifier.width(140),
				)
				Text(Component.literal("Slider: ${(slider * 100f).toInt()}%"), dropShadow = false)
				RadioGroup(
					options = listOf(
						RadioOption("alpha", Component.literal("Alpha")),
						RadioOption("beta", Component.literal("Beta")),
						RadioOption("gamma", Component.literal("Gamma")),
					),
					selected = radio,
					onSelected = { radio = it },
				)
				Text(Component.literal("Radio: $radio"), dropShadow = false)
				BasicTextField(
					value = input,
					onValueChange = { input = it },
					modifier = Modifier.sizeIn(minWidth = 120, maxWidth = 180),
				)
				Button(onClick = { onTestChanged(input) }) { Text(Component.literal("Apply Input to Synced Value"), dropShadow = false) }
				ColorPicker(
					color = color,
					onColorChanged = { color = it },
					modifier = Modifier.sizeIn(minWidth = 150, minHeight = 90, maxWidth = 170, maxHeight = 100),
				)
			}
		}
	}

	@Composable
	private fun ScrollingShowcase() {
		Panel(variant = "inset", modifier = Modifier.width(contentWidth)) {
			Scrollable(modifier = Modifier.height(96).width(contentWidth - 8)) {
				Column(verticalArrangement = Arrangement.spacedBy(2)) {
					repeat(40) { index ->
						Collapsible(
							title = Component.literal("Section ${index + 1}"),
							initiallyExpanded = index == 0,
						) {
							Text(Component.literal("Detail row ${index + 1}"), dropShadow = false)
						}
					}
				}
			}
		}
	}

	@Composable
	private fun LayerShowcase(test: String, onTestChanged: (String) -> Unit) {
		val layers = LocalLayerManager.current
		var pickedChoice by remember { mutableStateOf("none") }
		Panel(variant = "inset", modifier = Modifier.width(contentWidth)) {
			Column(verticalArrangement = Arrangement.spacedBy(4)) {
				Text(Component.literal("Synced value: ${test.ifEmpty { "<empty>" }}"), dropShadow = false)
				Text(Component.literal("Choice dialog result: $pickedChoice"), dropShadow = false)
				Button(onClick = {
					layers.confirmDialog(
						title = Component.literal("Compose Layer Demo"),
						onConfirm = { onTestChanged("Confirmed @ ${System.currentTimeMillis() % 100000}") },
						onCancel = { onTestChanged("Cancelled") },
					) {
						Text(Component.literal("This dialog is rendered in a modal layer."), dropShadow = false)
					}
				}) {
					Text(Component.literal("Open ConfirmDialog"), dropShadow = false)
				}
				Button(onClick = {
					layers.alertDialog(
						title = Component.literal("Heads Up"),
						message = Component.literal("This is the new AlertDialog primitive."),
					)
				}) {
					Text(Component.literal("Open AlertDialog"), dropShadow = false)
				}
				Button(onClick = {
					layers.promptDialog(
						title = Component.literal("Rename Synced Value"),
						initialValue = test,
						validator = { it.isNotBlank() },
						onConfirm = { onTestChanged(it) },
					)
				}) {
					Text(Component.literal("Open PromptDialog"), dropShadow = false)
				}
				Button(onClick = {
					layers.choiceDialog(
						title = Component.literal("Pick a Mode"),
						message = Component.literal("Choose an option below."),
						choices = listOf(
							ModalChoice("easy", Component.literal("Easy")),
							ModalChoice("hard", Component.literal("Hard")),
							ModalChoice("locked", Component.literal("Locked"), enabled = false),
						),
						onSelected = { pickedChoice = it },
					)
				}) {
					Text(Component.literal("Open ChoiceDialog"), dropShadow = false)
				}
			}
		}
	}

	@Composable
	fun TabShowcase()
	{
		Panel(variant = "inset", modifier = Modifier.width(contentWidth)) {
			TabPanel {
				tab("test", Component.literal("Test")) {
					Text(Component.literal("This is a tab!"), dropShadow = false)
				}
				tab("test2", Component.literal("Test 2")) {
					Text(Component.literal("This is another tab!"), dropShadow = false)
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