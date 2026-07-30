package net.kernelpanicsoft.archie

/** Cross-loader platform identification, backed by an `actual` per mod loader. */
expect object APlatform
{
	/** The current mod loader's short id: `"fabric"` on Fabric, `"neoforge"` on NeoForge. */
	val platform: String
}