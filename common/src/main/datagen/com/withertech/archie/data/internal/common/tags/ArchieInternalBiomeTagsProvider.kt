package com.withertech.archie.data.internal.common.tags

import com.withertech.archie.Archie
import com.withertech.archie.data.common.tags.ArchieTagsProvider
import com.withertech.archie.data.common.tags.CommonTags
import net.minecraft.core.HolderLookup
import net.minecraft.data.PackOutput
import net.minecraft.tags.BiomeTags
import net.minecraft.world.level.biome.Biomes
import java.util.concurrent.CompletableFuture

class ArchieInternalBiomeTagsProvider(
	output: PackOutput, registriesFuture: CompletableFuture<HolderLookup.Provider>
) : ArchieTagsProvider.BiomeTagsProvider(output, Archie.MOD, registriesFuture, false)
{
	override fun generate(provider: HolderLookup.Provider)
	{
		CommonTags.Biomes.NO_DEFAULT_MONSTERS += listOf(
			Biomes.MUSHROOM_FIELDS, Biomes.DEEP_DARK
		)
		CommonTags.Biomes.HIDDEN_FROM_LOCATOR_SELECTION() // Create tag file for visibility

		CommonTags.Biomes.IS_VOID += Biomes.THE_VOID

		CommonTags.Biomes.IS_END += BiomeTags.IS_END
		CommonTags.Biomes.IS_NETHER += BiomeTags.IS_NETHER
		CommonTags.Biomes.IS_OVERWORLD += BiomeTags.IS_OVERWORLD

		CommonTags.Biomes.IS_HOT_OVERWORLD += listOf(
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
		CommonTags.Biomes.IS_HOT_NETHER += listOf(
			Biomes.NETHER_WASTES,
			Biomes.CRIMSON_FOREST,
			Biomes.WARPED_FOREST,
			Biomes.SOUL_SAND_VALLEY,
			Biomes.BASALT_DELTAS
		)
		CommonTags.Biomes.IS_HOT_END()
		CommonTags.Biomes.IS_HOT {
			addTags(
				CommonTags.Biomes.IS_HOT_OVERWORLD,
				CommonTags.Biomes.IS_HOT_NETHER
			)
			addOptionalTag(CommonTags.Biomes.IS_HOT_END)
		}

		CommonTags.Biomes.IS_COLD_OVERWORLD += listOf(
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
		CommonTags.Biomes.IS_COLD_NETHER()
		CommonTags.Biomes.IS_COLD_END += listOf(
			Biomes.THE_END,
			Biomes.SMALL_END_ISLANDS,
			Biomes.END_MIDLANDS,
			Biomes.END_HIGHLANDS,
			Biomes.END_BARRENS
		)
		CommonTags.Biomes.IS_COLD {
			addTags(
				CommonTags.Biomes.IS_COLD_OVERWORLD,
				CommonTags.Biomes.IS_COLD_END
			)
			addOptionalTag(CommonTags.Biomes.IS_COLD_NETHER.location())
		}

		CommonTags.Biomes.IS_SPARSE_VEGETATION_OVERWORLD += listOf(
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
		CommonTags.Biomes.IS_SPARSE_VEGETATION_NETHER()
		CommonTags.Biomes.IS_SPARSE_VEGETATION_END()
		CommonTags.Biomes.IS_SPARSE_VEGETATION {
			addTag(CommonTags.Biomes.IS_SPARSE_VEGETATION_OVERWORLD)
			addOptionalTags(
				CommonTags.Biomes.IS_SPARSE_VEGETATION_NETHER,
				CommonTags.Biomes.IS_SPARSE_VEGETATION_END
			)
		}

		CommonTags.Biomes.IS_DENSE_VEGETATION_OVERWORLD += listOf(
			Biomes.DARK_FOREST,
			Biomes.OLD_GROWTH_BIRCH_FOREST,
			Biomes.OLD_GROWTH_SPRUCE_TAIGA,
			Biomes.JUNGLE
		)
		CommonTags.Biomes.IS_DENSE_VEGETATION_NETHER()
		CommonTags.Biomes.IS_DENSE_VEGETATION_END()
		CommonTags.Biomes.IS_DENSE_VEGETATION {
			addTag(CommonTags.Biomes.IS_DENSE_VEGETATION_OVERWORLD)
			addOptionalTags(
				CommonTags.Biomes.IS_DENSE_VEGETATION_NETHER,
				CommonTags.Biomes.IS_DENSE_VEGETATION_END
			)
		}

		CommonTags.Biomes.IS_WET_OVERWORLD += listOf(
			Biomes.SWAMP,
			Biomes.MANGROVE_SWAMP,
			Biomes.JUNGLE,
			Biomes.BAMBOO_JUNGLE,
			Biomes.SPARSE_JUNGLE,
			Biomes.BEACH,
			Biomes.LUSH_CAVES,
			Biomes.DRIPSTONE_CAVES
		)
		CommonTags.Biomes.IS_WET_NETHER()
		CommonTags.Biomes.IS_WET_END()
		CommonTags.Biomes.IS_WET {
			addTag(CommonTags.Biomes.IS_WET_OVERWORLD)
			addOptionalTags(
				CommonTags.Biomes.IS_WET_NETHER,
				CommonTags.Biomes.IS_WET_END
			)
		}

		CommonTags.Biomes.IS_DRY_OVERWORLD += listOf(
			Biomes.DESERT,
			Biomes.BADLANDS,
			Biomes.WOODED_BADLANDS,
			Biomes.ERODED_BADLANDS,
			Biomes.SAVANNA,
			Biomes.SAVANNA_PLATEAU,
			Biomes.WINDSWEPT_SAVANNA
		)
		CommonTags.Biomes.IS_DRY_NETHER += listOf(
			Biomes.NETHER_WASTES,
			Biomes.CRIMSON_FOREST,
			Biomes.WARPED_FOREST,
			Biomes.SOUL_SAND_VALLEY,
			Biomes.BASALT_DELTAS
		)
		CommonTags.Biomes.IS_DRY_END += listOf(
			Biomes.THE_END,
			Biomes.SMALL_END_ISLANDS,
			Biomes.END_MIDLANDS,
			Biomes.END_HIGHLANDS,
			Biomes.END_BARRENS
		)
		CommonTags.Biomes.IS_DRY += listOf(
			CommonTags.Biomes.IS_DRY_OVERWORLD,
			CommonTags.Biomes.IS_DRY_NETHER,
			CommonTags.Biomes.IS_DRY_END
		)

		CommonTags.Biomes.IS_CONIFEROUS_TREE += CommonTags.Biomes.IS_TAIGA
		CommonTags.Biomes.IS_CONIFEROUS_TREE +=	Biomes.GROVE

		CommonTags.Biomes.IS_SAVANNA_TREE += CommonTags.Biomes.IS_SAVANNA
		CommonTags.Biomes.IS_JUNGLE_TREE += CommonTags.Biomes.IS_JUNGLE
		CommonTags.Biomes.IS_DECIDUOUS_TREE += listOf(
			Biomes.FOREST,
			Biomes.FLOWER_FOREST,
			Biomes.BIRCH_FOREST,
			Biomes.DARK_FOREST,
			Biomes.OLD_GROWTH_BIRCH_FOREST,
			Biomes.WINDSWEPT_FOREST
		)

		CommonTags.Biomes.IS_MOUNTAIN_SLOPE += listOf(
			Biomes.SNOWY_SLOPES,
			Biomes.MEADOW,
			Biomes.GROVE,
			Biomes.CHERRY_GROVE
		)
		CommonTags.Biomes.IS_MOUNTAIN_PEAK += listOf(
			Biomes.JAGGED_PEAKS,
			Biomes.FROZEN_PEAKS,
			Biomes.STONY_PEAKS
		)
		CommonTags.Biomes.IS_MOUNTAIN += listOf(
			BiomeTags.IS_MOUNTAIN,
			CommonTags.Biomes.IS_MOUNTAIN_PEAK,
			CommonTags.Biomes.IS_MOUNTAIN_SLOPE
		)

		CommonTags.Biomes.IS_FOREST += BiomeTags.IS_FOREST
		CommonTags.Biomes.IS_BIRCH_FOREST += listOf(
			Biomes.BIRCH_FOREST,
			Biomes.OLD_GROWTH_BIRCH_FOREST
		)
		CommonTags.Biomes.IS_FLOWER_FOREST += Biomes.FLOWER_FOREST
		CommonTags.Biomes.IS_FLORAL += CommonTags.Biomes.IS_FLOWER_FOREST
		CommonTags.Biomes.IS_FLORAL += listOf(
			Biomes.SUNFLOWER_PLAINS,
			Biomes.CHERRY_GROVE,
			Biomes.MEADOW
		)
		CommonTags.Biomes.IS_BEACH += BiomeTags.IS_BEACH
		CommonTags.Biomes.IS_STONY_SHORES += Biomes.STONY_SHORE
		CommonTags.Biomes.IS_DESERT += Biomes.DESERT
		CommonTags.Biomes.IS_BADLANDS += BiomeTags.IS_BADLANDS
		CommonTags.Biomes.IS_PLAINS += listOf(
			Biomes.PLAINS,
			Biomes.SUNFLOWER_PLAINS
		)
		CommonTags.Biomes.IS_SNOWY_PLAINS += Biomes.SNOWY_PLAINS
		CommonTags.Biomes.IS_TAIGA += BiomeTags.IS_TAIGA
		CommonTags.Biomes.IS_HILL += BiomeTags.IS_HILL
		CommonTags.Biomes.IS_WINDSWEPT += listOf(
			Biomes.WINDSWEPT_HILLS,
			Biomes.WINDSWEPT_GRAVELLY_HILLS,
			Biomes.WINDSWEPT_FOREST,
			Biomes.WINDSWEPT_SAVANNA
		)
		CommonTags.Biomes.IS_SAVANNA += BiomeTags.IS_SAVANNA
		CommonTags.Biomes.IS_JUNGLE += BiomeTags.IS_JUNGLE
		CommonTags.Biomes.IS_SNOWY += listOf(
			Biomes.SNOWY_BEACH,
			Biomes.SNOWY_PLAINS,
			Biomes.ICE_SPIKES,
			Biomes.SNOWY_TAIGA,
			Biomes.GROVE,
			Biomes.SNOWY_SLOPES,
			Biomes.JAGGED_PEAKS,
			Biomes.FROZEN_PEAKS
		)
		CommonTags.Biomes.IS_ICY += listOf(
			Biomes.ICE_SPIKES,
			Biomes.FROZEN_PEAKS
		)
		CommonTags.Biomes.IS_SWAMP += listOf(
			Biomes.SWAMP,
			Biomes.MANGROVE_SWAMP
		)
		CommonTags.Biomes.IS_OLD_GROWTH += listOf(
			Biomes.OLD_GROWTH_BIRCH_FOREST,
			Biomes.OLD_GROWTH_PINE_TAIGA,
			Biomes.OLD_GROWTH_SPRUCE_TAIGA
		)
		CommonTags.Biomes.IS_LUSH += Biomes.LUSH_CAVES
		CommonTags.Biomes.IS_SANDY += listOf(
			Biomes.DESERT,
			Biomes.BADLANDS,
			Biomes.WOODED_BADLANDS,
			Biomes.ERODED_BADLANDS,
			Biomes.BEACH
		)
		CommonTags.Biomes.IS_MUSHROOM += Biomes.MUSHROOM_FIELDS
		CommonTags.Biomes.IS_PLATEAU += listOf(
			Biomes.WOODED_BADLANDS,
			Biomes.SAVANNA_PLATEAU,
			Biomes.CHERRY_GROVE,
			Biomes.MEADOW
		)
		CommonTags.Biomes.IS_SPOOKY += listOf(
			Biomes.DARK_FOREST,
			Biomes.DEEP_DARK
		)
		CommonTags.Biomes.IS_WASTELAND()
		CommonTags.Biomes.IS_RARE += listOf(
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

		CommonTags.Biomes.IS_RIVER += BiomeTags.IS_RIVER
		CommonTags.Biomes.IS_SHALLOW_OCEAN += listOf(
			Biomes.OCEAN,
			Biomes.LUKEWARM_OCEAN,
			Biomes.WARM_OCEAN,
			Biomes.COLD_OCEAN,
			Biomes.FROZEN_OCEAN
		)
		CommonTags.Biomes.IS_DEEP_OCEAN += BiomeTags.IS_DEEP_OCEAN
		CommonTags.Biomes.IS_OCEAN += listOf(
			BiomeTags.IS_OCEAN,
			CommonTags.Biomes.IS_SHALLOW_OCEAN,
			CommonTags.Biomes.IS_DEEP_OCEAN
		)
		CommonTags.Biomes.IS_AQUATIC_ICY += listOf(
			Biomes.FROZEN_RIVER,
			Biomes.DEEP_FROZEN_OCEAN,
			Biomes.FROZEN_OCEAN
		)
		CommonTags.Biomes.IS_AQUATIC += listOf(
			CommonTags.Biomes.IS_OCEAN,
			CommonTags.Biomes.IS_RIVER
		)

		CommonTags.Biomes.IS_CAVE += listOf(
			Biomes.LUSH_CAVES,
			Biomes.DRIPSTONE_CAVES,
			Biomes.DEEP_DARK
		)
		CommonTags.Biomes.IS_UNDERGROUND += CommonTags.Biomes.IS_CAVE

		CommonTags.Biomes.IS_NETHER_FOREST += listOf(
			Biomes.CRIMSON_FOREST,
			Biomes.WARPED_FOREST
		)
		CommonTags.Biomes.IS_OUTER_END_ISLAND += listOf(
			Biomes.END_HIGHLANDS,
			Biomes.END_MIDLANDS,
			Biomes.END_BARRENS
		)

	}

}