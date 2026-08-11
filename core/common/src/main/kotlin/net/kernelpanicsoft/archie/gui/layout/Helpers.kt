package net.kernelpanicsoft.archie.gui.layout

/**
 * A density-independent pixel unit used throughout the layout system. Currently an alias for
 * [Int] since GUI measurements map 1:1 to Minecraft GUI pixels (no separate density scaling).
 */
typealias Dp = Int

/**
 * Converts this [Int] to a [Dp] value. Provided so measurements read naturally at call sites,
 * e.g. `16.dp`, mirroring Compose's `Dp` API even though no unit conversion currently happens.
 */
inline val Int.dp: Int get() = this