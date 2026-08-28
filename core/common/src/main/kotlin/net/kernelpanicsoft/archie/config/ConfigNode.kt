package net.kernelpanicsoft.archie.config

import net.minecraft.network.chat.Component

/**
 * Something a [ConfigContainer] or [ConfigGroup] can hold as a child and show as a row in its
 * Cloth Config screen: a [ConfigSpec] (an "Edit" row opening its own screen), a [ConfigGroup]
 * (a folder - an "Edit" row opening a subscreen listing its own children), or a
 * [ConfigSpecCollection] (an "Edit" row opening an add/remove list screen).
 */
sealed interface ConfigNode {
    /** Display title shown for this node's row in its parent's screen. */
    val title: Component

    /** Stable identifier - this node's path segment when nested under a [ConfigGroup]. */
    val id: String
}
