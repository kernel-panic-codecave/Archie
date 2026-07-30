package net.kernelpanicsoft.archie.data.client.model

import net.minecraft.resources.ResourceLocation

/** [AModelBuilder] for a block model at [outputLocation], produced by [ABlockModelProvider]. */
class ABlockModelBuilder(
	outputLocation: ResourceLocation
) : AModelBuilder<ABlockModelBuilder>(outputLocation)