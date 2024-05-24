package com.withertech.archie.data.internal.common.tags

import com.withertech.archie.Archie
import com.withertech.archie.data.common.tags.ArchieTagsProvider
import com.withertech.archie.data.common.tags.CommonTags
import dev.architectury.platform.Platform
import net.minecraft.core.HolderLookup
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.data.PackOutput
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.BlockTags
import net.minecraft.tags.TagKey
import net.minecraft.world.item.DyeColor
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer

class ArchieInternalBlockTagsProvider(
	output: PackOutput,
	lookupProvider: CompletableFuture<HolderLookup.Provider>
) :
	ArchieTagsProvider.BlockTagsProvider(
		output, Archie.MOD,
		lookupProvider,
		false
	)
{
	override fun generate(provider: HolderLookup.Provider)
	{
		if (Platform.isNeoForge())
		{
			CommonTags.Blocks.ENDERMAN_PLACE_ON_BLACKLIST()
		}
		CommonTags.Blocks.BARRELS += CommonTags.Blocks.BARRELS_WOODEN
		CommonTags.Blocks.BARRELS_WOODEN += Blocks.BARREL
		CommonTags.Blocks.BOOKSHELVES += Blocks.BOOKSHELF
		CommonTags.Blocks.BUDDING_BLOCKS += Blocks.BUDDING_AMETHYST
		CommonTags.Blocks.BUDS += listOf(
			Blocks.SMALL_AMETHYST_BUD,
			Blocks.MEDIUM_AMETHYST_BUD,
			Blocks.LARGE_AMETHYST_BUD
		)
		CommonTags.Blocks.CHAINS += Blocks.CHAIN
		CommonTags.Blocks.CHESTS += listOf(
			CommonTags.Blocks.CHESTS_ENDER,
			CommonTags.Blocks.CHESTS_TRAPPED,
			CommonTags.Blocks.CHESTS_WOODEN
		)
		CommonTags.Blocks.CHESTS_ENDER += Blocks.ENDER_CHEST
		CommonTags.Blocks.CHESTS_TRAPPED += Blocks.TRAPPED_CHEST
		CommonTags.Blocks.CHESTS_WOODEN += listOf(
			Blocks.CHEST,
			Blocks.TRAPPED_CHEST
		)
		CommonTags.Blocks.CLUSTERS += Blocks.AMETHYST_CLUSTER
		CommonTags.Blocks.SHULKER_BOXES += listOf(
			Blocks.SHULKER_BOX,
			Blocks.BLUE_SHULKER_BOX,
			Blocks.BROWN_SHULKER_BOX,
			Blocks.CYAN_SHULKER_BOX,
			Blocks.GRAY_SHULKER_BOX,
			Blocks.GREEN_SHULKER_BOX,
			Blocks.LIGHT_BLUE_SHULKER_BOX,
			Blocks.LIGHT_GRAY_SHULKER_BOX,
			Blocks.LIME_SHULKER_BOX,
			Blocks.MAGENTA_SHULKER_BOX,
			Blocks.ORANGE_SHULKER_BOX,
			Blocks.PINK_SHULKER_BOX,
			Blocks.PURPLE_SHULKER_BOX,
			Blocks.RED_SHULKER_BOX,
			Blocks.WHITE_SHULKER_BOX,
			Blocks.YELLOW_SHULKER_BOX,
			Blocks.BLACK_SHULKER_BOX
		)
		CommonTags.Blocks.COBBLESTONES += listOf(
			CommonTags.Blocks.COBBLESTONES_NORMAL,
			CommonTags.Blocks.COBBLESTONES_INFESTED,
			CommonTags.Blocks.COBBLESTONES_MOSSY,
			CommonTags.Blocks.COBBLESTONES_DEEPSLATE
		)
		CommonTags.Blocks.COBBLESTONES_NORMAL += Blocks.COBBLESTONE
		CommonTags.Blocks.COBBLESTONES_INFESTED += Blocks.INFESTED_COBBLESTONE
		CommonTags.Blocks.COBBLESTONES_MOSSY += Blocks.MOSSY_COBBLESTONE
		CommonTags.Blocks.COBBLESTONES_DEEPSLATE += Blocks.COBBLED_DEEPSLATE
		CommonTags.Blocks.END_STONES += Blocks.END_STONE
		CommonTags.Blocks.FENCE_GATES += CommonTags.Blocks.FENCE_GATES_WOODEN
		CommonTags.Blocks.FENCE_GATES_WOODEN += listOf(
			Blocks.OAK_FENCE_GATE,
			Blocks.SPRUCE_FENCE_GATE,
			Blocks.BIRCH_FENCE_GATE,
			Blocks.JUNGLE_FENCE_GATE,
			Blocks.ACACIA_FENCE_GATE,
			Blocks.DARK_OAK_FENCE_GATE,
			Blocks.CRIMSON_FENCE_GATE,
			Blocks.WARPED_FENCE_GATE,
			Blocks.MANGROVE_FENCE_GATE,
			Blocks.BAMBOO_FENCE_GATE,
			Blocks.CHERRY_FENCE_GATE
		)
		CommonTags.Blocks.FENCES += listOf(
			CommonTags.Blocks.FENCES_NETHER_BRICK,
			CommonTags.Blocks.FENCES_WOODEN
		)
		CommonTags.Blocks.FENCES_NETHER_BRICK += Blocks.NETHER_BRICK_FENCE
		CommonTags.Blocks.FENCES_WOODEN += BlockTags.WOODEN_FENCES
		CommonTags.Blocks.GLASS_BLOCKS += listOf(
			CommonTags.Blocks.GLASS_BLOCKS_COLORLESS,
			CommonTags.Blocks.GLASS_BLOCKS_STAINED,
			CommonTags.Blocks.GLASS_BLOCKS_TINTED
		)
		CommonTags.Blocks.GLASS_BLOCKS_COLORLESS += Blocks.GLASS
		CommonTags.Blocks.GLASS_BLOCKS_CHEAP += listOf(
			CommonTags.Blocks.GLASS_BLOCKS_COLORLESS,
			CommonTags.Blocks.GLASS_BLOCKS_STAINED
		)
		CommonTags.Blocks.GLASS_BLOCKS_TINTED += Blocks.TINTED_GLASS
		CommonTags.Blocks.GLASS_PANES += listOf(
			CommonTags.Blocks.GLASS_PANES_COLORLESS,
			CommonTags.Blocks.GLASS_PANES_STAINED
		)
		CommonTags.Blocks.GLASS_PANES_COLORLESS += Blocks.GLASS_PANE
		addColoredFlat(CommonTags.Blocks.GLASS_BLOCKS_STAINED, "{color}_stained_glass")
		addColoredFlat(CommonTags.Blocks.GLASS_PANES_STAINED, "{color}_stained_glass_pane")
		addColored(CommonTags.Blocks.DYED, "{color}_banner")
		addColored(CommonTags.Blocks.DYED, "{color}_bed")
		addColored(CommonTags.Blocks.DYED, "{color}_candle")
		addColored(CommonTags.Blocks.DYED, "{color}_carpet")
		addColored(CommonTags.Blocks.DYED, "{color}_concrete")
		addColored(CommonTags.Blocks.DYED, "{color}_concrete_powder")
		addColored(CommonTags.Blocks.DYED, "{color}_glazed_terracotta")
		addColored(CommonTags.Blocks.DYED, "{color}_shulker_box")
		addColored(CommonTags.Blocks.DYED, "{color}_stained_glass")
		addColored(CommonTags.Blocks.DYED, "{color}_stained_glass_pane")
		addColored(CommonTags.Blocks.DYED, "{color}_terracotta")
		addColored(CommonTags.Blocks.DYED, "{color}_wall_banner")
		addColored(CommonTags.Blocks.DYED, "{color}_wool")
		CommonTags.Blocks.HIDDEN_FROM_RECIPE_VIEWERS()
		CommonTags.Blocks.GRAVELS += Blocks.GRAVEL
		CommonTags.Blocks.SKULLS += listOf(
			Blocks.SKELETON_SKULL,
			Blocks.SKELETON_WALL_SKULL,
			Blocks.WITHER_SKELETON_SKULL,
			Blocks.WITHER_SKELETON_WALL_SKULL,
			Blocks.PLAYER_HEAD,
			Blocks.PLAYER_WALL_HEAD,
			Blocks.ZOMBIE_HEAD,
			Blocks.ZOMBIE_WALL_HEAD,
			Blocks.CREEPER_HEAD,
			Blocks.CREEPER_WALL_HEAD,
			Blocks.PIGLIN_HEAD,
			Blocks.PIGLIN_WALL_HEAD,
			Blocks.DRAGON_HEAD,
			Blocks.DRAGON_WALL_HEAD
		)

		CommonTags.Blocks.NETHERRACKS += Blocks.NETHERRACK
		CommonTags.Blocks.OBSIDIANS += Blocks.OBSIDIAN
		CommonTags.Blocks.ORE_BEARING_GROUND_DEEPSLATE += Blocks.DEEPSLATE
		CommonTags.Blocks.ORE_BEARING_GROUND_NETHERRACK += Blocks.NETHERRACK
		CommonTags.Blocks.ORE_BEARING_GROUND_STONE += Blocks.STONE
		CommonTags.Blocks.ORE_RATES_DENSE += listOf(
			Blocks.COPPER_ORE,
			Blocks.DEEPSLATE_COPPER_ORE,
			Blocks.DEEPSLATE_LAPIS_ORE,
			Blocks.DEEPSLATE_REDSTONE_ORE,
			Blocks.LAPIS_ORE,
			Blocks.REDSTONE_ORE
		)
		CommonTags.Blocks.ORE_RATES_SINGULAR += listOf(
			Blocks.ANCIENT_DEBRIS,
			Blocks.COAL_ORE,
			Blocks.DEEPSLATE_COAL_ORE,
			Blocks.DEEPSLATE_DIAMOND_ORE,
			Blocks.DEEPSLATE_EMERALD_ORE,
			Blocks.DEEPSLATE_GOLD_ORE,
			Blocks.DEEPSLATE_IRON_ORE,
			Blocks.DIAMOND_ORE,
			Blocks.EMERALD_ORE,
			Blocks.GOLD_ORE,
			Blocks.IRON_ORE,
			Blocks.NETHER_QUARTZ_ORE
		)
		CommonTags.Blocks.ORE_RATES_SPARSE += Blocks.NETHER_GOLD_ORE
		CommonTags.Blocks.ORES += listOf(
			CommonTags.Blocks.ORES_COAL,
			CommonTags.Blocks.ORES_COPPER,
			CommonTags.Blocks.ORES_DIAMOND,
			CommonTags.Blocks.ORES_EMERALD,
			CommonTags.Blocks.ORES_GOLD,
			CommonTags.Blocks.ORES_IRON,
			CommonTags.Blocks.ORES_LAPIS,
			CommonTags.Blocks.ORES_REDSTONE,
			CommonTags.Blocks.ORES_QUARTZ,
			CommonTags.Blocks.ORES_NETHERITE_SCRAP
		)
		CommonTags.Blocks.ORES_COAL += BlockTags.COAL_ORES
		CommonTags.Blocks.ORES_COPPER += BlockTags.COPPER_ORES
		CommonTags.Blocks.ORES_DIAMOND += BlockTags.DIAMOND_ORES
		CommonTags.Blocks.ORES_EMERALD += BlockTags.EMERALD_ORES
		CommonTags.Blocks.ORES_GOLD += BlockTags.GOLD_ORES
		CommonTags.Blocks.ORES_IRON += BlockTags.IRON_ORES
		CommonTags.Blocks.ORES_LAPIS += BlockTags.LAPIS_ORES
		CommonTags.Blocks.ORES_QUARTZ += Blocks.NETHER_QUARTZ_ORE
		CommonTags.Blocks.ORES_REDSTONE += BlockTags.REDSTONE_ORES
		CommonTags.Blocks.ORES_NETHERITE_SCRAP += Blocks.ANCIENT_DEBRIS
		CommonTags.Blocks.ORES_IN_GROUND_DEEPSLATE += listOf(
			Blocks.DEEPSLATE_COAL_ORE,
			Blocks.DEEPSLATE_COPPER_ORE,
			Blocks.DEEPSLATE_DIAMOND_ORE,
			Blocks.DEEPSLATE_EMERALD_ORE,
			Blocks.DEEPSLATE_GOLD_ORE,
			Blocks.DEEPSLATE_IRON_ORE,
			Blocks.DEEPSLATE_LAPIS_ORE,
			Blocks.DEEPSLATE_REDSTONE_ORE
		)
		CommonTags.Blocks.ORES_IN_GROUND_NETHERRACK += listOf(
			Blocks.NETHER_GOLD_ORE,
			Blocks.NETHER_QUARTZ_ORE
		)
		CommonTags.Blocks.ORES_IN_GROUND_STONE += listOf(
			Blocks.COAL_ORE,
			Blocks.COPPER_ORE,
			Blocks.DIAMOND_ORE,
			Blocks.EMERALD_ORE,
			Blocks.GOLD_ORE,
			Blocks.IRON_ORE,
			Blocks.LAPIS_ORE,
			Blocks.REDSTONE_ORE
		)
		CommonTags.Blocks.PLAYER_WORKSTATIONS_CRAFTING_TABLES += Blocks.CRAFTING_TABLE
		CommonTags.Blocks.PLAYER_WORKSTATIONS_FURNACES += Blocks.FURNACE
		CommonTags.Blocks.RELOCATION_NOT_SUPPORTED()
		CommonTags.Blocks.ROPES()
		CommonTags.Blocks.SANDS += listOf(
			CommonTags.Blocks.SANDS_COLORLESS,
			CommonTags.Blocks.SANDS_RED
		)
		CommonTags.Blocks.SANDS_COLORLESS += Blocks.SAND
		CommonTags.Blocks.SANDS_RED += Blocks.RED_SAND
		CommonTags.Blocks.SANDSTONE_RED_BLOCKS += listOf(
			Blocks.RED_SANDSTONE,
			Blocks.CUT_RED_SANDSTONE,
			Blocks.CHISELED_RED_SANDSTONE,
			Blocks.SMOOTH_RED_SANDSTONE
		)
		CommonTags.Blocks.SANDSTONE_UNCOLORED_BLOCKS += listOf(
			Blocks.SANDSTONE,
			Blocks.CUT_SANDSTONE,
			Blocks.CHISELED_SANDSTONE,
			Blocks.SMOOTH_SANDSTONE
		)
		CommonTags.Blocks.SANDSTONE_BLOCKS += listOf(
			CommonTags.Blocks.SANDSTONE_RED_BLOCKS,
			CommonTags.Blocks.SANDSTONE_UNCOLORED_BLOCKS
		)
		CommonTags.Blocks.SANDSTONE_RED_SLABS += listOf(
			Blocks.RED_SANDSTONE_SLAB,
			Blocks.CUT_RED_SANDSTONE_SLAB,
			Blocks.SMOOTH_RED_SANDSTONE_SLAB
		)
		CommonTags.Blocks.SANDSTONE_UNCOLORED_SLABS += listOf(
			Blocks.SANDSTONE_SLAB,
			Blocks.CUT_SANDSTONE_SLAB,
			Blocks.SMOOTH_SANDSTONE_SLAB
		)
		CommonTags.Blocks.SANDSTONE_SLABS += listOf(
			CommonTags.Blocks.SANDSTONE_RED_SLABS,
			CommonTags.Blocks.SANDSTONE_UNCOLORED_SLABS
		)
		CommonTags.Blocks.SANDSTONE_RED_STAIRS += listOf(
			Blocks.RED_SANDSTONE_STAIRS,
			Blocks.SMOOTH_RED_SANDSTONE_STAIRS
		)
		CommonTags.Blocks.SANDSTONE_UNCOLORED_STAIRS += listOf(
			Blocks.SANDSTONE_STAIRS,
			Blocks.SMOOTH_SANDSTONE_STAIRS
		)
		CommonTags.Blocks.SANDSTONE_STAIRS += listOf(
			CommonTags.Blocks.SANDSTONE_RED_STAIRS,
			CommonTags.Blocks.SANDSTONE_UNCOLORED_STAIRS
		)
		CommonTags.Blocks.STONES += listOf(
			Blocks.ANDESITE,
			Blocks.DIORITE,
			Blocks.GRANITE,
			Blocks.STONE,
			Blocks.DEEPSLATE,
			Blocks.TUFF
		)
		CommonTags.Blocks.STORAGE_BLOCKS += listOf(
			CommonTags.Blocks.STORAGE_BLOCKS_BONE_MEAL,
			CommonTags.Blocks.STORAGE_BLOCKS_AMETHYST,
			CommonTags.Blocks.STORAGE_BLOCKS_COAL,
			CommonTags.Blocks.STORAGE_BLOCKS_COPPER,
			CommonTags.Blocks.STORAGE_BLOCKS_DIAMOND,
			CommonTags.Blocks.STORAGE_BLOCKS_DRIED_KELP,
			CommonTags.Blocks.STORAGE_BLOCKS_EMERALD,
			CommonTags.Blocks.STORAGE_BLOCKS_GOLD,
			CommonTags.Blocks.STORAGE_BLOCKS_IRON,
			CommonTags.Blocks.STORAGE_BLOCKS_LAPIS,
			CommonTags.Blocks.STORAGE_BLOCKS_QUARTZ,
			CommonTags.Blocks.STORAGE_BLOCKS_RAW_COPPER,
			CommonTags.Blocks.STORAGE_BLOCKS_RAW_GOLD,
			CommonTags.Blocks.STORAGE_BLOCKS_RAW_IRON,
			CommonTags.Blocks.STORAGE_BLOCKS_REDSTONE,
			CommonTags.Blocks.STORAGE_BLOCKS_NETHERITE,
			CommonTags.Blocks.STORAGE_BLOCKS_SLIME,
			CommonTags.Blocks.STORAGE_BLOCKS_WHEAT
		)
		CommonTags.Blocks.STORAGE_BLOCKS_BONE_MEAL += Blocks.BONE_BLOCK
		CommonTags.Blocks.STORAGE_BLOCKS_AMETHYST += Blocks.AMETHYST_BLOCK
		CommonTags.Blocks.STORAGE_BLOCKS_COAL += Blocks.COAL_BLOCK
		CommonTags.Blocks.STORAGE_BLOCKS_COPPER += Blocks.COPPER_BLOCK
		CommonTags.Blocks.STORAGE_BLOCKS_DIAMOND += Blocks.DIAMOND_BLOCK
		CommonTags.Blocks.STORAGE_BLOCKS_DRIED_KELP += Blocks.DRIED_KELP_BLOCK
		CommonTags.Blocks.STORAGE_BLOCKS_EMERALD += Blocks.EMERALD_BLOCK
		CommonTags.Blocks.STORAGE_BLOCKS_GOLD += Blocks.GOLD_BLOCK
		CommonTags.Blocks.STORAGE_BLOCKS_IRON += Blocks.IRON_BLOCK
		CommonTags.Blocks.STORAGE_BLOCKS_LAPIS += Blocks.LAPIS_BLOCK
		CommonTags.Blocks.STORAGE_BLOCKS_QUARTZ += Blocks.QUARTZ_BLOCK
		CommonTags.Blocks.STORAGE_BLOCKS_RAW_COPPER += Blocks.RAW_COPPER_BLOCK
		CommonTags.Blocks.STORAGE_BLOCKS_RAW_GOLD += Blocks.RAW_GOLD_BLOCK
		CommonTags.Blocks.STORAGE_BLOCKS_RAW_IRON += Blocks.RAW_IRON_BLOCK
		CommonTags.Blocks.STORAGE_BLOCKS_REDSTONE += Blocks.REDSTONE_BLOCK
		CommonTags.Blocks.STORAGE_BLOCKS_NETHERITE += Blocks.NETHERITE_BLOCK
		CommonTags.Blocks.STORAGE_BLOCKS_SLIME += Blocks.SLIME_BLOCK
		CommonTags.Blocks.STORAGE_BLOCKS_WHEAT += Blocks.HAY_BLOCK
		CommonTags.Blocks.VILLAGER_JOB_SITES += listOf(
			Blocks.BARREL, Blocks.BLAST_FURNACE, Blocks.BREWING_STAND, Blocks.CARTOGRAPHY_TABLE,
			Blocks.CAULDRON, Blocks.WATER_CAULDRON, Blocks.LAVA_CAULDRON, Blocks.POWDER_SNOW_CAULDRON,
			Blocks.COMPOSTER, Blocks.FLETCHING_TABLE, Blocks.GRINDSTONE, Blocks.LECTERN,
			Blocks.LOOM, Blocks.SMITHING_TABLE, Blocks.SMOKER, Blocks.STONECUTTER
		)
	}

	private fun addColored(group: TagKey<Block>, pattern: String, consumer: Consumer<Block> = Consumer {})
	{
		val prefix = group.location().path.lowercase() + '/'
		for (color in DyeColor.entries)
		{
			val key = ResourceLocation("minecraft", pattern.replace("{color}", color.getName().lowercase()))
			val tag = getCommonTag(prefix + color.getName().lowercase())
			val block = BuiltInRegistries.BLOCK[key]
			check(block !== Blocks.AIR) { "Unknown vanilla block: $key" }
			tag += block
			consumer.accept(block)
		}
	}

	private fun addColoredFlat(tag: TagKey<Block>, pattern: String, consumer: Consumer<Block> = Consumer {})
	{
		for (color in DyeColor.entries)
		{
			val key = ResourceLocation("minecraft", pattern.replace("{color}", color.getName().lowercase()))
			val block = BuiltInRegistries.BLOCK[key]
			check(block !== Blocks.AIR) { "Unknown vanilla block: $key" }
			tag += block
			consumer.accept(block)
		}
	}

	private fun getCommonTag(name: String): TagKey<Block>
	{
		return CommonTags.Blocks[ResourceLocation("c", name)]
			?: throw IllegalStateException(CommonTags.Blocks::class.java.getName() + " is missing tag name: " + name)
	}
}