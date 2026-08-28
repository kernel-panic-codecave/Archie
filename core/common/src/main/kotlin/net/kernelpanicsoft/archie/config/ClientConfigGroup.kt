package net.kernelpanicsoft.archie.config

import net.minecraft.client.gui.screens.Screen

/** Client-side mirror of a [ConfigGroup], built lazily as [ConfigGroup.client]. See [buildNodeListScreen]. */
class ClientConfigGroup(internal val group: ConfigGroup)
{
	fun buildGroupScreen(parent: Screen): Screen = buildNodeListScreen(group.title, group.children, parent)
}
