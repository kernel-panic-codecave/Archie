package com.withertech.archie.data.internal.common.tags

import com.withertech.archie.Archie
import com.withertech.archie.data.common.tags.ATagsProvider
import com.withertech.archie.data.common.tags.ACommonTags
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

class AInternalBlockTagsProvider(
	output: PackOutput,
	lookupProvider: CompletableFuture<HolderLookup.Provider>
) :
	ATagsProvider.BlockTagsProvider(
		output, Archie.MOD,
		lookupProvider,
		false
	)
{
	override fun generate(registries: HolderLookup.Provider)
	{
		if (Platform.isNeoForge())
		{
			ACommonTags.Blocks.ENDERMAN_PLACE_ON_BLACKLIST()
		}
		ACommonTags.Blocks.BARRELS += ACommonTags.Blocks.BARRELS_WOODEN
		ACommonTags.Blocks.BARRELS_WOODEN += Blocks.BARREL
		ACommonTags.Blocks.BOOKSHELVES += Blocks.BOOKSHELF
		ACommonTags.Blocks.BUDDING_BLOCKS += Blocks.BUDDING_AMETHYST
		ACommonTags.Blocks.BUDS += listOf(
			Blocks.SMALL_AMETHYST_BUD,
			Blocks.MEDIUM_AMETHYST_BUD,
			Blocks.LARGE_AMETHYST_BUD
		)
		ACommonTags.Blocks.CHAINS += Blocks.CHAIN
		ACommonTags.Blocks.CHESTS += listOf(
			ACommonTags.Blocks.CHESTS_ENDER,
			ACommonTags.Blocks.CHESTS_TRAPPED,
			ACommonTags.Blocks.CHESTS_WOODEN
		)
		ACommonTags.Blocks.CHESTS_ENDER += Blocks.ENDER_CHEST
		ACommonTags.Blocks.CHESTS_TRAPPED += Blocks.TRAPPED_CHEST
		ACommonTags.Blocks.CHESTS_WOODEN += listOf(
			Blocks.CHEST,
			Blocks.TRAPPED_CHEST
		)
		ACommonTags.Blocks.CLUSTERS += Blocks.AMETHYST_CLUSTER
		ACommonTags.Blocks.SHULKER_BOXES += listOf(
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
		ACommonTags.Blocks.COBBLESTONES += listOf(
			ACommonTags.Blocks.COBBLESTONES_NORMAL,
			ACommonTags.Blocks.COBBLESTONES_INFESTED,
			ACommonTags.Blocks.COBBLESTONES_MOSSY,
			ACommonTags.Blocks.COBBLESTONES_DEEPSLATE
		)
		ACommonTags.Blocks.COBBLESTONES_NORMAL += Blocks.COBBLESTONE
		ACommonTags.Blocks.COBBLESTONES_INFESTED += Blocks.INFESTED_COBBLESTONE
		ACommonTags.Blocks.COBBLESTONES_MOSSY += Blocks.MOSSY_COBBLESTONE
		ACommonTags.Blocks.COBBLESTONES_DEEPSLATE += Blocks.COBBLED_DEEPSLATE
		ACommonTags.Blocks.END_STONES += Blocks.END_STONE
		ACommonTags.Blocks.FENCE_GATES += ACommonTags.Blocks.FENCE_GATES_WOODEN
		ACommonTags.Blocks.FENCE_GATES_WOODEN += listOf(
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
		ACommonTags.Blocks.FENCES += listOf(
			ACommonTags.Blocks.FENCES_NETHER_BRICK,
			ACommonTags.Blocks.FENCES_WOODEN
		)
		ACommonTags.Blocks.FENCES_NETHER_BRICK += Blocks.NETHER_BRICK_FENCE
		ACommonTags.Blocks.FENCES_WOODEN += BlockTags.WOODEN_FENCES
		ACommonTags.Blocks.GLASS_BLOCKS += listOf(
			ACommonTags.Blocks.GLASS_BLOCKS_COLORLESS,
			ACommonTags.Blocks.GLASS_BLOCKS_STAINED,
			ACommonTags.Blocks.GLASS_BLOCKS_TINTED
		)
		ACommonTags.Blocks.GLASS_BLOCKS_COLORLESS += Blocks.GLASS
		ACommonTags.Blocks.GLASS_BLOCKS_CHEAP += listOf(
			ACommonTags.Blocks.GLASS_BLOCKS_COLORLESS,
			ACommonTags.Blocks.GLASS_BLOCKS_STAINED
		)
		ACommonTags.Blocks.GLASS_BLOCKS_TINTED += Blocks.TINTED_GLASS
		ACommonTags.Blocks.GLASS_PANES += listOf(
			ACommonTags.Blocks.GLASS_PANES_COLORLESS,
			ACommonTags.Blocks.GLASS_PANES_STAINED
		)
		ACommonTags.Blocks.GLASS_PANES_COLORLESS += Blocks.GLASS_PANE
		addColoredFlat(ACommonTags.Blocks.GLASS_BLOCKS_STAINED, "{color}_stained_glass")
		addColoredFlat(ACommonTags.Blocks.GLASS_PANES_STAINED, "{color}_stained_glass_pane")
		addColored(ACommonTags.Blocks.DYED, "{color}_banner")
		addColored(ACommonTags.Blocks.DYED, "{color}_bed")
		addColored(ACommonTags.Blocks.DYED, "{color}_candle")
		addColored(ACommonTags.Blocks.DYED, "{color}_carpet")
		addColored(ACommonTags.Blocks.DYED, "{color}_concrete")
		addColored(ACommonTags.Blocks.DYED, "{color}_concrete_powder")
		addColored(ACommonTags.Blocks.DYED, "{color}_glazed_terracotta")
		addColored(ACommonTags.Blocks.DYED, "{color}_shulker_box")
		addColored(ACommonTags.Blocks.DYED, "{color}_stained_glass")
		addColored(ACommonTags.Blocks.DYED, "{color}_stained_glass_pane")
		addColored(ACommonTags.Blocks.DYED, "{color}_terracotta")
		addColored(ACommonTags.Blocks.DYED, "{color}_wall_banner")
		addColored(ACommonTags.Blocks.DYED, "{color}_wool")
		ACommonTags.Blocks.HIDDEN_FROM_RECIPE_VIEWERS()
		ACommonTags.Blocks.GRAVELS += Blocks.GRAVEL
		ACommonTags.Blocks.SKULLS += listOf(
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

		ACommonTags.Blocks.NETHERRACKS += Blocks.NETHERRACK
		ACommonTags.Blocks.OBSIDIANS += Blocks.OBSIDIAN
		ACommonTags.Blocks.ORE_BEARING_GROUND_DEEPSLATE += Blocks.DEEPSLATE
		ACommonTags.Blocks.ORE_BEARING_GROUND_NETHERRACK += Blocks.NETHERRACK
		ACommonTags.Blocks.ORE_BEARING_GROUND_STONE += Blocks.STONE
		ACommonTags.Blocks.ORE_RATES_DENSE += listOf(
			Blocks.COPPER_ORE,
			Blocks.DEEPSLATE_COPPER_ORE,
			Blocks.DEEPSLATE_LAPIS_ORE,
			Blocks.DEEPSLATE_REDSTONE_ORE,
			Blocks.LAPIS_ORE,
			Blocks.REDSTONE_ORE
		)
		ACommonTags.Blocks.ORE_RATES_SINGULAR += listOf(
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
		ACommonTags.Blocks.ORE_RATES_SPARSE += Blocks.NETHER_GOLD_ORE
		ACommonTags.Blocks.ORES += listOf(
			ACommonTags.Blocks.ORES_COAL,
			ACommonTags.Blocks.ORES_COPPER,
			ACommonTags.Blocks.ORES_DIAMOND,
			ACommonTags.Blocks.ORES_EMERALD,
			ACommonTags.Blocks.ORES_GOLD,
			ACommonTags.Blocks.ORES_IRON,
			ACommonTags.Blocks.ORES_LAPIS,
			ACommonTags.Blocks.ORES_REDSTONE,
			ACommonTags.Blocks.ORES_QUARTZ,
			ACommonTags.Blocks.ORES_NETHERITE_SCRAP
		)
		ACommonTags.Blocks.ORES_COAL += BlockTags.COAL_ORES
		ACommonTags.Blocks.ORES_COPPER += BlockTags.COPPER_ORES
		ACommonTags.Blocks.ORES_DIAMOND += BlockTags.DIAMOND_ORES
		ACommonTags.Blocks.ORES_EMERALD += BlockTags.EMERALD_ORES
		ACommonTags.Blocks.ORES_GOLD += BlockTags.GOLD_ORES
		ACommonTags.Blocks.ORES_IRON += BlockTags.IRON_ORES
		ACommonTags.Blocks.ORES_LAPIS += BlockTags.LAPIS_ORES
		ACommonTags.Blocks.ORES_QUARTZ += Blocks.NETHER_QUARTZ_ORE
		ACommonTags.Blocks.ORES_REDSTONE += BlockTags.REDSTONE_ORES
		ACommonTags.Blocks.ORES_NETHERITE_SCRAP += Blocks.ANCIENT_DEBRIS
		ACommonTags.Blocks.ORES_IN_GROUND_DEEPSLATE += listOf(
			Blocks.DEEPSLATE_COAL_ORE,
			Blocks.DEEPSLATE_COPPER_ORE,
			Blocks.DEEPSLATE_DIAMOND_ORE,
			Blocks.DEEPSLATE_EMERALD_ORE,
			Blocks.DEEPSLATE_GOLD_ORE,
			Blocks.DEEPSLATE_IRON_ORE,
			Blocks.DEEPSLATE_LAPIS_ORE,
			Blocks.DEEPSLATE_REDSTONE_ORE
		)
		ACommonTags.Blocks.ORES_IN_GROUND_NETHERRACK += listOf(
			Blocks.NETHER_GOLD_ORE,
			Blocks.NETHER_QUARTZ_ORE
		)
		ACommonTags.Blocks.ORES_IN_GROUND_STONE += listOf(
			Blocks.COAL_ORE,
			Blocks.COPPER_ORE,
			Blocks.DIAMOND_ORE,
			Blocks.EMERALD_ORE,
			Blocks.GOLD_ORE,
			Blocks.IRON_ORE,
			Blocks.LAPIS_ORE,
			Blocks.REDSTONE_ORE
		)
		ACommonTags.Blocks.PLAYER_WORKSTATIONS_CRAFTING_TABLES += Blocks.CRAFTING_TABLE
		ACommonTags.Blocks.PLAYER_WORKSTATIONS_FURNACES += Blocks.FURNACE
		ACommonTags.Blocks.RELOCATION_NOT_SUPPORTED()
		ACommonTags.Blocks.ROPES()
		ACommonTags.Blocks.SANDS += listOf(
			ACommonTags.Blocks.SANDS_COLORLESS,
			ACommonTags.Blocks.SANDS_RED
		)
		ACommonTags.Blocks.SANDS_COLORLESS += Blocks.SAND
		ACommonTags.Blocks.SANDS_RED += Blocks.RED_SAND
		ACommonTags.Blocks.SANDSTONE_RED_BLOCKS += listOf(
			Blocks.RED_SANDSTONE,
			Blocks.CUT_RED_SANDSTONE,
			Blocks.CHISELED_RED_SANDSTONE,
			Blocks.SMOOTH_RED_SANDSTONE
		)
		ACommonTags.Blocks.SANDSTONE_UNCOLORED_BLOCKS += listOf(
			Blocks.SANDSTONE,
			Blocks.CUT_SANDSTONE,
			Blocks.CHISELED_SANDSTONE,
			Blocks.SMOOTH_SANDSTONE
		)
		ACommonTags.Blocks.SANDSTONE_BLOCKS += listOf(
			ACommonTags.Blocks.SANDSTONE_RED_BLOCKS,
			ACommonTags.Blocks.SANDSTONE_UNCOLORED_BLOCKS
		)
		ACommonTags.Blocks.SANDSTONE_RED_SLABS += listOf(
			Blocks.RED_SANDSTONE_SLAB,
			Blocks.CUT_RED_SANDSTONE_SLAB,
			Blocks.SMOOTH_RED_SANDSTONE_SLAB
		)
		ACommonTags.Blocks.SANDSTONE_UNCOLORED_SLABS += listOf(
			Blocks.SANDSTONE_SLAB,
			Blocks.CUT_SANDSTONE_SLAB,
			Blocks.SMOOTH_SANDSTONE_SLAB
		)
		ACommonTags.Blocks.SANDSTONE_SLABS += listOf(
			ACommonTags.Blocks.SANDSTONE_RED_SLABS,
			ACommonTags.Blocks.SANDSTONE_UNCOLORED_SLABS
		)
		ACommonTags.Blocks.SANDSTONE_RED_STAIRS += listOf(
			Blocks.RED_SANDSTONE_STAIRS,
			Blocks.SMOOTH_RED_SANDSTONE_STAIRS
		)
		ACommonTags.Blocks.SANDSTONE_UNCOLORED_STAIRS += listOf(
			Blocks.SANDSTONE_STAIRS,
			Blocks.SMOOTH_SANDSTONE_STAIRS
		)
		ACommonTags.Blocks.SANDSTONE_STAIRS += listOf(
			ACommonTags.Blocks.SANDSTONE_RED_STAIRS,
			ACommonTags.Blocks.SANDSTONE_UNCOLORED_STAIRS
		)
		ACommonTags.Blocks.STONES += listOf(
			Blocks.ANDESITE,
			Blocks.DIORITE,
			Blocks.GRANITE,
			Blocks.STONE,
			Blocks.DEEPSLATE,
			Blocks.TUFF
		)
		ACommonTags.Blocks.STORAGE_BLOCKS += listOf(
			ACommonTags.Blocks.STORAGE_BLOCKS_BONE_MEAL,
			ACommonTags.Blocks.STORAGE_BLOCKS_AMETHYST,
			ACommonTags.Blocks.STORAGE_BLOCKS_COAL,
			ACommonTags.Blocks.STORAGE_BLOCKS_COPPER,
			ACommonTags.Blocks.STORAGE_BLOCKS_DIAMOND,
			ACommonTags.Blocks.STORAGE_BLOCKS_DRIED_KELP,
			ACommonTags.Blocks.STORAGE_BLOCKS_EMERALD,
			ACommonTags.Blocks.STORAGE_BLOCKS_GOLD,
			ACommonTags.Blocks.STORAGE_BLOCKS_IRON,
			ACommonTags.Blocks.STORAGE_BLOCKS_LAPIS,
			ACommonTags.Blocks.STORAGE_BLOCKS_QUARTZ,
			ACommonTags.Blocks.STORAGE_BLOCKS_RAW_COPPER,
			ACommonTags.Blocks.STORAGE_BLOCKS_RAW_GOLD,
			ACommonTags.Blocks.STORAGE_BLOCKS_RAW_IRON,
			ACommonTags.Blocks.STORAGE_BLOCKS_REDSTONE,
			ACommonTags.Blocks.STORAGE_BLOCKS_NETHERITE,
			ACommonTags.Blocks.STORAGE_BLOCKS_SLIME,
			ACommonTags.Blocks.STORAGE_BLOCKS_WHEAT
		)
		ACommonTags.Blocks.STORAGE_BLOCKS_BONE_MEAL += Blocks.BONE_BLOCK
		ACommonTags.Blocks.STORAGE_BLOCKS_AMETHYST += Blocks.AMETHYST_BLOCK
		ACommonTags.Blocks.STORAGE_BLOCKS_COAL += Blocks.COAL_BLOCK
		ACommonTags.Blocks.STORAGE_BLOCKS_COPPER += Blocks.COPPER_BLOCK
		ACommonTags.Blocks.STORAGE_BLOCKS_DIAMOND += Blocks.DIAMOND_BLOCK
		ACommonTags.Blocks.STORAGE_BLOCKS_DRIED_KELP += Blocks.DRIED_KELP_BLOCK
		ACommonTags.Blocks.STORAGE_BLOCKS_EMERALD += Blocks.EMERALD_BLOCK
		ACommonTags.Blocks.STORAGE_BLOCKS_GOLD += Blocks.GOLD_BLOCK
		ACommonTags.Blocks.STORAGE_BLOCKS_IRON += Blocks.IRON_BLOCK
		ACommonTags.Blocks.STORAGE_BLOCKS_LAPIS += Blocks.LAPIS_BLOCK
		ACommonTags.Blocks.STORAGE_BLOCKS_QUARTZ += Blocks.QUARTZ_BLOCK
		ACommonTags.Blocks.STORAGE_BLOCKS_RAW_COPPER += Blocks.RAW_COPPER_BLOCK
		ACommonTags.Blocks.STORAGE_BLOCKS_RAW_GOLD += Blocks.RAW_GOLD_BLOCK
		ACommonTags.Blocks.STORAGE_BLOCKS_RAW_IRON += Blocks.RAW_IRON_BLOCK
		ACommonTags.Blocks.STORAGE_BLOCKS_REDSTONE += Blocks.REDSTONE_BLOCK
		ACommonTags.Blocks.STORAGE_BLOCKS_NETHERITE += Blocks.NETHERITE_BLOCK
		ACommonTags.Blocks.STORAGE_BLOCKS_SLIME += Blocks.SLIME_BLOCK
		ACommonTags.Blocks.STORAGE_BLOCKS_WHEAT += Blocks.HAY_BLOCK
		ACommonTags.Blocks.VILLAGER_JOB_SITES += listOf(
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
			val key = ResourceLocation.fromNamespaceAndPath("minecraft", pattern.replace("{color}", color.getName().lowercase()))
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
			val key = ResourceLocation.fromNamespaceAndPath("minecraft", pattern.replace("{color}", color.getName().lowercase()))
			val block = BuiltInRegistries.BLOCK[key]
			check(block !== Blocks.AIR) { "Unknown vanilla block: $key" }
			tag += block
			consumer.accept(block)
		}
	}

	private fun getCommonTag(name: String): TagKey<Block>
	{
		return ACommonTags.Blocks[ResourceLocation.fromNamespaceAndPath("c", name)]
			?: throw IllegalStateException(ACommonTags.Blocks::class.java.name + " is missing tag name: " + name)
	}
}