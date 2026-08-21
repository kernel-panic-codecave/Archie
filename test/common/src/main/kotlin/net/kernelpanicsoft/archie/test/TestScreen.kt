package net.kernelpanicsoft.archie.test

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.kernelpanicsoft.archie.gui.ComposeContainerScreen
import net.kernelpanicsoft.archie.gui.Slots
import net.kernelpanicsoft.archie.gui.blockentity.observeProperty
import net.kernelpanicsoft.archie.gui.nodes.UINode
import net.kernelpanicsoft.archie.gui.composables.basic.Text
import net.kernelpanicsoft.archie.gui.composables.containers.Collapsible
import net.kernelpanicsoft.archie.gui.composables.containers.ConnectorAnimation
import net.kernelpanicsoft.archie.gui.composables.containers.ConnectorDirection
import net.kernelpanicsoft.archie.gui.composables.containers.ConnectorShape
import net.kernelpanicsoft.archie.gui.composables.containers.ConnectorStyle
import net.kernelpanicsoft.archie.gui.composables.containers.NodeAnimation
import net.kernelpanicsoft.archie.gui.composables.containers.NodeBuilder
import net.kernelpanicsoft.archie.gui.composables.containers.NodeFrame
import net.kernelpanicsoft.archie.gui.composables.containers.NodeTreeView
import net.kernelpanicsoft.archie.gui.composables.containers.Panel
import net.kernelpanicsoft.archie.gui.composables.containers.Scrollable
import net.kernelpanicsoft.archie.gui.composables.containers.Surface
import net.kernelpanicsoft.archie.gui.composables.containers.TabContainerPanel
import net.kernelpanicsoft.archie.gui.composables.containers.TabPanel
import net.kernelpanicsoft.archie.gui.composables.containers.TreeNode
import net.kernelpanicsoft.archie.gui.composables.containers.TreeScope
import net.kernelpanicsoft.archie.gui.composables.containers.rememberTree
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
import net.kernelpanicsoft.archie.util.buildComponent
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
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
									TestKind.TREE -> TreeShowcase()
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

	/**
	 * A small toy skill tree, one node per [SkillNode], deliberately shaped to demonstrate every
	 * [NodeTreeView]/[NodeFrame]/[tree] feature at once rather than a single straight line of nodes:
	 * - `combat` and `gathering` are two independent roots (a forest, not one tree) - two top-level
	 *   [TreeScope.node] calls in [tree]'s own builder.
	 * - `smithing` has *two* prerequisites - `mining` (its nesting parent, an ordinary tree edge) and
	 *   `gathering` (an extra [NodeBuilder.dependsOn] naming an *already*-declared id) - exercising
	 *   `dependsOn`'s immediate-resolution path, and the topological depth pass placing `smithing`
	 *   correctly past *both* of its own prerequisites (one column past `mining`, which is itself one
	 *   past `gathering`) rather than tied with `mining`.
	 * - `rare_ore` is a [SkillNode.locked] child of `mining`, and `secret` a [SkillNode.locked] extra
	 *   [NodeBuilder.dependsOn] prerequisite of `boss` - named *before* `secret` itself is declared,
	 *   exercising `dependsOn`'s other, deferred-resolution path (see [NodeBuilder.dependsOn]'s own
	 *   `id` overload). Both are laid out exactly as if they passed [NodeTreeView]'s own `visible`
	 *   (their own real position, row and column, is what a connector reaching either of them
	 *   actually routes to), but [NodeTreeView]'s `visible` rejects both, so neither's own content
	 *   ever draws and every connector touching either one draws as a dim, wavy
	 *   [ConnectorStyle.DISCONNECTED] stub instead - reaching each one's own *real* position rather
	 *   than a vague generic direction. `boss` stays permanently [NodeAnimation.DISABLED] as a
	 *   result, since a hidden prerequisite can never be confirmed obtained.
	 * - Every other node's own [NodeAnimation] reflects whether it's already obtained (no overlay),
	 *   currently obtainable - every *visible* prerequisite already obtained, no hidden ones -
	 *   ([NodeAnimation.BLINKING], a "come get this" pulse), or blocked ([NodeAnimation.DISABLED]).
	 *   Connector animation mirrors the same states one level down ([ConnectorAnimation.NONE]/
	 *   `FLOWING`), and [NodeFrame]'s own `onClick` toggles a node's obtained state directly, so
	 *   obtaining one prerequisite is immediately visible rippling through whatever it unlocks next.
	 * - A non-`task` node's own connector - `smithing`/`boss` - uses [ConnectorShape.SPLINE] instead
	 *   of the default [ConnectorShape.ELBOW] every plain `task` node keeps, so both render side by
	 *   side in the same tree.
	 */
	/**
	 * [TreeShowcase]'s own per-node payload. [label]/[kind]/[locked] are genuine Compose state
	 * ([mutableStateOf]), not plain immutable fields: flipping `locked`, retitling `label`, or
	 * promoting a node's own `kind` is an ordinary, directly-observable mutation on the *same*
	 * [SkillNode] instance a [TreeNode.data] already holds - no need to construct a whole new
	 * [SkillNode] and reassign [TreeNode.data] just to change one field, the same reasoning
	 * [TreeNode]/[TreeRegistry]'s own KDoc gives for their own statefulness. [id] alone stays a
	 * plain, immutable `val` - it's this node's own stable identity ([TreeRegistry.get]'s own
	 * lookup key, [TreeNode.id] on the owning wrapper), not a value anything keyed on it would want
	 * to see drift out from under it.
	 */
	private class SkillNode(val id: String, label: Component, kind: String = "task", obtained: Boolean = false)
	{
		var label: Component by mutableStateOf(label)
		var kind: String by mutableStateOf(kind)
		var obtained: Boolean by mutableStateOf(obtained)
	}

	/**
	 * [SkillNode]'s own [NodeBuilder] - a fresh instance per [TreeScope.node] call builds up exactly
	 * one node's own fields via ordinary property-setter syntax, [build] finalizing them into the
	 * actual [SkillNode]. [id] itself comes from [NodeBuilder]'s own inherited property - the same
	 * id [TreeScope.node]'s own `id` argument already used - rather than needing to be set again here.
	 */
	private class SkillNodeBuilder(id: String) : NodeBuilder<String, SkillNode, SkillNodeBuilder>(id) {
		var label: Component = Component.literal("")
		var kind: String = "task"
		var obtained: Boolean = false
		override fun build() = SkillNode(id, label, kind, obtained)
	}

	@Composable
	fun TreeShowcase()
	{
		val skillTree = rememberTree(::SkillNodeBuilder) {
			node("gathering") {
				label = Component.literal("Gathering")
				children {
					node("mining") {
						label = Component.literal("Mining")
						children {
							node("smithing") {
								label = Component.literal("Smithing")
								kind = "goal"
								dependsOn("gathering")
							}
							node("rare_ore") {
								label = Component.literal("Rare Ore")
							}
						}
					}
				}
			}
			node("combat") {
				label = Component.literal("Combat")
				children {
					node("boss") {
						label = Component.literal("Boss Fight")
						kind = "challenge"
						dependsOn("secret") // forward reference - "secret" isn't declared until below
					}
				}
			}
			node("secret") {
				label = Component.literal("Secret Path")
			}
		}

		fun animationFor(node: TreeNode<String, SkillNode>): NodeAnimation
		{
			if (node.data.obtained) return NodeAnimation.NONE
			val obtainable = node.parents.all { it.data.obtained }
			return if (obtainable) NodeAnimation.BLINKING else NodeAnimation.DISABLED
		}

		fun isVisible(node: TreeNode<String, SkillNode>): Boolean =
			node.parents.isEmpty() || node.parents.any { it.data.obtained }

		NodeTreeView(
			roots = skillTree,
			visible = { node -> isVisible(node) },
			connectorDirection = { ConnectorDirection.PARENT_TO_CHILD },
			connectorStyle = { ConnectorStyle.ARROW },
			connectorShape = { ConnectorShape.SPLINE },
			connectorColor = { node -> if (node.data.obtained) 0xFF55CC55.toInt() else 0xFF808080.toInt() },
			connectorAnimation = { node ->
				when
				{
					node.data.obtained -> ConnectorAnimation.NONE
					animationFor(node) == NodeAnimation.DISABLED -> ConnectorAnimation.NONE
					else -> ConnectorAnimation.FLOWING
				}
			},
			nodeAnimation = { node -> animationFor(node) },
			backgroundTexture = ResourceLocation.withDefaultNamespace("textures/block/dirt.png"),
			backgroundTextureSize = 16,
			backgroundTint = 0xFFAAAAAA.toInt(),
			backgroundParallax = 0.5f,
			modifier = Modifier.size(contentWidth, 130),
		) { node ->
			NodeFrame(
				variant = node.data.kind,
				obtained = node.data.obtained,
				onClick = { _: UINode -> node.data.obtained = !node.data.obtained }.takeIf { isVisible(node) },
			) {
				Text(node.data.label, dropShadow = false, fontScale = 0.6f)
			}
		}
	}
}