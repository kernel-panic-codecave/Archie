package com.withertech.archie.data.internal.common.tags

import com.withertech.archie.Archie
import com.withertech.archie.data.common.tags.ATagsProvider
import com.withertech.archie.data.common.tags.ACommonTags
import dev.architectury.platform.Platform
import net.minecraft.core.HolderLookup
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.data.PackOutput
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.ItemTags
import net.minecraft.tags.TagKey
import net.minecraft.world.item.DyeColor
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer

 class AInternalItemTagsProvider(
	output: PackOutput,
	lookupProvider: CompletableFuture<HolderLookup.Provider>,
	blockTagsProvider: BlockTagsProvider
) : ATagsProvider.ItemTagsProvider(output, Archie.MOD, lookupProvider, blockTagsProvider, false)
{
	override fun generate(registries: HolderLookup.Provider)
	{
		if (Platform.isNeoForge())
		{
			ACommonTags.Items.ENCHANTING_FUELS += ACommonTags.Items.GEMS_LAPIS
		}
		copy(ACommonTags.Blocks.BARRELS, ACommonTags.Items.BARRELS)
		copy(
			ACommonTags.Blocks.BARRELS_WOODEN,
			ACommonTags.Items.BARRELS_WOODEN
		)
		ACommonTags.Items.BONES += Items.BONE
		copy(
			ACommonTags.Blocks.BOOKSHELVES,
			ACommonTags.Items.BOOKSHELVES
		)
		ACommonTags.Items.BRICKS += listOf(
			ACommonTags.Items.BRICKS_NORMAL,
			ACommonTags.Items.BRICKS_NETHER
		)
		ACommonTags.Items.BRICKS_NORMAL += Items.BRICK
		ACommonTags.Items.BRICKS_NETHER += Items.NETHER_BRICK
		ACommonTags.Items.BUCKETS_EMPTY += Items.BUCKET
		ACommonTags.Items.BUCKETS_WATER += Items.WATER_BUCKET
		ACommonTags.Items.BUCKETS_LAVA += Items.LAVA_BUCKET
		ACommonTags.Items.BUCKETS_MILK += Items.MILK_BUCKET
		ACommonTags.Items.BUCKETS_POWDER_SNOW += Items.POWDER_SNOW_BUCKET
		ACommonTags.Items.BUCKETS_ENTITY_WATER += listOf(
			Items.AXOLOTL_BUCKET,
			Items.COD_BUCKET,
			Items.PUFFERFISH_BUCKET,
			Items.TADPOLE_BUCKET,
			Items.TROPICAL_FISH_BUCKET,
			Items.SALMON_BUCKET
		)
		ACommonTags.Items.BUCKETS += listOf(
			ACommonTags.Items.BUCKETS_EMPTY,
			ACommonTags.Items.BUCKETS_WATER,
			ACommonTags.Items.BUCKETS_LAVA,
			ACommonTags.Items.BUCKETS_MILK,
			ACommonTags.Items.BUCKETS_POWDER_SNOW,
			ACommonTags.Items.BUCKETS_ENTITY_WATER
		)
		copy(
			ACommonTags.Blocks.BUDDING_BLOCKS,
			ACommonTags.Items.BUDDING_BLOCKS
		)
		copy(ACommonTags.Blocks.BUDS, ACommonTags.Items.BUDS)
		copy(ACommonTags.Blocks.CHAINS, ACommonTags.Items.CHAINS)
		copy(ACommonTags.Blocks.CHESTS, ACommonTags.Items.CHESTS)
		copy(
			ACommonTags.Blocks.CHESTS_ENDER,
			ACommonTags.Items.CHESTS_ENDER
		)
		copy(
			ACommonTags.Blocks.CHESTS_TRAPPED,
			ACommonTags.Items.CHESTS_TRAPPED
		)
		copy(
			ACommonTags.Blocks.CHESTS_WOODEN,
			ACommonTags.Items.CHESTS_WOODEN
		)
		copy(ACommonTags.Blocks.CLUSTERS, ACommonTags.Items.CLUSTERS)
		copy(
			ACommonTags.Blocks.COBBLESTONES,
			ACommonTags.Items.COBBLESTONES
		)
		copy(
			ACommonTags.Blocks.COBBLESTONES_NORMAL,
			ACommonTags.Items.COBBLESTONES_NORMAL
		)
		copy(
			ACommonTags.Blocks.COBBLESTONES_INFESTED,
			ACommonTags.Items.COBBLESTONES_INFESTED
		)
		copy(
			ACommonTags.Blocks.COBBLESTONES_MOSSY,
			ACommonTags.Items.COBBLESTONES_MOSSY
		)
		copy(
			ACommonTags.Blocks.COBBLESTONES_DEEPSLATE,
			ACommonTags.Items.COBBLESTONES_DEEPSLATE
		)
		ACommonTags.Items.CROPS += listOf(
			ACommonTags.Items.CROPS_BEETROOT,
			ACommonTags.Items.CROPS_CARROT,
			ACommonTags.Items.CROPS_NETHER_WART,
			ACommonTags.Items.CROPS_POTATO,
			ACommonTags.Items.CROPS_WHEAT
		)
		ACommonTags.Items.CROPS_BEETROOT += Items.BEETROOT
		ACommonTags.Items.CROPS_CARROT += Items.CARROT
		ACommonTags.Items.CROPS_NETHER_WART += Items.NETHER_WART
		ACommonTags.Items.CROPS_POTATO += Items.POTATO
		ACommonTags.Items.CROPS_WHEAT += Items.WHEAT
		addColored(ACommonTags.Items.DYED, "{color}_banner")
		addColored(ACommonTags.Items.DYED, "{color}_bed")
		addColored(ACommonTags.Items.DYED, "{color}_candle")
		addColored(ACommonTags.Items.DYED, "{color}_carpet")
		addColored(ACommonTags.Items.DYED, "{color}_concrete")
		addColored(ACommonTags.Items.DYED, "{color}_concrete_powder")
		addColored(ACommonTags.Items.DYED, "{color}_glazed_terracotta")
		addColored(ACommonTags.Items.DYED, "{color}_shulker_box")
		addColored(ACommonTags.Items.DYED, "{color}_stained_glass")
		addColored(ACommonTags.Items.DYED, "{color}_stained_glass_pane")
		addColored(ACommonTags.Items.DYED, "{color}_terracotta")
		addColored(ACommonTags.Items.DYED, "{color}_wool")
		addColoredTags(ACommonTags.Items.DYED) { values: TagKey<Item> ->
			ACommonTags.Items.DYED += values
		}
		ACommonTags.Items.DUSTS += listOf(
			ACommonTags.Items.DUSTS_GLOWSTONE,
			ACommonTags.Items.DUSTS_REDSTONE,
			ACommonTags.Items.DUSTS_PRISMARINE
		)
		ACommonTags.Items.DUSTS_GLOWSTONE += Items.GLOWSTONE_DUST
		ACommonTags.Items.DUSTS_REDSTONE += Items.REDSTONE
		ACommonTags.Items.DUSTS_PRISMARINE += Items.PRISMARINE_SHARD
		addColored(ACommonTags.Items.DYES, "{color}_dye")
		addColoredTags(ACommonTags.Items.DYES) { values: TagKey<Item> ->
			ACommonTags.Items.DYES += listOf(values)
		}
		ACommonTags.Items.EGGS += Items.EGG
		copy(ACommonTags.Blocks.END_STONES, ACommonTags.Items.END_STONES)
		ACommonTags.Items.ENDER_PEARLS += Items.ENDER_PEARL
		ACommonTags.Items.FEATHERS += Items.FEATHER
		copy(
			ACommonTags.Blocks.FENCE_GATES,
			ACommonTags.Items.FENCE_GATES
		)
		copy(
			ACommonTags.Blocks.FENCE_GATES_WOODEN,
			ACommonTags.Items.FENCE_GATES_WOODEN
		)
		copy(ACommonTags.Blocks.FENCES, ACommonTags.Items.FENCES)
		copy(
			ACommonTags.Blocks.FENCES_NETHER_BRICK,
			ACommonTags.Items.FENCES_NETHER_BRICK
		)
		copy(
			ACommonTags.Blocks.FENCES_WOODEN,
			ACommonTags.Items.FENCES_WOODEN
		)
		ACommonTags.Items.FOODS_FRUITS += listOf(
			Items.APPLE,
			Items.GOLDEN_APPLE,
			Items.ENCHANTED_GOLDEN_APPLE
		)
		ACommonTags.Items.FOODS_VEGETABLES += listOf(
			Items.CARROT,
			Items.GOLDEN_CARROT,
			Items.POTATO,
			Items.MELON_SLICE,
			Items.BEETROOT
		)
		ACommonTags.Items.FOODS_BERRIES += listOf(
			Items.SWEET_BERRIES,
			Items.GLOW_BERRIES
		)
		ACommonTags.Items.FOODS_BREADS += Items.BREAD
		ACommonTags.Items.FOODS_COOKIES += Items.COOKIE
		ACommonTags.Items.FOODS_RAW_MEATS += listOf(
			Items.BEEF,
			Items.PORKCHOP,
			Items.CHICKEN,
			Items.RABBIT,
			Items.MUTTON
		)
		ACommonTags.Items.FOODS_RAW_FISHES += listOf(
			Items.COD,
			Items.SALMON,
			Items.TROPICAL_FISH,
			Items.PUFFERFISH
		)
		ACommonTags.Items.FOODS_COOKED_MEATS += listOf(
			Items.COOKED_BEEF,
			Items.COOKED_PORKCHOP,
			Items.COOKED_CHICKEN,
			Items.COOKED_RABBIT,
			Items.COOKED_MUTTON
		)
		ACommonTags.Items.FOODS_COOKED_FISHES += listOf(
			Items.COOKED_COD,
			Items.COOKED_SALMON
		)
		ACommonTags.Items.FOODS_SOUPS += listOf(
			Items.BEETROOT_SOUP,
			Items.MUSHROOM_STEW,
			Items.RABBIT_STEW,
			Items.SUSPICIOUS_STEW
		)
		ACommonTags.Items.FOODS_CANDIES()
		ACommonTags.Items.FOODS_EDIBLE_WHEN_PLACED += Items.CAKE
		ACommonTags.Items.FOODS_FOOD_POISONING += listOf(
			Items.POISONOUS_POTATO,
			Items.PUFFERFISH,
			Items.SPIDER_EYE,
			Items.CHICKEN,
			Items.ROTTEN_FLESH
		)
		ACommonTags.Items.FOODS {
			add(
				Items.BAKED_POTATO,
				Items.PUMPKIN_PIE,
				Items.HONEY_BOTTLE,
				Items.OMINOUS_BOTTLE,
				Items.DRIED_KELP
			)
			addTags(
				ACommonTags.Items.FOODS_FRUITS,
				ACommonTags.Items.FOODS_VEGETABLES,
				ACommonTags.Items.FOODS_BERRIES,
				ACommonTags.Items.FOODS_BREADS,
				ACommonTags.Items.FOODS_COOKIES,
				ACommonTags.Items.FOODS_RAW_MEATS,
				ACommonTags.Items.FOODS_RAW_FISHES,
				ACommonTags.Items.FOODS_COOKED_MEATS,
				ACommonTags.Items.FOODS_COOKED_FISHES,
				ACommonTags.Items.FOODS_SOUPS,
				ACommonTags.Items.FOODS_CANDIES,
				ACommonTags.Items.FOODS_EDIBLE_WHEN_PLACED,
				ACommonTags.Items.FOODS_FOOD_POISONING
			)
		}
		ACommonTags.Items.GEMS += listOf(
			ACommonTags.Items.GEMS_AMETHYST,
			ACommonTags.Items.GEMS_DIAMOND,
			ACommonTags.Items.GEMS_EMERALD,
			ACommonTags.Items.GEMS_LAPIS,
			ACommonTags.Items.GEMS_PRISMARINE,
			ACommonTags.Items.GEMS_QUARTZ
		)
		ACommonTags.Items.GEMS_AMETHYST += Items.AMETHYST_SHARD
		ACommonTags.Items.GEMS_DIAMOND += Items.DIAMOND
		ACommonTags.Items.GEMS_EMERALD += Items.EMERALD
		ACommonTags.Items.GEMS_LAPIS += Items.LAPIS_LAZULI
		ACommonTags.Items.GEMS_PRISMARINE += Items.PRISMARINE_CRYSTALS
		ACommonTags.Items.GEMS_QUARTZ += Items.QUARTZ
		copy(
			ACommonTags.Blocks.GLASS_BLOCKS,
			ACommonTags.Items.GLASS_BLOCKS
		)
		copy(
			ACommonTags.Blocks.GLASS_BLOCKS_COLORLESS,
			ACommonTags.Items.GLASS_BLOCKS_COLORLESS
		)
		copy(
			ACommonTags.Blocks.GLASS_BLOCKS_TINTED,
			ACommonTags.Items.GLASS_BLOCKS_TINTED
		)
		copy(
			ACommonTags.Blocks.GLASS_BLOCKS_CHEAP,
			ACommonTags.Items.GLASS_BLOCKS_CHEAP
		)
		copy(
			ACommonTags.Blocks.GLASS_BLOCKS_STAINED,
			ACommonTags.Items.GLASS_BLOCKS_STAINED
		)
		copy(
			ACommonTags.Blocks.GLASS_PANES,
			ACommonTags.Items.GLASS_PANES
		)
		copy(
			ACommonTags.Blocks.GLASS_PANES_COLORLESS,
			ACommonTags.Items.GLASS_PANES_COLORLESS
		)
		copy(
			ACommonTags.Blocks.GLASS_PANES_STAINED,
			ACommonTags.Items.GLASS_PANES_STAINED
		)
		copy(ACommonTags.Blocks.GRAVELS, ACommonTags.Items.GRAVELS)
		ACommonTags.Items.GUNPOWDERS += Items.GUNPOWDER
		ACommonTags.Items.HIDDEN_FROM_RECIPE_VIEWERS()
		ACommonTags.Items.INGOTS += listOf(
			ACommonTags.Items.INGOTS_COPPER,
			ACommonTags.Items.INGOTS_GOLD,
			ACommonTags.Items.INGOTS_IRON,
			ACommonTags.Items.INGOTS_NETHERITE
		)
		ACommonTags.Items.INGOTS_COPPER += Items.COPPER_INGOT
		ACommonTags.Items.INGOTS_GOLD += Items.GOLD_INGOT
		ACommonTags.Items.INGOTS_IRON += Items.IRON_INGOT
		ACommonTags.Items.INGOTS_NETHERITE += Items.NETHERITE_INGOT
		ACommonTags.Items.LEATHERS += Items.LEATHER
		ACommonTags.Items.MUSHROOMS += listOf(
			Items.BROWN_MUSHROOM,
			Items.RED_MUSHROOM
		)
		ACommonTags.Items.NETHER_STARS += Items.NETHER_STAR
		copy(
			ACommonTags.Blocks.NETHERRACKS,
			ACommonTags.Items.NETHERRACKS
		)
		ACommonTags.Items.NUGGETS += listOf(
			ACommonTags.Items.NUGGETS_GOLD,
			ACommonTags.Items.NUGGETS_IRON
		)
		ACommonTags.Items.NUGGETS_IRON += Items.IRON_NUGGET
		ACommonTags.Items.NUGGETS_GOLD += Items.GOLD_NUGGET
		copy(ACommonTags.Blocks.OBSIDIANS, ACommonTags.Items.OBSIDIANS)
		copy(
			ACommonTags.Blocks.ORE_BEARING_GROUND_DEEPSLATE,
			ACommonTags.Items.ORE_BEARING_GROUND_DEEPSLATE
		)
		copy(
			ACommonTags.Blocks.ORE_BEARING_GROUND_NETHERRACK,
			ACommonTags.Items.ORE_BEARING_GROUND_NETHERRACK
		)
		copy(
			ACommonTags.Blocks.ORE_BEARING_GROUND_STONE,
			ACommonTags.Items.ORE_BEARING_GROUND_STONE
		)
		copy(
			ACommonTags.Blocks.ORE_RATES_DENSE,
			ACommonTags.Items.ORE_RATES_DENSE
		)
		copy(
			ACommonTags.Blocks.ORE_RATES_SINGULAR,
			ACommonTags.Items.ORE_RATES_SINGULAR
		)
		copy(
			ACommonTags.Blocks.ORE_RATES_SPARSE,
			ACommonTags.Items.ORE_RATES_SPARSE
		)
		copy(ACommonTags.Blocks.ORES, ACommonTags.Items.ORES)
		copy(ACommonTags.Blocks.ORES_COAL, ACommonTags.Items.ORES_COAL)
		copy(
			ACommonTags.Blocks.ORES_COPPER,
			ACommonTags.Items.ORES_COPPER
		)
		copy(
			ACommonTags.Blocks.ORES_DIAMOND,
			ACommonTags.Items.ORES_DIAMOND
		)
		copy(
			ACommonTags.Blocks.ORES_EMERALD,
			ACommonTags.Items.ORES_EMERALD
		)
		copy(ACommonTags.Blocks.ORES_GOLD, ACommonTags.Items.ORES_GOLD)
		copy(ACommonTags.Blocks.ORES_IRON, ACommonTags.Items.ORES_IRON)
		copy(ACommonTags.Blocks.ORES_LAPIS, ACommonTags.Items.ORES_LAPIS)
		copy(
			ACommonTags.Blocks.ORES_QUARTZ,
			ACommonTags.Items.ORES_QUARTZ
		)
		copy(
			ACommonTags.Blocks.ORES_REDSTONE,
			ACommonTags.Items.ORES_REDSTONE
		)
		copy(
			ACommonTags.Blocks.ORES_NETHERITE_SCRAP,
			ACommonTags.Items.ORES_NETHERITE_SCRAP
		)
		copy(
			ACommonTags.Blocks.ORES_IN_GROUND_DEEPSLATE,
			ACommonTags.Items.ORES_IN_GROUND_DEEPSLATE
		)
		copy(
			ACommonTags.Blocks.ORES_IN_GROUND_NETHERRACK,
			ACommonTags.Items.ORES_IN_GROUND_NETHERRACK
		)
		copy(
			ACommonTags.Blocks.ORES_IN_GROUND_STONE,
			ACommonTags.Items.ORES_IN_GROUND_STONE
		)
		copy(
			ACommonTags.Blocks.PLAYER_WORKSTATIONS_CRAFTING_TABLES,
			ACommonTags.Items.PLAYER_WORKSTATIONS_CRAFTING_TABLES
		)
		copy(
			ACommonTags.Blocks.PLAYER_WORKSTATIONS_FURNACES,
			ACommonTags.Items.PLAYER_WORKSTATIONS_FURNACES
		)
		ACommonTags.Items.RAW_BLOCKS += listOf(
			ACommonTags.Items.RAW_BLOCKS_COPPER,
			ACommonTags.Items.RAW_BLOCKS_GOLD,
			ACommonTags.Items.RAW_BLOCKS_IRON
		)
		ACommonTags.Items.RAW_BLOCKS_COPPER += Items.RAW_COPPER_BLOCK
		ACommonTags.Items.RAW_BLOCKS_GOLD += Items.RAW_GOLD_BLOCK
		ACommonTags.Items.RAW_BLOCKS_IRON += Items.RAW_IRON_BLOCK
		ACommonTags.Items.RAW_MATERIALS += listOf(
			ACommonTags.Items.RAW_MATERIALS_COPPER,
			ACommonTags.Items.RAW_MATERIALS_GOLD,
			ACommonTags.Items.RAW_MATERIALS_IRON
		)
		ACommonTags.Items.RAW_MATERIALS_COPPER += Items.RAW_COPPER
		ACommonTags.Items.RAW_MATERIALS_GOLD += Items.RAW_GOLD
		ACommonTags.Items.RAW_MATERIALS_IRON += Items.RAW_IRON
		ACommonTags.Items.RODS += listOf(
			ACommonTags.Items.RODS_WOODEN,
			ACommonTags.Items.RODS_BLAZE,
			ACommonTags.Items.RODS_BREEZE
		)
		ACommonTags.Items.RODS_BLAZE += Items.BLAZE_ROD
		ACommonTags.Items.RODS_BREEZE += Items.BREEZE_ROD
		ACommonTags.Items.RODS_WOODEN += Items.STICK
		copy(ACommonTags.Blocks.ROPES, ACommonTags.Items.ROPES)
		copy(ACommonTags.Blocks.SANDS, ACommonTags.Items.SANDS)
		copy(
			ACommonTags.Blocks.SANDS_COLORLESS,
			ACommonTags.Items.SANDS_COLORLESS
		)
		copy(ACommonTags.Blocks.SANDS_RED, ACommonTags.Items.SANDS_RED)
		copy(
			ACommonTags.Blocks.SANDSTONE_BLOCKS,
			ACommonTags.Items.SANDSTONE_BLOCKS
		)
		copy(
			ACommonTags.Blocks.SANDSTONE_SLABS,
			ACommonTags.Items.SANDSTONE_SLABS
		)
		copy(
			ACommonTags.Blocks.SANDSTONE_STAIRS,
			ACommonTags.Items.SANDSTONE_STAIRS
		)
		copy(
			ACommonTags.Blocks.SANDSTONE_RED_BLOCKS,
			ACommonTags.Items.SANDSTONE_RED_BLOCKS
		)
		copy(
			ACommonTags.Blocks.SANDSTONE_RED_SLABS,
			ACommonTags.Items.SANDSTONE_RED_SLABS
		)
		copy(
			ACommonTags.Blocks.SANDSTONE_RED_STAIRS,
			ACommonTags.Items.SANDSTONE_RED_STAIRS
		)
		copy(
			ACommonTags.Blocks.SANDSTONE_UNCOLORED_BLOCKS,
			ACommonTags.Items.SANDSTONE_UNCOLORED_BLOCKS
		)
		copy(
			ACommonTags.Blocks.SANDSTONE_UNCOLORED_SLABS,
			ACommonTags.Items.SANDSTONE_UNCOLORED_SLABS
		)
		copy(
			ACommonTags.Blocks.SANDSTONE_UNCOLORED_STAIRS,
			ACommonTags.Items.SANDSTONE_UNCOLORED_STAIRS
		)
		ACommonTags.Items.SEEDS += listOf(
			ACommonTags.Items.SEEDS_BEETROOT,
			ACommonTags.Items.SEEDS_MELON,
			ACommonTags.Items.SEEDS_PUMPKIN,
			ACommonTags.Items.SEEDS_WHEAT
		)
		ACommonTags.Items.SEEDS_BEETROOT += Items.BEETROOT_SEEDS
		ACommonTags.Items.SEEDS_MELON += Items.MELON_SEEDS
		ACommonTags.Items.SEEDS_PUMPKIN += Items.PUMPKIN_SEEDS
		ACommonTags.Items.SEEDS_WHEAT += Items.WHEAT_SEEDS
		copy(
			ACommonTags.Blocks.SHULKER_BOXES,
			ACommonTags.Items.SHULKER_BOXES
		)
		ACommonTags.Items.SLIMEBALLS += Items.SLIME_BALL
		copy(ACommonTags.Blocks.STONES, ACommonTags.Items.STONES)
		copy(
			ACommonTags.Blocks.STORAGE_BLOCKS,
			ACommonTags.Items.STORAGE_BLOCKS
		)
		copy(
			ACommonTags.Blocks.STORAGE_BLOCKS_AMETHYST,
			ACommonTags.Items.STORAGE_BLOCKS_AMETHYST
		)
		copy(
			ACommonTags.Blocks.STORAGE_BLOCKS_BONE_MEAL,
			ACommonTags.Items.STORAGE_BLOCKS_BONE_MEAL
		)
		copy(
			ACommonTags.Blocks.STORAGE_BLOCKS_COAL,
			ACommonTags.Items.STORAGE_BLOCKS_COAL
		)
		copy(
			ACommonTags.Blocks.STORAGE_BLOCKS_COPPER,
			ACommonTags.Items.STORAGE_BLOCKS_COPPER
		)
		copy(
			ACommonTags.Blocks.STORAGE_BLOCKS_DIAMOND,
			ACommonTags.Items.STORAGE_BLOCKS_DIAMOND
		)
		copy(
			ACommonTags.Blocks.STORAGE_BLOCKS_DRIED_KELP,
			ACommonTags.Items.STORAGE_BLOCKS_DRIED_KELP
		)
		copy(
			ACommonTags.Blocks.STORAGE_BLOCKS_EMERALD,
			ACommonTags.Items.STORAGE_BLOCKS_EMERALD
		)
		copy(
			ACommonTags.Blocks.STORAGE_BLOCKS_GOLD,
			ACommonTags.Items.STORAGE_BLOCKS_GOLD
		)
		copy(
			ACommonTags.Blocks.STORAGE_BLOCKS_IRON,
			ACommonTags.Items.STORAGE_BLOCKS_IRON
		)
		copy(
			ACommonTags.Blocks.STORAGE_BLOCKS_LAPIS,
			ACommonTags.Items.STORAGE_BLOCKS_LAPIS
		)
		copy(
			ACommonTags.Blocks.STORAGE_BLOCKS_NETHERITE,
			ACommonTags.Items.STORAGE_BLOCKS_NETHERITE
		)
		copy(
			ACommonTags.Blocks.STORAGE_BLOCKS_QUARTZ,
			ACommonTags.Items.STORAGE_BLOCKS_QUARTZ
		)
		copy(
			ACommonTags.Blocks.STORAGE_BLOCKS_RAW_COPPER,
			ACommonTags.Items.STORAGE_BLOCKS_RAW_COPPER
		)
		copy(
			ACommonTags.Blocks.STORAGE_BLOCKS_RAW_GOLD,
			ACommonTags.Items.STORAGE_BLOCKS_RAW_GOLD
		)
		copy(
			ACommonTags.Blocks.STORAGE_BLOCKS_RAW_IRON,
			ACommonTags.Items.STORAGE_BLOCKS_RAW_IRON
		)
		copy(
			ACommonTags.Blocks.STORAGE_BLOCKS_REDSTONE,
			ACommonTags.Items.STORAGE_BLOCKS_REDSTONE
		)
		copy(
			ACommonTags.Blocks.STORAGE_BLOCKS_SLIME,
			ACommonTags.Items.STORAGE_BLOCKS_SLIME
		)
		copy(
			ACommonTags.Blocks.STORAGE_BLOCKS_WHEAT,
			ACommonTags.Items.STORAGE_BLOCKS_WHEAT
		)
		ACommonTags.Items.STRINGS += Items.STRING
		ACommonTags.Items.VILLAGER_JOB_SITES += listOf(
			Items.BARREL, Items.BLAST_FURNACE, Items.BREWING_STAND, Items.CARTOGRAPHY_TABLE,
			Items.CAULDRON, Items.COMPOSTER, Items.FLETCHING_TABLE, Items.GRINDSTONE,
			Items.LECTERN, Items.LOOM, Items.SMITHING_TABLE, Items.SMOKER, Items.STONECUTTER
		)


		// Tools and Armors
		ACommonTags.Items.TOOLS_SHIELDS += Items.SHIELD
		ACommonTags.Items.TOOLS_BOWS += Items.BOW
		ACommonTags.Items.TOOLS_BRUSHES += Items.BRUSH
		ACommonTags.Items.TOOLS_CROSSBOWS += Items.CROSSBOW
		ACommonTags.Items.TOOLS_FISHING_RODS += Items.FISHING_ROD
		ACommonTags.Items.TOOLS_SHEARS += Items.SHEARS
		ACommonTags.Items.TOOLS_SPEARS += Items.TRIDENT
		ACommonTags.Items.TOOLS += listOf(
			ACommonTags.Items.TOOLS_AXES,
			ACommonTags.Items.TOOLS_HOES,
			ACommonTags.Items.TOOLS_PICKAXES,
			ACommonTags.Items.TOOLS_SHOVELS,
			ACommonTags.Items.TOOLS_SWORDS,

			ACommonTags.Items.TOOLS_BOWS,
			ACommonTags.Items.TOOLS_BRUSHES,
			ACommonTags.Items.TOOLS_CROSSBOWS,
			ACommonTags.Items.TOOLS_FISHING_RODS,
			ACommonTags.Items.TOOLS_SHEARS,
			ACommonTags.Items.TOOLS_SHIELDS,
			ACommonTags.Items.TOOLS_SPEARS
		)
		ACommonTags.Items.ARMORS += listOf(
			ACommonTags.Items.ARMORS_HELMETS,
			ACommonTags.Items.ARMORS_CHESTPLATES,
			ACommonTags.Items.ARMORS_LEGGINGS,
			ACommonTags.Items.ARMORS_BOOTS
		)
		ACommonTags.Items.ENCHANTABLES += listOf(
			ItemTags.ARMOR_ENCHANTABLE,
			ItemTags.EQUIPPABLE_ENCHANTABLE,
			ItemTags.WEAPON_ENCHANTABLE,
			ItemTags.SWORD_ENCHANTABLE,
			ItemTags.MINING_ENCHANTABLE,
			ItemTags.MINING_LOOT_ENCHANTABLE,
			ItemTags.FISHING_ENCHANTABLE,
			ItemTags.TRIDENT_ENCHANTABLE,
			ItemTags.BOW_ENCHANTABLE,
			ItemTags.CROSSBOW_ENCHANTABLE,
			ItemTags.FIRE_ASPECT_ENCHANTABLE,
			ItemTags.DURABILITY_ENCHANTABLE
		)

		ACommonTags.Items.ENCHANTABLES *= ItemTags.MACE_ENCHANTABLE


	}

	private fun addColored(group: TagKey<Item>, pattern: String)
	{
		val prefix = group.location().path.lowercase() + '/'
		for (color in DyeColor.entries)
		{
			val key = ResourceLocation.fromNamespaceAndPath("minecraft", pattern.replace("{color}", color.getName()))
			val tag = getCommonItemTag(prefix + color.getName())
			val item = BuiltInRegistries.ITEM[key]
			check(item !== Items.AIR) { "Unknown vanilla item: $key" }
			tag += item
		}
	}

	private fun addColoredTags(group: TagKey<Item>, consumer: Consumer<TagKey<Item>>)
	{
		val prefix = group.location().path.lowercase() + '/'
		for (color in DyeColor.entries)
		{
			val tag = getCommonItemTag(prefix + color.getName())
			consumer.accept(tag)
		}
	}

	private fun getCommonItemTag(name: String): TagKey<Item>
	{
		return ACommonTags.Items[ResourceLocation.fromNamespaceAndPath("c", name)]
			?: throw IllegalStateException(ACommonTags.Items::class.java.name + " is missing tag name: " + name)
	}

}