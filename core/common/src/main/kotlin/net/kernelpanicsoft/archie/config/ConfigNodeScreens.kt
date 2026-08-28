package net.kernelpanicsoft.archie.config

import dev.architectury.utils.GameInstance
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry
import me.shedaniel.clothconfig2.api.ConfigBuilder
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder
import net.kernelpanicsoft.archie.config.builder.startConfigSpecList
import net.kernelpanicsoft.archie.config.builder.startConfigSpecMap
import net.kernelpanicsoft.archie.config.builder.startScreenField
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import java.util.function.Consumer

/** Whether [this] should be shown, given whether the client is currently [isWorld] - a [ConfigSpec.Server] (and a synchronized [ConfigSpecCollection]) hides outside a world, and a [ConfigGroup] hides once every child of it would. */
internal fun ConfigNode.isVisible(isWorld: Boolean): Boolean = when (this)
{
	is ConfigSpec -> type != ConfigSpec.Type.SERVER || isWorld
	is ConfigGroup -> children.any { it.isVisible(isWorld) }
	is ConfigSpecCollection<*> -> !synchronized || isWorld
}

/** Builds whatever screen [this] opens when its "Edit" row is clicked. Never called for a [ConfigSpecCollection] - see [buildCollectionEntry]. */
internal fun ConfigNode.buildScreen(parent: Screen): Screen = when (this)
{
	is ConfigSpec -> client.buildConfig(parent)
	is ConfigGroup -> client.buildGroupScreen(parent)
	is ConfigSpecCollection<*> -> parent
}

/**
 * Builds a native add/remove list/map field for [collection] - see [ConfigSpecList]/[ConfigSpecMap].
 * Used both as a [DataSpec] field's own entry and, top-level, as one row of a container/group screen.
 * A [ConfigSpecCollection.synchronized] collection's membership is server-authoritative, so its
 * add/remove buttons are disabled everywhere but the actual host (see [GameInstance.getServer]).
 */
internal fun <T : ConfigSpec> ConfigEntryBuilder.buildCollectionEntry(collection: ConfigSpecCollection<T>): AbstractConfigListEntry<*>
{
	val editable = !collection.synchronized || GameInstance.getServer() != null
	return when (collection)
	{
		is ConfigSpecList<T> -> startConfigSpecList(collection.title, collection.entries) { collection.createScratchEntry() }
			.apply {
				setInsertButtonEnabled(editable)
				setDeleteButtonEnabled(editable)
				saveConsumer = Consumer { collection.reconcile(it) }
			}
			.build()

		is ConfigSpecMap<T> -> startConfigSpecMap(collection.title, collection.entries) { collection.createScratchEntry() }
			.apply {
				setInsertButtonEnabled(editable)
				setDeleteButtonEnabled(editable)
				saveConsumer = Consumer { rows -> collection.reconcile(rows.map { it.value }) }
			}
			.build()
	}
}

/**
 * Builds a screen listing one row per visible child of [children] - a [ConfigSpecCollection]
 * embeds directly as a list/map field, a [ConfigSpec]/[ConfigGroup] gets an "Edit" row drilling
 * into its own screen. With none visible, just returns [parent]; with exactly one visible
 * [ConfigSpec]/[ConfigGroup] and no collections, that child's own screen directly, skipping the
 * wrapper. Shared by [ClientConfigContainer] and [ClientConfigGroup].
 */
internal fun buildNodeListScreen(screenTitle: Component, children: List<ConfigNode>, parent: Screen): Screen
{
	val isWorld = Minecraft.getInstance().level != null
	val visible = children.filter { it.isVisible(isWorld) }
	if (visible.isEmpty()) return parent
	val onlyChild = visible.singleOrNull()
	if (onlyChild != null && onlyChild !is ConfigSpecCollection<*>) return onlyChild.buildScreen(parent)
	return ConfigBuilder.create().apply {
		title = screenTitle
		val category = getOrCreateCategory(screenTitle)
		val entryBuilder = entryBuilder()
		visible.forEach { node ->
			category.addEntry(
				when (node)
				{
					is ConfigSpecCollection<*> -> entryBuilder.buildCollectionEntry(node)
					else -> entryBuilder.startScreenField(node.title, node) { s -> node.buildScreen(s) }.build()
				}
			)
		}
		setFallbackCategory(category)
		parentScreen = parent
		setAfterInitConsumer { configScreen ->
			configScreen.removeWidget(configScreen.children().first { it is Button && it.message == Component.empty() })
		}
	}.build()
}
