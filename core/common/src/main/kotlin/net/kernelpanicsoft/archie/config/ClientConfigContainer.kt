package net.kernelpanicsoft.archie.config

import net.minecraft.client.gui.screens.Screen

/**
 * Client-side mirror of a [ConfigContainer]. If exactly one of [ConfigContainer.children] is
 * visible, [buildConfigContainer] opens that child's own screen directly; with more than one, it
 * builds a screen listing an "Edit" entry per child that drills into it. See [buildNodeListScreen].
 */
class ClientConfigContainer(internal var container: ConfigContainer)
{
	fun buildConfigContainer(parent: Screen): Screen = buildNodeListScreen(container.title, container.children, parent)

	/** Registers this spec's config screen with the platform's mod-list UI, client-side only. */
	fun initClient() = container.mod.registerConfigurationScreen(::buildConfigContainer)
}
