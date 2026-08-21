package net.kernelpanicsoft.archie.util

import dev.architectury.platform.Platform
import net.kernelpanicsoft.archie.APlatform

/**
 * Cloth Config's own mod id, which differs by loader: Fabric allows hyphens (`"cloth-config"`),
 * while NeoForge's mod id charset doesn't, so its variant registers as `"cloth_config"` instead.
 */
val clothConfigModId: String
	get() = when (val platform = APlatform.platform)
	{
		"fabric" -> "cloth-config"
		"neoforge" -> "cloth_config"
		else -> throw UnsupportedOperationException("Unsupported platform: $platform")
	}

/**
 * Whether Cloth Config is actually present among the currently loaded mods - not merely whether
 * this is the physical client. [onClient] alone only rules out a dedicated server; it still lets
 * client-only code run on a client that simply doesn't have Cloth Config installed, which is
 * exactly the case any code touching Cloth Config's own types (`ModifierKeyCode` and friends)
 * needs to additionally guard against to avoid a hard dependency on it.
 */
val isClothConfigLoaded: Boolean get() = Platform.isModLoaded(clothConfigModId)
