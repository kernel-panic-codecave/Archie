package com.withertech.archie.data.internal.common.tags

import com.withertech.archie.Archie
import com.withertech.archie.data.common.tags.ArchieTagsProvider
import com.withertech.archie.data.common.tags.CommonTags
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

 class ArchieInternalItemTagsProvider(
	output: PackOutput,
	lookupProvider: CompletableFuture<HolderLookup.Provider>,
	blockTagsProvider: ArchieTagsProvider.BlockTagsProvider
) : ArchieTagsProvider.ItemTagsProvider(output, Archie.MOD, lookupProvider, blockTagsProvider, false)
{
	override fun generate(provider: HolderLookup.Provider)
	{
		if (Platform.isNeoForge())
		{
			CommonTags.Items.ENCHANTING_FUELS += CommonTags.Items.GEMS_LAPIS
		}
		copy(CommonTags.Blocks.BARRELS, CommonTags.Items.BARRELS)
		copy(
			CommonTags.Blocks.BARRELS_WOODEN,
			CommonTags.Items.BARRELS_WOODEN
		)
		CommonTags.Items.BONES += Items.BONE
		copy(
			CommonTags.Blocks.BOOKSHELVES,
			CommonTags.Items.BOOKSHELVES
		)
		CommonTags.Items.BRICKS += listOf(
			CommonTags.Items.BRICKS_NORMAL,
			CommonTags.Items.BRICKS_NETHER
		)
		CommonTags.Items.BRICKS_NORMAL += Items.BRICK
		CommonTags.Items.BRICKS_NETHER += Items.NETHER_BRICK
		CommonTags.Items.BUCKETS_EMPTY += Items.BUCKET
		CommonTags.Items.BUCKETS_WATER += Items.WATER_BUCKET
		CommonTags.Items.BUCKETS_LAVA += Items.LAVA_BUCKET
		CommonTags.Items.BUCKETS_MILK += Items.MILK_BUCKET
		CommonTags.Items.BUCKETS_POWDER_SNOW += Items.POWDER_SNOW_BUCKET
		CommonTags.Items.BUCKETS_ENTITY_WATER += listOf(
			Items.AXOLOTL_BUCKET,
			Items.COD_BUCKET,
			Items.PUFFERFISH_BUCKET,
			Items.TADPOLE_BUCKET,
			Items.TROPICAL_FISH_BUCKET,
			Items.SALMON_BUCKET
		)
		CommonTags.Items.BUCKETS += listOf(
			CommonTags.Items.BUCKETS_EMPTY,
			CommonTags.Items.BUCKETS_WATER,
			CommonTags.Items.BUCKETS_LAVA,
			CommonTags.Items.BUCKETS_MILK,
			CommonTags.Items.BUCKETS_POWDER_SNOW,
			CommonTags.Items.BUCKETS_ENTITY_WATER
		)
		copy(
			CommonTags.Blocks.BUDDING_BLOCKS,
			CommonTags.Items.BUDDING_BLOCKS
		)
		copy(CommonTags.Blocks.BUDS, CommonTags.Items.BUDS)
		copy(CommonTags.Blocks.CHAINS, CommonTags.Items.CHAINS)
		copy(CommonTags.Blocks.CHESTS, CommonTags.Items.CHESTS)
		copy(
			CommonTags.Blocks.CHESTS_ENDER,
			CommonTags.Items.CHESTS_ENDER
		)
		copy(
			CommonTags.Blocks.CHESTS_TRAPPED,
			CommonTags.Items.CHESTS_TRAPPED
		)
		copy(
			CommonTags.Blocks.CHESTS_WOODEN,
			CommonTags.Items.CHESTS_WOODEN
		)
		copy(CommonTags.Blocks.CLUSTERS, CommonTags.Items.CLUSTERS)
		copy(
			CommonTags.Blocks.COBBLESTONES,
			CommonTags.Items.COBBLESTONES
		)
		copy(
			CommonTags.Blocks.COBBLESTONES_NORMAL,
			CommonTags.Items.COBBLESTONES_NORMAL
		)
		copy(
			CommonTags.Blocks.COBBLESTONES_INFESTED,
			CommonTags.Items.COBBLESTONES_INFESTED
		)
		copy(
			CommonTags.Blocks.COBBLESTONES_MOSSY,
			CommonTags.Items.COBBLESTONES_MOSSY
		)
		copy(
			CommonTags.Blocks.COBBLESTONES_DEEPSLATE,
			CommonTags.Items.COBBLESTONES_DEEPSLATE
		)
		CommonTags.Items.CROPS += listOf(
			CommonTags.Items.CROPS_BEETROOT,
			CommonTags.Items.CROPS_CARROT,
			CommonTags.Items.CROPS_NETHER_WART,
			CommonTags.Items.CROPS_POTATO,
			CommonTags.Items.CROPS_WHEAT
		)
		CommonTags.Items.CROPS_BEETROOT += Items.BEETROOT
		CommonTags.Items.CROPS_CARROT += Items.CARROT
		CommonTags.Items.CROPS_NETHER_WART += Items.NETHER_WART
		CommonTags.Items.CROPS_POTATO += Items.POTATO
		CommonTags.Items.CROPS_WHEAT += Items.WHEAT
		addColored(CommonTags.Items.DYED, "{color}_banner")
		addColored(CommonTags.Items.DYED, "{color}_bed")
		addColored(CommonTags.Items.DYED, "{color}_candle")
		addColored(CommonTags.Items.DYED, "{color}_carpet")
		addColored(CommonTags.Items.DYED, "{color}_concrete")
		addColored(CommonTags.Items.DYED, "{color}_concrete_powder")
		addColored(CommonTags.Items.DYED, "{color}_glazed_terracotta")
		addColored(CommonTags.Items.DYED, "{color}_shulker_box")
		addColored(CommonTags.Items.DYED, "{color}_stained_glass")
		addColored(CommonTags.Items.DYED, "{color}_stained_glass_pane")
		addColored(CommonTags.Items.DYED, "{color}_terracotta")
		addColored(CommonTags.Items.DYED, "{color}_wool")
		addColoredTags(CommonTags.Items.DYED) { values: TagKey<Item> ->
			CommonTags.Items.DYED += values
		}
		CommonTags.Items.DUSTS += listOf(
			CommonTags.Items.DUSTS_GLOWSTONE,
			CommonTags.Items.DUSTS_REDSTONE,
			CommonTags.Items.DUSTS_PRISMARINE
		)
		CommonTags.Items.DUSTS_GLOWSTONE += Items.GLOWSTONE_DUST
		CommonTags.Items.DUSTS_REDSTONE += Items.REDSTONE
		CommonTags.Items.DUSTS_PRISMARINE += Items.PRISMARINE_SHARD
		addColored(CommonTags.Items.DYES, "{color}_dye")
		addColoredTags(CommonTags.Items.DYES) { values: TagKey<Item> ->
			CommonTags.Items.DYES += listOf(values)
		}
		CommonTags.Items.EGGS += Items.EGG
		copy(CommonTags.Blocks.END_STONES, CommonTags.Items.END_STONES)
		CommonTags.Items.ENDER_PEARLS += Items.ENDER_PEARL
		CommonTags.Items.FEATHERS += Items.FEATHER
		copy(
			CommonTags.Blocks.FENCE_GATES,
			CommonTags.Items.FENCE_GATES
		)
		copy(
			CommonTags.Blocks.FENCE_GATES_WOODEN,
			CommonTags.Items.FENCE_GATES_WOODEN
		)
		copy(CommonTags.Blocks.FENCES, CommonTags.Items.FENCES)
		copy(
			CommonTags.Blocks.FENCES_NETHER_BRICK,
			CommonTags.Items.FENCES_NETHER_BRICK
		)
		copy(
			CommonTags.Blocks.FENCES_WOODEN,
			CommonTags.Items.FENCES_WOODEN
		)
		CommonTags.Items.FOODS_FRUITS += listOf(
			Items.APPLE,
			Items.GOLDEN_APPLE,
			Items.ENCHANTED_GOLDEN_APPLE
		)
		CommonTags.Items.FOODS_VEGETABLES += listOf(
			Items.CARROT,
			Items.GOLDEN_CARROT,
			Items.POTATO,
			Items.MELON_SLICE,
			Items.BEETROOT
		)
		CommonTags.Items.FOODS_BERRIES += listOf(
			Items.SWEET_BERRIES,
			Items.GLOW_BERRIES
		)
		CommonTags.Items.FOODS_BREADS += Items.BREAD
		CommonTags.Items.FOODS_COOKIES += Items.COOKIE
		CommonTags.Items.FOODS_RAW_MEATS += listOf(
			Items.BEEF,
			Items.PORKCHOP,
			Items.CHICKEN,
			Items.RABBIT,
			Items.MUTTON
		)
		CommonTags.Items.FOODS_RAW_FISHES += listOf(
			Items.COD,
			Items.SALMON,
			Items.TROPICAL_FISH,
			Items.PUFFERFISH
		)
		CommonTags.Items.FOODS_COOKED_MEATS += listOf(
			Items.COOKED_BEEF,
			Items.COOKED_PORKCHOP,
			Items.COOKED_CHICKEN,
			Items.COOKED_RABBIT,
			Items.COOKED_MUTTON
		)
		CommonTags.Items.FOODS_COOKED_FISHES += listOf(
			Items.COOKED_COD,
			Items.COOKED_SALMON
		)
		CommonTags.Items.FOODS_SOUPS += listOf(
			Items.BEETROOT_SOUP,
			Items.MUSHROOM_STEW,
			Items.RABBIT_STEW,
			Items.SUSPICIOUS_STEW
		)
		CommonTags.Items.FOODS_CANDIES()
		CommonTags.Items.FOODS_EDIBLE_WHEN_PLACED += Items.CAKE
		CommonTags.Items.FOODS_FOOD_POISONING += listOf(
			Items.POISONOUS_POTATO,
			Items.PUFFERFISH,
			Items.SPIDER_EYE,
			Items.CHICKEN,
			Items.ROTTEN_FLESH
		)
		CommonTags.Items.FOODS {
			add(
				Items.BAKED_POTATO,
				Items.PUMPKIN_PIE,
				Items.HONEY_BOTTLE,
				Items.OMINOUS_BOTTLE,
				Items.DRIED_KELP
			)
			addTags(
				CommonTags.Items.FOODS_FRUITS,
				CommonTags.Items.FOODS_VEGETABLES,
				CommonTags.Items.FOODS_BERRIES,
				CommonTags.Items.FOODS_BREADS,
				CommonTags.Items.FOODS_COOKIES,
				CommonTags.Items.FOODS_RAW_MEATS,
				CommonTags.Items.FOODS_RAW_FISHES,
				CommonTags.Items.FOODS_COOKED_MEATS,
				CommonTags.Items.FOODS_COOKED_FISHES,
				CommonTags.Items.FOODS_SOUPS,
				CommonTags.Items.FOODS_CANDIES,
				CommonTags.Items.FOODS_EDIBLE_WHEN_PLACED,
				CommonTags.Items.FOODS_FOOD_POISONING
			)
		}
		CommonTags.Items.GEMS += listOf(
			CommonTags.Items.GEMS_AMETHYST,
			CommonTags.Items.GEMS_DIAMOND,
			CommonTags.Items.GEMS_EMERALD,
			CommonTags.Items.GEMS_LAPIS,
			CommonTags.Items.GEMS_PRISMARINE,
			CommonTags.Items.GEMS_QUARTZ
		)
		CommonTags.Items.GEMS_AMETHYST += Items.AMETHYST_SHARD
		CommonTags.Items.GEMS_DIAMOND += Items.DIAMOND
		CommonTags.Items.GEMS_EMERALD += Items.EMERALD
		CommonTags.Items.GEMS_LAPIS += Items.LAPIS_LAZULI
		CommonTags.Items.GEMS_PRISMARINE += Items.PRISMARINE_CRYSTALS
		CommonTags.Items.GEMS_QUARTZ += Items.QUARTZ
		copy(
			CommonTags.Blocks.GLASS_BLOCKS,
			CommonTags.Items.GLASS_BLOCKS
		)
		copy(
			CommonTags.Blocks.GLASS_BLOCKS_COLORLESS,
			CommonTags.Items.GLASS_BLOCKS_COLORLESS
		)
		copy(
			CommonTags.Blocks.GLASS_BLOCKS_TINTED,
			CommonTags.Items.GLASS_BLOCKS_TINTED
		)
		copy(
			CommonTags.Blocks.GLASS_BLOCKS_CHEAP,
			CommonTags.Items.GLASS_BLOCKS_CHEAP
		)
		copy(
			CommonTags.Blocks.GLASS_BLOCKS_STAINED,
			CommonTags.Items.GLASS_BLOCKS_STAINED
		)
		copy(
			CommonTags.Blocks.GLASS_PANES,
			CommonTags.Items.GLASS_PANES
		)
		copy(
			CommonTags.Blocks.GLASS_PANES_COLORLESS,
			CommonTags.Items.GLASS_PANES_COLORLESS
		)
		copy(
			CommonTags.Blocks.GLASS_PANES_STAINED,
			CommonTags.Items.GLASS_PANES_STAINED
		)
		copy(CommonTags.Blocks.GRAVELS, CommonTags.Items.GRAVELS)
		CommonTags.Items.GUNPOWDERS += Items.GUNPOWDER
		CommonTags.Items.HIDDEN_FROM_RECIPE_VIEWERS()
		CommonTags.Items.INGOTS += listOf(
			CommonTags.Items.INGOTS_COPPER,
			CommonTags.Items.INGOTS_GOLD,
			CommonTags.Items.INGOTS_IRON,
			CommonTags.Items.INGOTS_NETHERITE
		)
		CommonTags.Items.INGOTS_COPPER += Items.COPPER_INGOT
		CommonTags.Items.INGOTS_GOLD += Items.GOLD_INGOT
		CommonTags.Items.INGOTS_IRON += Items.IRON_INGOT
		CommonTags.Items.INGOTS_NETHERITE += Items.NETHERITE_INGOT
		CommonTags.Items.LEATHERS += Items.LEATHER
		CommonTags.Items.MUSHROOMS += listOf(
			Items.BROWN_MUSHROOM,
			Items.RED_MUSHROOM
		)
		CommonTags.Items.NETHER_STARS += Items.NETHER_STAR
		copy(
			CommonTags.Blocks.NETHERRACKS,
			CommonTags.Items.NETHERRACKS
		)
		CommonTags.Items.NUGGETS += listOf(
			CommonTags.Items.NUGGETS_GOLD,
			CommonTags.Items.NUGGETS_IRON
		)
		CommonTags.Items.NUGGETS_IRON += Items.IRON_NUGGET
		CommonTags.Items.NUGGETS_GOLD += Items.GOLD_NUGGET
		copy(CommonTags.Blocks.OBSIDIANS, CommonTags.Items.OBSIDIANS)
		copy(
			CommonTags.Blocks.ORE_BEARING_GROUND_DEEPSLATE,
			CommonTags.Items.ORE_BEARING_GROUND_DEEPSLATE
		)
		copy(
			CommonTags.Blocks.ORE_BEARING_GROUND_NETHERRACK,
			CommonTags.Items.ORE_BEARING_GROUND_NETHERRACK
		)
		copy(
			CommonTags.Blocks.ORE_BEARING_GROUND_STONE,
			CommonTags.Items.ORE_BEARING_GROUND_STONE
		)
		copy(
			CommonTags.Blocks.ORE_RATES_DENSE,
			CommonTags.Items.ORE_RATES_DENSE
		)
		copy(
			CommonTags.Blocks.ORE_RATES_SINGULAR,
			CommonTags.Items.ORE_RATES_SINGULAR
		)
		copy(
			CommonTags.Blocks.ORE_RATES_SPARSE,
			CommonTags.Items.ORE_RATES_SPARSE
		)
		copy(CommonTags.Blocks.ORES, CommonTags.Items.ORES)
		copy(CommonTags.Blocks.ORES_COAL, CommonTags.Items.ORES_COAL)
		copy(
			CommonTags.Blocks.ORES_COPPER,
			CommonTags.Items.ORES_COPPER
		)
		copy(
			CommonTags.Blocks.ORES_DIAMOND,
			CommonTags.Items.ORES_DIAMOND
		)
		copy(
			CommonTags.Blocks.ORES_EMERALD,
			CommonTags.Items.ORES_EMERALD
		)
		copy(CommonTags.Blocks.ORES_GOLD, CommonTags.Items.ORES_GOLD)
		copy(CommonTags.Blocks.ORES_IRON, CommonTags.Items.ORES_IRON)
		copy(CommonTags.Blocks.ORES_LAPIS, CommonTags.Items.ORES_LAPIS)
		copy(
			CommonTags.Blocks.ORES_QUARTZ,
			CommonTags.Items.ORES_QUARTZ
		)
		copy(
			CommonTags.Blocks.ORES_REDSTONE,
			CommonTags.Items.ORES_REDSTONE
		)
		copy(
			CommonTags.Blocks.ORES_NETHERITE_SCRAP,
			CommonTags.Items.ORES_NETHERITE_SCRAP
		)
		copy(
			CommonTags.Blocks.ORES_IN_GROUND_DEEPSLATE,
			CommonTags.Items.ORES_IN_GROUND_DEEPSLATE
		)
		copy(
			CommonTags.Blocks.ORES_IN_GROUND_NETHERRACK,
			CommonTags.Items.ORES_IN_GROUND_NETHERRACK
		)
		copy(
			CommonTags.Blocks.ORES_IN_GROUND_STONE,
			CommonTags.Items.ORES_IN_GROUND_STONE
		)
		copy(
			CommonTags.Blocks.PLAYER_WORKSTATIONS_CRAFTING_TABLES,
			CommonTags.Items.PLAYER_WORKSTATIONS_CRAFTING_TABLES
		)
		copy(
			CommonTags.Blocks.PLAYER_WORKSTATIONS_FURNACES,
			CommonTags.Items.PLAYER_WORKSTATIONS_FURNACES
		)
		CommonTags.Items.RAW_BLOCKS += listOf(
			CommonTags.Items.RAW_BLOCKS_COPPER,
			CommonTags.Items.RAW_BLOCKS_GOLD,
			CommonTags.Items.RAW_BLOCKS_IRON
		)
		CommonTags.Items.RAW_BLOCKS_COPPER += Items.RAW_COPPER_BLOCK
		CommonTags.Items.RAW_BLOCKS_GOLD += Items.RAW_GOLD_BLOCK
		CommonTags.Items.RAW_BLOCKS_IRON += Items.RAW_IRON_BLOCK
		CommonTags.Items.RAW_MATERIALS += listOf(
			CommonTags.Items.RAW_MATERIALS_COPPER,
			CommonTags.Items.RAW_MATERIALS_GOLD,
			CommonTags.Items.RAW_MATERIALS_IRON
		)
		CommonTags.Items.RAW_MATERIALS_COPPER += Items.RAW_COPPER
		CommonTags.Items.RAW_MATERIALS_GOLD += Items.RAW_GOLD
		CommonTags.Items.RAW_MATERIALS_IRON += Items.RAW_IRON
		CommonTags.Items.RODS += listOf(
			CommonTags.Items.RODS_WOODEN,
			CommonTags.Items.RODS_BLAZE,
			CommonTags.Items.RODS_BREEZE
		)
		CommonTags.Items.RODS_BLAZE += Items.BLAZE_ROD
		CommonTags.Items.RODS_BREEZE += Items.BREEZE_ROD
		CommonTags.Items.RODS_WOODEN += Items.STICK
		copy(CommonTags.Blocks.ROPES, CommonTags.Items.ROPES)
		copy(CommonTags.Blocks.SANDS, CommonTags.Items.SANDS)
		copy(
			CommonTags.Blocks.SANDS_COLORLESS,
			CommonTags.Items.SANDS_COLORLESS
		)
		copy(CommonTags.Blocks.SANDS_RED, CommonTags.Items.SANDS_RED)
		copy(
			CommonTags.Blocks.SANDSTONE_BLOCKS,
			CommonTags.Items.SANDSTONE_BLOCKS
		)
		copy(
			CommonTags.Blocks.SANDSTONE_SLABS,
			CommonTags.Items.SANDSTONE_SLABS
		)
		copy(
			CommonTags.Blocks.SANDSTONE_STAIRS,
			CommonTags.Items.SANDSTONE_STAIRS
		)
		copy(
			CommonTags.Blocks.SANDSTONE_RED_BLOCKS,
			CommonTags.Items.SANDSTONE_RED_BLOCKS
		)
		copy(
			CommonTags.Blocks.SANDSTONE_RED_SLABS,
			CommonTags.Items.SANDSTONE_RED_SLABS
		)
		copy(
			CommonTags.Blocks.SANDSTONE_RED_STAIRS,
			CommonTags.Items.SANDSTONE_RED_STAIRS
		)
		copy(
			CommonTags.Blocks.SANDSTONE_UNCOLORED_BLOCKS,
			CommonTags.Items.SANDSTONE_UNCOLORED_BLOCKS
		)
		copy(
			CommonTags.Blocks.SANDSTONE_UNCOLORED_SLABS,
			CommonTags.Items.SANDSTONE_UNCOLORED_SLABS
		)
		copy(
			CommonTags.Blocks.SANDSTONE_UNCOLORED_STAIRS,
			CommonTags.Items.SANDSTONE_UNCOLORED_STAIRS
		)
		CommonTags.Items.SEEDS += listOf(
			CommonTags.Items.SEEDS_BEETROOT,
			CommonTags.Items.SEEDS_MELON,
			CommonTags.Items.SEEDS_PUMPKIN,
			CommonTags.Items.SEEDS_WHEAT
		)
		CommonTags.Items.SEEDS_BEETROOT += Items.BEETROOT_SEEDS
		CommonTags.Items.SEEDS_MELON += Items.MELON_SEEDS
		CommonTags.Items.SEEDS_PUMPKIN += Items.PUMPKIN_SEEDS
		CommonTags.Items.SEEDS_WHEAT += Items.WHEAT_SEEDS
		copy(
			CommonTags.Blocks.SHULKER_BOXES,
			CommonTags.Items.SHULKER_BOXES
		)
		CommonTags.Items.SLIMEBALLS += Items.SLIME_BALL
		copy(CommonTags.Blocks.STONES, CommonTags.Items.STONES)
		copy(
			CommonTags.Blocks.STORAGE_BLOCKS,
			CommonTags.Items.STORAGE_BLOCKS
		)
		copy(
			CommonTags.Blocks.STORAGE_BLOCKS_AMETHYST,
			CommonTags.Items.STORAGE_BLOCKS_AMETHYST
		)
		copy(
			CommonTags.Blocks.STORAGE_BLOCKS_BONE_MEAL,
			CommonTags.Items.STORAGE_BLOCKS_BONE_MEAL
		)
		copy(
			CommonTags.Blocks.STORAGE_BLOCKS_COAL,
			CommonTags.Items.STORAGE_BLOCKS_COAL
		)
		copy(
			CommonTags.Blocks.STORAGE_BLOCKS_COPPER,
			CommonTags.Items.STORAGE_BLOCKS_COPPER
		)
		copy(
			CommonTags.Blocks.STORAGE_BLOCKS_DIAMOND,
			CommonTags.Items.STORAGE_BLOCKS_DIAMOND
		)
		copy(
			CommonTags.Blocks.STORAGE_BLOCKS_DRIED_KELP,
			CommonTags.Items.STORAGE_BLOCKS_DRIED_KELP
		)
		copy(
			CommonTags.Blocks.STORAGE_BLOCKS_EMERALD,
			CommonTags.Items.STORAGE_BLOCKS_EMERALD
		)
		copy(
			CommonTags.Blocks.STORAGE_BLOCKS_GOLD,
			CommonTags.Items.STORAGE_BLOCKS_GOLD
		)
		copy(
			CommonTags.Blocks.STORAGE_BLOCKS_IRON,
			CommonTags.Items.STORAGE_BLOCKS_IRON
		)
		copy(
			CommonTags.Blocks.STORAGE_BLOCKS_LAPIS,
			CommonTags.Items.STORAGE_BLOCKS_LAPIS
		)
		copy(
			CommonTags.Blocks.STORAGE_BLOCKS_NETHERITE,
			CommonTags.Items.STORAGE_BLOCKS_NETHERITE
		)
		copy(
			CommonTags.Blocks.STORAGE_BLOCKS_QUARTZ,
			CommonTags.Items.STORAGE_BLOCKS_QUARTZ
		)
		copy(
			CommonTags.Blocks.STORAGE_BLOCKS_RAW_COPPER,
			CommonTags.Items.STORAGE_BLOCKS_RAW_COPPER
		)
		copy(
			CommonTags.Blocks.STORAGE_BLOCKS_RAW_GOLD,
			CommonTags.Items.STORAGE_BLOCKS_RAW_GOLD
		)
		copy(
			CommonTags.Blocks.STORAGE_BLOCKS_RAW_IRON,
			CommonTags.Items.STORAGE_BLOCKS_RAW_IRON
		)
		copy(
			CommonTags.Blocks.STORAGE_BLOCKS_REDSTONE,
			CommonTags.Items.STORAGE_BLOCKS_REDSTONE
		)
		copy(
			CommonTags.Blocks.STORAGE_BLOCKS_SLIME,
			CommonTags.Items.STORAGE_BLOCKS_SLIME
		)
		copy(
			CommonTags.Blocks.STORAGE_BLOCKS_WHEAT,
			CommonTags.Items.STORAGE_BLOCKS_WHEAT
		)
		CommonTags.Items.STRINGS += Items.STRING
		CommonTags.Items.VILLAGER_JOB_SITES += listOf(
			Items.BARREL, Items.BLAST_FURNACE, Items.BREWING_STAND, Items.CARTOGRAPHY_TABLE,
			Items.CAULDRON, Items.COMPOSTER, Items.FLETCHING_TABLE, Items.GRINDSTONE,
			Items.LECTERN, Items.LOOM, Items.SMITHING_TABLE, Items.SMOKER, Items.STONECUTTER
		)


		// Tools and Armors
		CommonTags.Items.TOOLS_SHIELDS += Items.SHIELD
		CommonTags.Items.TOOLS_BOWS += Items.BOW
		CommonTags.Items.TOOLS_BRUSHES += Items.BRUSH
		CommonTags.Items.TOOLS_CROSSBOWS += Items.CROSSBOW
		CommonTags.Items.TOOLS_FISHING_RODS += Items.FISHING_ROD
		CommonTags.Items.TOOLS_SHEARS += Items.SHEARS
		CommonTags.Items.TOOLS_SPEARS += Items.TRIDENT
		CommonTags.Items.TOOLS += listOf(
			CommonTags.Items.TOOLS_AXES,
			CommonTags.Items.TOOLS_HOES,
			CommonTags.Items.TOOLS_PICKAXES,
			CommonTags.Items.TOOLS_SHOVELS,
			CommonTags.Items.TOOLS_SWORDS,

			CommonTags.Items.TOOLS_BOWS,
			CommonTags.Items.TOOLS_BRUSHES,
			CommonTags.Items.TOOLS_CROSSBOWS,
			CommonTags.Items.TOOLS_FISHING_RODS,
			CommonTags.Items.TOOLS_SHEARS,
			CommonTags.Items.TOOLS_SHIELDS,
			CommonTags.Items.TOOLS_SPEARS
		)
		CommonTags.Items.ARMORS += listOf(
			CommonTags.Items.ARMORS_HELMETS,
			CommonTags.Items.ARMORS_CHESTPLATES,
			CommonTags.Items.ARMORS_LEGGINGS,
			CommonTags.Items.ARMORS_BOOTS
		)
		CommonTags.Items.ENCHANTABLES += listOf(
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

		CommonTags.Items.ENCHANTABLES *= ItemTags.MACE_ENCHANTABLE


	}

	private fun addColored(group: TagKey<Item>, pattern: String)
	{
		val prefix = group.location().path.lowercase() + '/'
		for (color in DyeColor.entries)
		{
			val key = ResourceLocation("minecraft", pattern.replace("{color}", color.getName()))
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
		return CommonTags.Items[ResourceLocation("c", name)]
			?: throw IllegalStateException(CommonTags.Items::class.java.getName() + " is missing tag name: " + name)
	}

}