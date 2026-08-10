package net.kernelpanicsoft.archie.data.common.conditions

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import net.minecraft.resources.ResourceLocation

/**
 * Cross-loader hooks that plug [IACondition] into each loader's native datapack condition
 * system, since Fabric and NeoForge each have their own recipe/tag condition machinery.
 *
 * Trimmed to the runtime-needed half (backs [IACondition.register]/[IACondition.CODEC], evaluated
 * whenever a datapack loads a condition) - `withCondition`/`fabricRecipeProvider` (used only by the
 * recipe-datagen DSL) live in `archie-datagen` instead.
 */
expect object AConditionsPlatform
{
	/** Registers condition type keyed by [identifier], decodable with [codec]; backs [IACondition.register]. */
	fun register(identifier: ResourceLocation, codec: MapCodec<out IACondition>)

	/** The dispatch codec decoding any registered [IACondition]; backs [IACondition.CODEC]. */
	fun codec(): Codec<IACondition>
}
