package net.kernelpanicsoft.archie.data.client.model

import com.google.gson.JsonObject

/** Something that can be serialized as a complete `blockstates` JSON document, e.g. [AVariantBlockStateBuilder]/[AMultiPartBlockStateBuilder]. */
interface IAGeneratedBlockState
{
	/** Serializes this blockstate to its JSON representation. */
	fun toJson(): JsonObject
}