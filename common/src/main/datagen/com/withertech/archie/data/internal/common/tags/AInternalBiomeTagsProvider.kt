package com.withertech.archie.data.internal.common.tags

import com.withertech.archie.Archie
import com.withertech.archie.data.common.tags.ATagsProvider
import com.withertech.archie.data.common.tags.ACommonTags
import net.minecraft.core.HolderLookup
import net.minecraft.data.PackOutput
import net.minecraft.tags.BiomeTags
import net.minecraft.world.level.biome.Biomes
import java.util.concurrent.CompletableFuture

class AInternalBiomeTagsProvider(
	output: PackOutput, registriesFuture: CompletableFuture<HolderLookup.Provider>
) : ATagsProvider.BiomeTagsProvider(output, Archie.MOD, registriesFuture, false)
{
	override fun generate(registries: HolderLookup.Provider)
	{
		ACommonTags.Biomes.NO_DEFAULT_MONSTERS += listOf(
			Biomes.MUSHROOM_FIELDS, Biomes.DEEP_DARK
		)
		ACommonTags.Biomes.HIDDEN_FROM_LOCATOR_SELECTION() // Create tag file for visibility

		ACommonTags.Biomes.IS_VOID += Biomes.THE_VOID

		ACommonTags.Biomes.IS_END += BiomeTags.IS_END
		ACommonTags.Biomes.IS_NETHER += BiomeTags.IS_NETHER
		ACommonTags.Biomes.IS_OVERWORLD += BiomeTags.IS_OVERWORLD

		ACommonTags.Biomes.IS_HOT_OVERWORLD += listOf(
			Biomes.SWAMP,
			Biomes.MANGROVE_SWAMP,
			Biomes.JUNGLE,
			Biomes.BAMBOO_JUNGLE,
			Biomes.SPARSE_JUNGLE,
			Biomes.DESERT,
			Biomes.ERODED_BADLANDS,
			Biomes.SAVANNA,
			Biomes.SAVANNA_PLATEAU,
			Biomes.WINDSWEPT_SAVANNA,
			Biomes.STONY_PEAKS,
			Biomes.WARM_OCEAN
		)
		ACommonTags.Biomes.IS_HOT_NETHER += listOf(
			Biomes.NETHER_WASTES,
			Biomes.CRIMSON_FOREST,
			Biomes.WARPED_FOREST,
			Biomes.SOUL_SAND_VALLEY,
			Biomes.BASALT_DELTAS
		)
		ACommonTags.Biomes.IS_HOT_END()
		ACommonTags.Biomes.IS_HOT {
			addTags(
				ACommonTags.Biomes.IS_HOT_OVERWORLD,
				ACommonTags.Biomes.IS_HOT_NETHER
			)
			addOptionalTag(ACommonTags.Biomes.IS_HOT_END)
		}

		ACommonTags.Biomes.IS_COLD_OVERWORLD += listOf(
			Biomes.TAIGA,
			Biomes.OLD_GROWTH_PINE_TAIGA,
			Biomes.SNOWY_PLAINS,
			Biomes.ICE_SPIKES,
			Biomes.GROVE,
			Biomes.SNOWY_SLOPES,
			Biomes.JAGGED_PEAKS,
			Biomes.FROZEN_PEAKS,
			Biomes.SNOWY_BEACH,
			Biomes.SNOWY_TAIGA,
			Biomes.FROZEN_RIVER,
			Biomes.COLD_OCEAN,
			Biomes.FROZEN_OCEAN,
			Biomes.DEEP_COLD_OCEAN,
			Biomes.DEEP_FROZEN_OCEAN
		)
		ACommonTags.Biomes.IS_COLD_NETHER()
		ACommonTags.Biomes.IS_COLD_END += listOf(
			Biomes.THE_END,
			Biomes.SMALL_END_ISLANDS,
			Biomes.END_MIDLANDS,
			Biomes.END_HIGHLANDS,
			Biomes.END_BARRENS
		)
		ACommonTags.Biomes.IS_COLD {
			addTags(
				ACommonTags.Biomes.IS_COLD_OVERWORLD,
				ACommonTags.Biomes.IS_COLD_END
			)
			addOptionalTag(ACommonTags.Biomes.IS_COLD_NETHER.location())
		}

		ACommonTags.Biomes.IS_SPARSE_VEGETATION_OVERWORLD += listOf(
			Biomes.WOODED_BADLANDS,
			Biomes.ERODED_BADLANDS,
			Biomes.SAVANNA,
			Biomes.SAVANNA_PLATEAU,
			Biomes.WINDSWEPT_SAVANNA,
			Biomes.WINDSWEPT_FOREST,
			Biomes.WINDSWEPT_HILLS,
			Biomes.WINDSWEPT_GRAVELLY_HILLS,
			Biomes.SNOWY_SLOPES,
			Biomes.JAGGED_PEAKS,
			Biomes.FROZEN_PEAKS
		)
		ACommonTags.Biomes.IS_SPARSE_VEGETATION_NETHER()
		ACommonTags.Biomes.IS_SPARSE_VEGETATION_END()
		ACommonTags.Biomes.IS_SPARSE_VEGETATION {
			addTag(ACommonTags.Biomes.IS_SPARSE_VEGETATION_OVERWORLD)
			addOptionalTags(
				ACommonTags.Biomes.IS_SPARSE_VEGETATION_NETHER,
				ACommonTags.Biomes.IS_SPARSE_VEGETATION_END
			)
		}

		ACommonTags.Biomes.IS_DENSE_VEGETATION_OVERWORLD += listOf(
			Biomes.DARK_FOREST,
			Biomes.OLD_GROWTH_BIRCH_FOREST,
			Biomes.OLD_GROWTH_SPRUCE_TAIGA,
			Biomes.JUNGLE
		)
		ACommonTags.Biomes.IS_DENSE_VEGETATION_NETHER()
		ACommonTags.Biomes.IS_DENSE_VEGETATION_END()
		ACommonTags.Biomes.IS_DENSE_VEGETATION {
			addTag(ACommonTags.Biomes.IS_DENSE_VEGETATION_OVERWORLD)
			addOptionalTags(
				ACommonTags.Biomes.IS_DENSE_VEGETATION_NETHER,
				ACommonTags.Biomes.IS_DENSE_VEGETATION_END
			)
		}

		ACommonTags.Biomes.IS_WET_OVERWORLD += listOf(
			Biomes.SWAMP,
			Biomes.MANGROVE_SWAMP,
			Biomes.JUNGLE,
			Biomes.BAMBOO_JUNGLE,
			Biomes.SPARSE_JUNGLE,
			Biomes.BEACH,
			Biomes.LUSH_CAVES,
			Biomes.DRIPSTONE_CAVES
		)
		ACommonTags.Biomes.IS_WET_NETHER()
		ACommonTags.Biomes.IS_WET_END()
		ACommonTags.Biomes.IS_WET {
			addTag(ACommonTags.Biomes.IS_WET_OVERWORLD)
			addOptionalTags(
				ACommonTags.Biomes.IS_WET_NETHER,
				ACommonTags.Biomes.IS_WET_END
			)
		}

		ACommonTags.Biomes.IS_DRY_OVERWORLD += listOf(
			Biomes.DESERT,
			Biomes.BADLANDS,
			Biomes.WOODED_BADLANDS,
			Biomes.ERODED_BADLANDS,
			Biomes.SAVANNA,
			Biomes.SAVANNA_PLATEAU,
			Biomes.WINDSWEPT_SAVANNA
		)
		ACommonTags.Biomes.IS_DRY_NETHER += listOf(
			Biomes.NETHER_WASTES,
			Biomes.CRIMSON_FOREST,
			Biomes.WARPED_FOREST,
			Biomes.SOUL_SAND_VALLEY,
			Biomes.BASALT_DELTAS
		)
		ACommonTags.Biomes.IS_DRY_END += listOf(
			Biomes.THE_END,
			Biomes.SMALL_END_ISLANDS,
			Biomes.END_MIDLANDS,
			Biomes.END_HIGHLANDS,
			Biomes.END_BARRENS
		)
		ACommonTags.Biomes.IS_DRY += listOf(
			ACommonTags.Biomes.IS_DRY_OVERWORLD,
			ACommonTags.Biomes.IS_DRY_NETHER,
			ACommonTags.Biomes.IS_DRY_END
		)

		ACommonTags.Biomes.IS_CONIFEROUS_TREE += ACommonTags.Biomes.IS_TAIGA
		ACommonTags.Biomes.IS_CONIFEROUS_TREE +=	Biomes.GROVE

		ACommonTags.Biomes.IS_SAVANNA_TREE += ACommonTags.Biomes.IS_SAVANNA
		ACommonTags.Biomes.IS_JUNGLE_TREE += ACommonTags.Biomes.IS_JUNGLE
		ACommonTags.Biomes.IS_DECIDUOUS_TREE += listOf(
			Biomes.FOREST,
			Biomes.FLOWER_FOREST,
			Biomes.BIRCH_FOREST,
			Biomes.DARK_FOREST,
			Biomes.OLD_GROWTH_BIRCH_FOREST,
			Biomes.WINDSWEPT_FOREST
		)

		ACommonTags.Biomes.IS_MOUNTAIN_SLOPE += listOf(
			Biomes.SNOWY_SLOPES,
			Biomes.MEADOW,
			Biomes.GROVE,
			Biomes.CHERRY_GROVE
		)
		ACommonTags.Biomes.IS_MOUNTAIN_PEAK += listOf(
			Biomes.JAGGED_PEAKS,
			Biomes.FROZEN_PEAKS,
			Biomes.STONY_PEAKS
		)
		ACommonTags.Biomes.IS_MOUNTAIN += listOf(
			BiomeTags.IS_MOUNTAIN,
			ACommonTags.Biomes.IS_MOUNTAIN_PEAK,
			ACommonTags.Biomes.IS_MOUNTAIN_SLOPE
		)

		ACommonTags.Biomes.IS_FOREST += BiomeTags.IS_FOREST
		ACommonTags.Biomes.IS_BIRCH_FOREST += listOf(
			Biomes.BIRCH_FOREST,
			Biomes.OLD_GROWTH_BIRCH_FOREST
		)
		ACommonTags.Biomes.IS_FLOWER_FOREST += Biomes.FLOWER_FOREST
		ACommonTags.Biomes.IS_FLORAL += ACommonTags.Biomes.IS_FLOWER_FOREST
		ACommonTags.Biomes.IS_FLORAL += listOf(
			Biomes.SUNFLOWER_PLAINS,
			Biomes.CHERRY_GROVE,
			Biomes.MEADOW
		)
		ACommonTags.Biomes.IS_BEACH += BiomeTags.IS_BEACH
		ACommonTags.Biomes.IS_STONY_SHORES += Biomes.STONY_SHORE
		ACommonTags.Biomes.IS_DESERT += Biomes.DESERT
		ACommonTags.Biomes.IS_BADLANDS += BiomeTags.IS_BADLANDS
		ACommonTags.Biomes.IS_PLAINS += listOf(
			Biomes.PLAINS,
			Biomes.SUNFLOWER_PLAINS
		)
		ACommonTags.Biomes.IS_SNOWY_PLAINS += Biomes.SNOWY_PLAINS
		ACommonTags.Biomes.IS_TAIGA += BiomeTags.IS_TAIGA
		ACommonTags.Biomes.IS_HILL += BiomeTags.IS_HILL
		ACommonTags.Biomes.IS_WINDSWEPT += listOf(
			Biomes.WINDSWEPT_HILLS,
			Biomes.WINDSWEPT_GRAVELLY_HILLS,
			Biomes.WINDSWEPT_FOREST,
			Biomes.WINDSWEPT_SAVANNA
		)
		ACommonTags.Biomes.IS_SAVANNA += BiomeTags.IS_SAVANNA
		ACommonTags.Biomes.IS_JUNGLE += BiomeTags.IS_JUNGLE
		ACommonTags.Biomes.IS_SNOWY += listOf(
			Biomes.SNOWY_BEACH,
			Biomes.SNOWY_PLAINS,
			Biomes.ICE_SPIKES,
			Biomes.SNOWY_TAIGA,
			Biomes.GROVE,
			Biomes.SNOWY_SLOPES,
			Biomes.JAGGED_PEAKS,
			Biomes.FROZEN_PEAKS
		)
		ACommonTags.Biomes.IS_ICY += listOf(
			Biomes.ICE_SPIKES,
			Biomes.FROZEN_PEAKS
		)
		ACommonTags.Biomes.IS_SWAMP += listOf(
			Biomes.SWAMP,
			Biomes.MANGROVE_SWAMP
		)
		ACommonTags.Biomes.IS_OLD_GROWTH += listOf(
			Biomes.OLD_GROWTH_BIRCH_FOREST,
			Biomes.OLD_GROWTH_PINE_TAIGA,
			Biomes.OLD_GROWTH_SPRUCE_TAIGA
		)
		ACommonTags.Biomes.IS_LUSH += Biomes.LUSH_CAVES
		ACommonTags.Biomes.IS_SANDY += listOf(
			Biomes.DESERT,
			Biomes.BADLANDS,
			Biomes.WOODED_BADLANDS,
			Biomes.ERODED_BADLANDS,
			Biomes.BEACH
		)
		ACommonTags.Biomes.IS_MUSHROOM += Biomes.MUSHROOM_FIELDS
		ACommonTags.Biomes.IS_PLATEAU += listOf(
			Biomes.WOODED_BADLANDS,
			Biomes.SAVANNA_PLATEAU,
			Biomes.CHERRY_GROVE,
			Biomes.MEADOW
		)
		ACommonTags.Biomes.IS_SPOOKY += listOf(
			Biomes.DARK_FOREST,
			Biomes.DEEP_DARK
		)
		ACommonTags.Biomes.IS_WASTELAND()
		ACommonTags.Biomes.IS_RARE += listOf(
			Biomes.SUNFLOWER_PLAINS,
			Biomes.FLOWER_FOREST,
			Biomes.OLD_GROWTH_BIRCH_FOREST,
			Biomes.OLD_GROWTH_SPRUCE_TAIGA,
			Biomes.BAMBOO_JUNGLE,
			Biomes.SPARSE_JUNGLE,
			Biomes.ERODED_BADLANDS,
			Biomes.SAVANNA_PLATEAU,
			Biomes.WINDSWEPT_SAVANNA,
			Biomes.ICE_SPIKES,
			Biomes.WINDSWEPT_GRAVELLY_HILLS,
			Biomes.MUSHROOM_FIELDS,
			Biomes.DEEP_DARK
		)

		ACommonTags.Biomes.IS_RIVER += BiomeTags.IS_RIVER
		ACommonTags.Biomes.IS_SHALLOW_OCEAN += listOf(
			Biomes.OCEAN,
			Biomes.LUKEWARM_OCEAN,
			Biomes.WARM_OCEAN,
			Biomes.COLD_OCEAN,
			Biomes.FROZEN_OCEAN
		)
		ACommonTags.Biomes.IS_DEEP_OCEAN += BiomeTags.IS_DEEP_OCEAN
		ACommonTags.Biomes.IS_OCEAN += listOf(
			BiomeTags.IS_OCEAN,
			ACommonTags.Biomes.IS_SHALLOW_OCEAN,
			ACommonTags.Biomes.IS_DEEP_OCEAN
		)
		ACommonTags.Biomes.IS_AQUATIC_ICY += listOf(
			Biomes.FROZEN_RIVER,
			Biomes.DEEP_FROZEN_OCEAN,
			Biomes.FROZEN_OCEAN
		)
		ACommonTags.Biomes.IS_AQUATIC += listOf(
			ACommonTags.Biomes.IS_OCEAN,
			ACommonTags.Biomes.IS_RIVER
		)

		ACommonTags.Biomes.IS_CAVE += listOf(
			Biomes.LUSH_CAVES,
			Biomes.DRIPSTONE_CAVES,
			Biomes.DEEP_DARK
		)
		ACommonTags.Biomes.IS_UNDERGROUND += ACommonTags.Biomes.IS_CAVE

		ACommonTags.Biomes.IS_NETHER_FOREST += listOf(
			Biomes.CRIMSON_FOREST,
			Biomes.WARPED_FOREST
		)
		ACommonTags.Biomes.IS_OUTER_END_ISLAND += listOf(
			Biomes.END_HIGHLANDS,
			Biomes.END_MIDLANDS,
			Biomes.END_BARRENS
		)

	}

}