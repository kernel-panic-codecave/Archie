package com.withertech.archie.data.common.tags

import net.minecraft.core.Registry
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.ItemTags
import net.minecraft.tags.TagKey
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.DyeColor
import net.minecraft.world.item.Item
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.material.Fluid

@Suppress("unused")
object CommonTags
{
	fun init()
	{
		Blocks.init()
		EntityTypes.init()
		Items.init()
		Fluids.init()
		Biomes.init()
	}

	abstract class Tags<T> private constructor(
		private val registry: ResourceKey<out Registry<T>>,
		private val tags: MutableMap<ResourceLocation, TagKey<T>>
	) : Map<ResourceLocation, TagKey<T>> by tags
	{
		constructor(registry: ResourceKey<out Registry<T>>) : this(registry, mutableMapOf())
		protected fun tag(name: String): TagKey<T> = tag("c", name)

		protected fun tag(namespace: String, name: String): TagKey<T>
		{
			return TagKey.create(registry, ResourceLocation(namespace, name)).also { tags[it.location] = it }
		}

		protected fun existing(tag: TagKey<T>): TagKey<T>
		{
			return tag.also { tags[it.location] = it }
		}

	}

	object Blocks : Tags<Block>(Registries.BLOCK)
	{
		internal fun init()
		{
		}

		val ENDERMAN_PLACE_ON_BLACKLIST: TagKey<Block> = tag("neoforge", "enderman_place_on_blacklist")
		val NEEDS_WOOD_TOOL: TagKey<Block> = tag("neoforge", "needs_wood_tool")
		val NEEDS_GOLD_TOOL: TagKey<Block> = tag("neoforge", "needs_gold_tool")
		val NEEDS_NETHERITE_TOOL: TagKey<Block> = tag("neoforge", "needs_netherite_tool")

		// `c` tags for common conventions
		val BARRELS: TagKey<Block> = tag("barrels")
		val BARRELS_WOODEN: TagKey<Block> = tag("barrels/wooden")
		val BOOKSHELVES: TagKey<Block> = tag("bookshelves")

		/**
		 * For blocks that are similar to amethyst where their budding block produces buds and cluster blocks
		 */
		val BUDDING_BLOCKS: TagKey<Block> = tag("budding_blocks")

		/**
		 * For blocks that are similar to amethyst where they have buddings forming from budding blocks
		 */
		val BUDS: TagKey<Block> = tag("buds")
		val CHAINS: TagKey<Block> = tag("chains")
		val CHESTS: TagKey<Block> = tag("chests")
		val CHESTS_ENDER: TagKey<Block> = tag("chests/ender")
		val CHESTS_TRAPPED: TagKey<Block> = tag("chests/trapped")
		val CHESTS_WOODEN: TagKey<Block> = tag("chests/wooden")

		/**
		 * For blocks that are similar to amethyst where they have clusters forming from budding blocks
		 */
		val CLUSTERS: TagKey<Block> = tag("clusters")
		val COBBLESTONES: TagKey<Block> = tag("cobblestones")
		val COBBLESTONES_NORMAL: TagKey<Block> = tag("cobblestones/normal")
		val COBBLESTONES_INFESTED: TagKey<Block> = tag("cobblestones/infested")
		val COBBLESTONES_MOSSY: TagKey<Block> = tag("cobblestones/mossy")
		val COBBLESTONES_DEEPSLATE: TagKey<Block> = tag("cobblestones/deepslate")

		/**
		 * Tag that holds all blocks that can be dyed a specific color.
		 * (Does not include color blending blocks that would behave similar to leather armor item)
		 */
		val DYED: TagKey<Block> = tag("dyed")
		val DYED_BLACK: TagKey<Block> = tag("dyed/black")
		val DYED_BLUE: TagKey<Block> = tag("dyed/blue")
		val DYED_BROWN: TagKey<Block> = tag("dyed/brown")
		val DYED_CYAN: TagKey<Block> = tag("dyed/cyan")
		val DYED_GRAY: TagKey<Block> = tag("dyed/gray")
		val DYED_GREEN: TagKey<Block> = tag("dyed/green")
		val DYED_LIGHT_BLUE: TagKey<Block> = tag("dyed/light_blue")
		val DYED_LIGHT_GRAY: TagKey<Block> = tag("dyed/light_gray")
		val DYED_LIME: TagKey<Block> = tag("dyed/lime")
		val DYED_MAGENTA: TagKey<Block> = tag("dyed/magenta")
		val DYED_ORANGE: TagKey<Block> = tag("dyed/orange")
		val DYED_PINK: TagKey<Block> = tag("dyed/pink")
		val DYED_PURPLE: TagKey<Block> = tag("dyed/purple")
		val DYED_RED: TagKey<Block> = tag("dyed/red")
		val DYED_WHITE: TagKey<Block> = tag("dyed/white")
		val DYED_YELLOW: TagKey<Block> = tag("dyed/yellow")
		val END_STONES: TagKey<Block> = tag("end_stones")
		val FENCE_GATES: TagKey<Block> = tag("fence_gates")
		val FENCE_GATES_WOODEN: TagKey<Block> = tag("fence_gates/wooden")
		val FENCES: TagKey<Block> = tag("fences")
		val FENCES_NETHER_BRICK: TagKey<Block> = tag("fences/nether_brick")
		val FENCES_WOODEN: TagKey<Block> = tag("fences/wooden")

		val GLASS_BLOCKS: TagKey<Block> = tag("glass_blocks")
		val GLASS_BLOCKS_COLORLESS: TagKey<Block> = tag("glass_blocks/colorless")

		/**
		 * Glass which is made from cheap resources like sand and only minor additional ingredients like dyes
		 */
		val GLASS_BLOCKS_CHEAP: TagKey<Block> = tag("glass_blocks/cheap")
		val GLASS_BLOCKS_STAINED: TagKey<Block> = tag("glass_blocks/stained")
		val GLASS_BLOCKS_TINTED: TagKey<Block> = tag("glass_blocks/tinted")

		val GLASS_PANES: TagKey<Block> = tag("glass_panes")
		val GLASS_PANES_COLORLESS: TagKey<Block> = tag("glass_panes/colorless")
		val GLASS_PANES_STAINED: TagKey<Block> = tag("glass_panes/stained")

		val GRAVELS: TagKey<Block> = tag("gravels")

		/**
		 * Tag that holds all blocks that recipe viewers should not show to users.
		 * Recipe viewers may use this to automatically find the corresponding BlockItem to hide.
		 */
		val HIDDEN_FROM_RECIPE_VIEWERS: TagKey<Block> = tag("hidden_from_recipe_viewers")
		val NETHERRACKS: TagKey<Block> = tag("netherrack")
		val OBSIDIANS: TagKey<Block> = tag("obsidians")

		/**
		 * Blocks which are often replaced by deepslate ores, i.e. the ores in the tag [.ORES_IN_GROUND_DEEPSLATE], during world generation
		 */
		val ORE_BEARING_GROUND_DEEPSLATE: TagKey<Block> = tag("ore_bearing_ground/deepslate")

		/**
		 * Blocks which are often replaced by netherrack ores, i.e. the ores in the tag [.ORES_IN_GROUND_NETHERRACK], during world generation
		 */
		val ORE_BEARING_GROUND_NETHERRACK: TagKey<Block> = tag("ore_bearing_ground/netherrack")

		/**
		 * Blocks which are often replaced by stone ores, i.e. the ores in the tag [.ORES_IN_GROUND_STONE], during world generation
		 */
		val ORE_BEARING_GROUND_STONE: TagKey<Block> = tag("ore_bearing_ground/stone")

		/**
		 * Ores which on average result in more than one resource worth of materials
		 */
		val ORE_RATES_DENSE: TagKey<Block> = tag("ore_rates/dense")

		/**
		 * Ores which on average result in one resource worth of materials
		 */
		val ORE_RATES_SINGULAR: TagKey<Block> = tag("ore_rates/singular")

		/**
		 * Ores which on average result in less than one resource worth of materials
		 */
		val ORE_RATES_SPARSE: TagKey<Block> = tag("ore_rates/sparse")
		val ORES: TagKey<Block> = tag("ores")
		val ORES_COAL: TagKey<Block> = tag("ores/coal")
		val ORES_COPPER: TagKey<Block> = tag("ores/copper")
		val ORES_DIAMOND: TagKey<Block> = tag("ores/diamond")
		val ORES_EMERALD: TagKey<Block> = tag("ores/emerald")
		val ORES_GOLD: TagKey<Block> = tag("ores/gold")
		val ORES_IRON: TagKey<Block> = tag("ores/iron")
		val ORES_LAPIS: TagKey<Block> = tag("ores/lapis")
		val ORES_NETHERITE_SCRAP: TagKey<Block> = tag("ores/netherite_scrap")
		val ORES_QUARTZ: TagKey<Block> = tag("ores/quartz")
		val ORES_REDSTONE: TagKey<Block> = tag("ores/redstone")

		/**
		 * Ores in deepslate (or in equivalent blocks in the tag [.ORE_BEARING_GROUND_DEEPSLATE]) which could logically use deepslate as recipe input or output
		 */
		val ORES_IN_GROUND_DEEPSLATE: TagKey<Block> = tag("ores_in_ground/deepslate")

		/**
		 * Ores in netherrack (or in equivalent blocks in the tag [.ORE_BEARING_GROUND_NETHERRACK]) which could logically use netherrack as recipe input or output
		 */
		val ORES_IN_GROUND_NETHERRACK: TagKey<Block> = tag("ores_in_ground/netherrack")

		/**
		 * Ores in stone (or in equivalent blocks in the tag [.ORE_BEARING_GROUND_STONE]) which could logically use stone as recipe input or output
		 */
		val ORES_IN_GROUND_STONE: TagKey<Block> = tag("ores_in_ground/stone")
		val PLAYER_WORKSTATIONS_CRAFTING_TABLES: TagKey<Block> = tag("player_workstations/crafting_tables")
		val PLAYER_WORKSTATIONS_FURNACES: TagKey<Block> = tag("player_workstations/furnaces")

		/**
		 * Blocks should be included in this tag if their movement/relocation can cause serious issues such
		 * as world corruption upon being moved or for balance reason where the block should not be able to be relocated.
		 * Example: Chunk loaders or pipes where other mods that move blocks do not respect
		 * [BlockBehaviour.BlockStateBase.getPistonPushReaction].
		 */
		val RELOCATION_NOT_SUPPORTED: TagKey<Block> = tag("relocation_not_supported")
		val ROPES: TagKey<Block> = tag("ropes")

		val SANDS: TagKey<Block> = tag("sands")
		val SANDS_COLORLESS: TagKey<Block> = tag("sands/colorless")
		val SANDS_RED: TagKey<Block> = tag("sands/red")

		val SANDSTONE_BLOCKS: TagKey<Block> = tag("sandstone/blocks")
		val SANDSTONE_SLABS: TagKey<Block> = tag("sandstone/slabs")
		val SANDSTONE_STAIRS: TagKey<Block> = tag("sandstone/stairs")
		val SANDSTONE_RED_BLOCKS: TagKey<Block> = tag("sandstone/red_blocks")
		val SANDSTONE_RED_SLABS: TagKey<Block> = tag("sandstone/red_slabs")
		val SANDSTONE_RED_STAIRS: TagKey<Block> = tag("sandstone/red_stairs")
		val SANDSTONE_UNCOLORED_BLOCKS: TagKey<Block> = tag("sandstone/uncolored_blocks")
		val SANDSTONE_UNCOLORED_SLABS: TagKey<Block> = tag("sandstone/uncolored_slabs")
		val SANDSTONE_UNCOLORED_STAIRS: TagKey<Block> = tag("sandstone/uncolored_stairs")

		val SHULKER_BOXES: TagKey<Block> = tag("shulker_boxes")

		/**
		 * Tag that holds all head based blocks such as Skeleton Skull or Player Head. (Named skulls to match minecraft:skulls item tag)
		 */
		val SKULLS: TagKey<Block> = tag("skulls")

		/**
		 * Natural stone-like blocks that can be used as a base ingredient in recipes that takes stone.
		 */
		val STONES: TagKey<Block> = tag("stones")

		/**
		 * A storage block is generally a block that has a recipe to craft a bulk of 1 kind of resource to a block
		 * and has a mirror recipe to reverse the crafting with no loss in resources.
		 *
		 *
		 * Honey Block is special in that the reversing recipe is not a perfect mirror of the crafting recipe
		 * and so, it is considered a special case and not given a storage block tag.
		 */
		val STORAGE_BLOCKS: TagKey<Block> = tag("storage_blocks")
		val STORAGE_BLOCKS_AMETHYST: TagKey<Block> = tag("storage_blocks/amethyst")
		val STORAGE_BLOCKS_BONE_MEAL: TagKey<Block> = tag("storage_blocks/bone_meal")
		val STORAGE_BLOCKS_COAL: TagKey<Block> = tag("storage_blocks/coal")
		val STORAGE_BLOCKS_COPPER: TagKey<Block> = tag("storage_blocks/copper")
		val STORAGE_BLOCKS_DIAMOND: TagKey<Block> = tag("storage_blocks/diamond")
		val STORAGE_BLOCKS_DRIED_KELP: TagKey<Block> = tag("storage_blocks/dried_kelp")
		val STORAGE_BLOCKS_EMERALD: TagKey<Block> = tag("storage_blocks/emerald")
		val STORAGE_BLOCKS_GOLD: TagKey<Block> = tag("storage_blocks/gold")
		val STORAGE_BLOCKS_IRON: TagKey<Block> = tag("storage_blocks/iron")
		val STORAGE_BLOCKS_LAPIS: TagKey<Block> = tag("storage_blocks/lapis")
		val STORAGE_BLOCKS_NETHERITE: TagKey<Block> = tag("storage_blocks/netherite")
		val STORAGE_BLOCKS_QUARTZ: TagKey<Block> = tag("storage_blocks/quartz")
		val STORAGE_BLOCKS_RAW_COPPER: TagKey<Block> = tag("storage_blocks/raw_copper")
		val STORAGE_BLOCKS_RAW_GOLD: TagKey<Block> = tag("storage_blocks/raw_gold")
		val STORAGE_BLOCKS_RAW_IRON: TagKey<Block> = tag("storage_blocks/raw_iron")
		val STORAGE_BLOCKS_REDSTONE: TagKey<Block> = tag("storage_blocks/redstone")
		val STORAGE_BLOCKS_SLIME: TagKey<Block> = tag("storage_blocks/slime")
		val STORAGE_BLOCKS_WHEAT: TagKey<Block> = tag("storage_blocks/wheat")
		val VILLAGER_JOB_SITES: TagKey<Block> = tag("villager_job_sites")
	}

	object EntityTypes : Tags<EntityType<*>>(Registries.ENTITY_TYPE)
	{
		internal fun init()
		{
		}

		val BOSSES: TagKey<EntityType<*>> = tag("bosses")
		val MINECARTS: TagKey<EntityType<*>> = tag("minecarts")
		val BOATS: TagKey<EntityType<*>> = tag("boats")

		/**
		 * Entities should be included in this tag if they are not allowed to be picked up by items or grabbed in a way
		 * that a player can easily move the entity to anywhere they want. Ideal for special entities that should not
		 * be able to be put into a mob jar for example.
		 */
		val CAPTURING_NOT_SUPPORTED: TagKey<EntityType<*>> = tag("capturing_not_supported")

		/**
		 * Entities should be included in this tag if they are not allowed to be teleported in any way.
		 * This is more for mods that allow teleporting entities within the same dimension. Any mod that is
		 * teleporting entities to new dimensions should be checking canChangeDimensions method on the entity itself.
		 */
		val TELEPORTING_NOT_SUPPORTED: TagKey<EntityType<*>> = tag("teleporting_not_supported")
	}

	object Items : Tags<Item>(Registries.ITEM)
	{
		internal fun init()
		{
		}


		/**
		 * Controls what items can be consumed for enchanting such as Enchanting Tables.
		 * This tag defaults to [net.minecraft.world.item.Items.LAPIS_LAZULI] when not present in any datapacks, including forge client on vanilla server
		 */
		val ENCHANTING_FUELS: TagKey<Item> = tag("neoforge", "enchanting_fuels")


		// `c` tags for common conventions
		val BARRELS: TagKey<Item> = tag("barrels")
		val BARRELS_WOODEN: TagKey<Item> = tag("barrels/wooden")
		val BONES: TagKey<Item> = tag("bones")
		val BOOKSHELVES: TagKey<Item> = tag("bookshelves")
		val BRICKS: TagKey<Item> = tag("bricks")
		val BRICKS_NORMAL: TagKey<Item> = tag("bricks/normal")
		val BRICKS_NETHER: TagKey<Item> = tag("bricks/nether")
		val BUCKETS: TagKey<Item> = tag("buckets")
		val BUCKETS_EMPTY: TagKey<Item> = tag("buckets/empty")

		/**
		 * Does not include entity water buckets.
		 * If checking for the fluid this bucket holds in code, please use [net.neoforged.neoforge.fluids.capability.wrappers.FluidBucketWrapper.getFluid] instead.
		 */
		val BUCKETS_WATER: TagKey<Item> = tag("buckets/water")

		/**
		 * If checking for the fluid this bucket holds in code, please use [net.neoforged.neoforge.fluids.capability.wrappers.FluidBucketWrapper.getFluid] instead.
		 */
		val BUCKETS_LAVA: TagKey<Item> = tag("buckets/lava")
		val BUCKETS_MILK: TagKey<Item> = tag("buckets/milk")
		val BUCKETS_POWDER_SNOW: TagKey<Item> = tag("buckets/powder_snow")
		val BUCKETS_ENTITY_WATER: TagKey<Item> = tag("buckets/entity_water")

		/**
		 * For blocks that are similar to amethyst where their budding block produces buds and cluster blocks
		 */
		val BUDDING_BLOCKS: TagKey<Item> = tag("budding_blocks")

		/**
		 * For blocks that are similar to amethyst where they have buddings forming from budding blocks
		 */
		val BUDS: TagKey<Item> = tag("buds")
		val CHAINS: TagKey<Item> = tag("chains")
		val CHESTS: TagKey<Item> = tag("chests")
		val CHESTS_ENDER: TagKey<Item> = tag("chests/ender")
		val CHESTS_TRAPPED: TagKey<Item> = tag("chests/trapped")
		val CHESTS_WOODEN: TagKey<Item> = tag("chests/wooden")
		val COBBLESTONES: TagKey<Item> = tag("cobblestones")
		val COBBLESTONES_NORMAL: TagKey<Item> = tag("cobblestones/normal")
		val COBBLESTONES_INFESTED: TagKey<Item> = tag("cobblestones/infested")
		val COBBLESTONES_MOSSY: TagKey<Item> = tag("cobblestones/mossy")
		val COBBLESTONES_DEEPSLATE: TagKey<Item> = tag("cobblestones/deepslate")

		/**
		 * For blocks that are similar to amethyst where they have clusters forming from budding blocks
		 */
		val CLUSTERS: TagKey<Item> = tag("clusters")
		val CROPS: TagKey<Item> = tag("crops")
		val CROPS_BEETROOT: TagKey<Item> = tag("crops/beetroot")
		val CROPS_CARROT: TagKey<Item> = tag("crops/carrot")
		val CROPS_NETHER_WART: TagKey<Item> = tag("crops/nether_wart")
		val CROPS_POTATO: TagKey<Item> = tag("crops/potato")
		val CROPS_WHEAT: TagKey<Item> = tag("crops/wheat")
		val DUSTS: TagKey<Item> = tag("dusts")
		val DUSTS_PRISMARINE: TagKey<Item> = tag("dusts/prismarine")
		val DUSTS_REDSTONE: TagKey<Item> = tag("dusts/redstone")
		val DUSTS_GLOWSTONE: TagKey<Item> = tag("dusts/glowstone")

		/**
		 * Tag that holds all blocks and items that can be dyed a specific color.
		 * (Does not include color blending items like leather armor
		 * Use [net.minecraft.tags.ItemTags.DYEABLE] tag instead for color blending items)
		 *
		 *
		 * Note: Use custom ingredients in recipes to do tag intersections and/or tag exclusions
		 * to make more powerful recipes utilizing multiple tags such as dyed tags for an ingredient.
		 * See [net.neoforged.neoforge.common.crafting.DifferenceIngredient] and [net.neoforged.neoforge.common.crafting.CompoundIngredient]
		 * for various custom ingredients available that can also be used in data generation.
		 */
		val DYED: TagKey<Item> = tag("dyed")
		val DYED_BLACK: TagKey<Item> = tag("dyed/black")
		val DYED_BLUE: TagKey<Item> = tag("dyed/blue")
		val DYED_BROWN: TagKey<Item> = tag("dyed/brown")
		val DYED_CYAN: TagKey<Item> = tag("dyed/cyan")
		val DYED_GRAY: TagKey<Item> = tag("dyed/gray")
		val DYED_GREEN: TagKey<Item> = tag("dyed/green")
		val DYED_LIGHT_BLUE: TagKey<Item> = tag("dyed/light_blue")
		val DYED_LIGHT_GRAY: TagKey<Item> = tag("dyed/light_gray")
		val DYED_LIME: TagKey<Item> = tag("dyed/lime")
		val DYED_MAGENTA: TagKey<Item> = tag("dyed/magenta")
		val DYED_ORANGE: TagKey<Item> = tag("dyed/orange")
		val DYED_PINK: TagKey<Item> = tag("dyed/pink")
		val DYED_PURPLE: TagKey<Item> = tag("dyed/purple")
		val DYED_RED: TagKey<Item> = tag("dyed/red")
		val DYED_WHITE: TagKey<Item> = tag("dyed/white")
		val DYED_YELLOW: TagKey<Item> = tag("dyed/yellow")

		val DYES: TagKey<Item> = tag("dyes")
		val DYES_BLACK: TagKey<Item> = DyeColor.BLACK.tag
		val DYES_RED: TagKey<Item> = DyeColor.RED.tag
		val DYES_GREEN: TagKey<Item> = DyeColor.GREEN.tag
		val DYES_BROWN: TagKey<Item> = DyeColor.BROWN.tag
		val DYES_BLUE: TagKey<Item> = DyeColor.BLUE.tag
		val DYES_PURPLE: TagKey<Item> = DyeColor.PURPLE.tag
		val DYES_CYAN: TagKey<Item> = DyeColor.CYAN.tag
		val DYES_LIGHT_GRAY: TagKey<Item> = DyeColor.LIGHT_GRAY.tag
		val DYES_GRAY: TagKey<Item> = DyeColor.GRAY.tag
		val DYES_PINK: TagKey<Item> = DyeColor.PINK.tag
		val DYES_LIME: TagKey<Item> = DyeColor.LIME.tag
		val DYES_YELLOW: TagKey<Item> = DyeColor.YELLOW.tag
		val DYES_LIGHT_BLUE: TagKey<Item> = DyeColor.LIGHT_BLUE.tag
		val DYES_MAGENTA: TagKey<Item> = DyeColor.MAGENTA.tag
		val DYES_ORANGE: TagKey<Item> = DyeColor.ORANGE.tag
		val DYES_WHITE: TagKey<Item> = DyeColor.WHITE.tag

		private val DyeColor.tag: TagKey<Item>
			get() = tag("dyes/$name".lowercase())


		val EGGS: TagKey<Item> = tag("eggs")
		val END_STONES: TagKey<Item> = tag("end_stones")
		val ENDER_PEARLS: TagKey<Item> = tag("ender_pearls")
		val FEATHERS: TagKey<Item> = tag("feathers")
		val FENCE_GATES: TagKey<Item> = tag("fence_gates")
		val FENCE_GATES_WOODEN: TagKey<Item> = tag("fence_gates/wooden")
		val FENCES: TagKey<Item> = tag("fences")
		val FENCES_NETHER_BRICK: TagKey<Item> = tag("fences/nether_brick")
		val FENCES_WOODEN: TagKey<Item> = tag("fences/wooden")
		val FOODS: TagKey<Item> = tag("foods")

		/**
		 * Apples and other foods that are considered fruits in the culinary field belong in this tag.
		 * Cherries would go here as they are considered a "stone fruit" within culinary fields.
		 */
		val FOODS_FRUITS: TagKey<Item> = tag("foods/fruits")

		/**
		 * Tomatoes and other foods that are considered vegetables in the culinary field belong in this tag.
		 */
		val FOODS_VEGETABLES: TagKey<Item> = tag("foods/vegetables")

		/**
		 * Strawberries, raspberries, and other berry foods belong in this tag.
		 * Cherries would NOT go here as they are considered a "stone fruit" within culinary fields.
		 */
		val FOODS_BERRIES: TagKey<Item> = tag("foods/berries")
		val FOODS_BREADS: TagKey<Item> = tag("foods/breads")
		val FOODS_COOKIES: TagKey<Item> = tag("foods/cookies")
		val FOODS_RAW_MEATS: TagKey<Item> = tag("foods/raw_meats")
		val FOODS_COOKED_MEATS: TagKey<Item> = tag("foods/cooked_meats")
		val FOODS_RAW_FISHES: TagKey<Item> = tag("foods/raw_fishes")
		val FOODS_COOKED_FISHES: TagKey<Item> = tag("foods/cooked_fishes")

		/**
		 * Soups, stews, and other liquid food in bowls belongs in this tag.
		 */
		val FOODS_SOUPS: TagKey<Item> = tag("foods/soups")

		/**
		 * Sweets and candies like lollipops or chocolate belong in this tag.
		 */
		val FOODS_CANDIES: TagKey<Item> = tag("foods/candies")

		/**
		 * Foods like cake that can be eaten when placed in the world belong in this tag.
		 */
		val FOODS_EDIBLE_WHEN_PLACED: TagKey<Item> = tag("foods/edible_when_placed")

		/**
		 * For foods that inflict food poisoning-like effects.
		 * Examples are Rotten Flesh's Hunger or Pufferfish's Nausea, or Poisonous Potato's Poison.
		 */
		val FOODS_FOOD_POISONING: TagKey<Item> = tag("foods/food_poisoning")
		val GEMS: TagKey<Item> = tag("gems")
		val GEMS_DIAMOND: TagKey<Item> = tag("gems/diamond")
		val GEMS_EMERALD: TagKey<Item> = tag("gems/emerald")
		val GEMS_AMETHYST: TagKey<Item> = tag("gems/amethyst")
		val GEMS_LAPIS: TagKey<Item> = tag("gems/lapis")
		val GEMS_PRISMARINE: TagKey<Item> = tag("gems/prismarine")
		val GEMS_QUARTZ: TagKey<Item> = tag("gems/quartz")

		val GLASS_BLOCKS: TagKey<Item> = tag("glass_blocks")
		val GLASS_BLOCKS_COLORLESS: TagKey<Item> = tag("glass_blocks/colorless")

		/**
		 * Glass which is made from cheap resources like sand and only minor additional ingredients like dyes
		 */
		val GLASS_BLOCKS_CHEAP: TagKey<Item> = tag("glass_blocks/cheap")
		val GLASS_BLOCKS_STAINED: TagKey<Item> = tag("glass_blocks/stained")
		val GLASS_BLOCKS_TINTED: TagKey<Item> = tag("glass_blocks/tinted")

		val GLASS_PANES: TagKey<Item> = tag("glass_panes")
		val GLASS_PANES_COLORLESS: TagKey<Item> = tag("glass_panes/colorless")
		val GLASS_PANES_STAINED: TagKey<Item> = tag("glass_panes/stained")

		val GRAVELS: TagKey<Item> = tag("gravel")
		val GUNPOWDERS: TagKey<Item> = tag("gunpowder")

		/**
		 * Tag that holds all items that recipe viewers should not show to users.
		 */
		val HIDDEN_FROM_RECIPE_VIEWERS: TagKey<Item> = tag("hidden_from_recipe_viewers")
		val INGOTS: TagKey<Item> = tag("ingots")
		val INGOTS_COPPER: TagKey<Item> = tag("ingots/copper")
		val INGOTS_GOLD: TagKey<Item> = tag("ingots/gold")
		val INGOTS_IRON: TagKey<Item> = tag("ingots/iron")
		val INGOTS_NETHERITE: TagKey<Item> = tag("ingots/netherite")
		val LEATHERS: TagKey<Item> = tag("leather")
		val MUSHROOMS: TagKey<Item> = tag("mushrooms")
		val NETHER_STARS: TagKey<Item> = tag("nether_stars")
		val NETHERRACKS: TagKey<Item> = tag("netherrack")
		val NUGGETS: TagKey<Item> = tag("nuggets")
		val NUGGETS_GOLD: TagKey<Item> = tag("nuggets/gold")
		val NUGGETS_IRON: TagKey<Item> = tag("nuggets/iron")
		val OBSIDIANS: TagKey<Item> = tag("obsidians")

		/**
		 * Blocks which are often replaced by deepslate ores, i.e. the ores in the tag [.ORES_IN_GROUND_DEEPSLATE], during world generation
		 */
		val ORE_BEARING_GROUND_DEEPSLATE: TagKey<Item> = tag("ore_bearing_ground/deepslate")

		/**
		 * Blocks which are often replaced by netherrack ores, i.e. the ores in the tag [.ORES_IN_GROUND_NETHERRACK], during world generation
		 */
		val ORE_BEARING_GROUND_NETHERRACK: TagKey<Item> = tag("ore_bearing_ground/netherrack")

		/**
		 * Blocks which are often replaced by stone ores, i.e. the ores in the tag [.ORES_IN_GROUND_STONE], during world generation
		 */
		val ORE_BEARING_GROUND_STONE: TagKey<Item> = tag("ore_bearing_ground/stone")

		/**
		 * Ores which on average result in more than one resource worth of materials
		 */
		val ORE_RATES_DENSE: TagKey<Item> = tag("ore_rates/dense")

		/**
		 * Ores which on average result in one resource worth of materials
		 */
		val ORE_RATES_SINGULAR: TagKey<Item> = tag("ore_rates/singular")

		/**
		 * Ores which on average result in less than one resource worth of materials
		 */
		val ORE_RATES_SPARSE: TagKey<Item> = tag("ore_rates/sparse")
		val ORES: TagKey<Item> = tag("ores")
		val ORES_COAL: TagKey<Item> = tag("ores/coal")
		val ORES_COPPER: TagKey<Item> = tag("ores/copper")
		val ORES_DIAMOND: TagKey<Item> = tag("ores/diamond")
		val ORES_EMERALD: TagKey<Item> = tag("ores/emerald")
		val ORES_GOLD: TagKey<Item> = tag("ores/gold")
		val ORES_IRON: TagKey<Item> = tag("ores/iron")
		val ORES_LAPIS: TagKey<Item> = tag("ores/lapis")
		val ORES_NETHERITE_SCRAP: TagKey<Item> = tag("ores/netherite_scrap")
		val ORES_QUARTZ: TagKey<Item> = tag("ores/quartz")
		val ORES_REDSTONE: TagKey<Item> = tag("ores/redstone")

		/**
		 * Ores in deepslate (or in equivalent blocks in the tag [.ORE_BEARING_GROUND_DEEPSLATE]) which could logically use deepslate as recipe input or output
		 */
		val ORES_IN_GROUND_DEEPSLATE: TagKey<Item> = tag("ores_in_ground/deepslate")

		/**
		 * Ores in netherrack (or in equivalent blocks in the tag [.ORE_BEARING_GROUND_NETHERRACK]) which could logically use netherrack as recipe input or output
		 */
		val ORES_IN_GROUND_NETHERRACK: TagKey<Item> = tag("ores_in_ground/netherrack")

		/**
		 * Ores in stone (or in equivalent blocks in the tag [.ORE_BEARING_GROUND_STONE]) which could logically use stone as recipe input or output
		 */
		val ORES_IN_GROUND_STONE: TagKey<Item> = tag("ores_in_ground/stone")
		val PLAYER_WORKSTATIONS_CRAFTING_TABLES: TagKey<Item> = tag("player_workstations/crafting_tables")
		val PLAYER_WORKSTATIONS_FURNACES: TagKey<Item> = tag("player_workstations/furnaces")
		val RAW_BLOCKS: TagKey<Item> = tag("raw_blocks")
		val RAW_BLOCKS_COPPER: TagKey<Item> = tag("raw_blocks/copper")
		val RAW_BLOCKS_GOLD: TagKey<Item> = tag("raw_blocks/gold")
		val RAW_BLOCKS_IRON: TagKey<Item> = tag("raw_blocks/iron")
		val RAW_MATERIALS: TagKey<Item> = tag("raw_materials")
		val RAW_MATERIALS_COPPER: TagKey<Item> = tag("raw_materials/copper")
		val RAW_MATERIALS_GOLD: TagKey<Item> = tag("raw_materials/gold")
		val RAW_MATERIALS_IRON: TagKey<Item> = tag("raw_materials/iron")

		/**
		 * For rod-like materials to be used in recipes.
		 */
		val RODS: TagKey<Item> = tag("rods")
		val RODS_BLAZE: TagKey<Item> = tag("rods/blaze")
		val RODS_BREEZE: TagKey<Item> = tag("rods/breeze")

		/**
		 * For stick-like materials to be used in recipes.
		 * One example is a mod adds stick variants such as Spruce Sticks but would like stick recipes to be able to use it.
		 */
		val RODS_WOODEN: TagKey<Item> = tag("rods/wooden")
		val ROPES: TagKey<Item> = tag("ropes")

		val SANDS: TagKey<Item> = tag("sands")
		val SANDS_COLORLESS: TagKey<Item> = tag("sands/colorless")
		val SANDS_RED: TagKey<Item> = tag("sands/red")

		val SANDSTONE_BLOCKS: TagKey<Item> = tag("sandstone/blocks")
		val SANDSTONE_SLABS: TagKey<Item> = tag("sandstone/slabs")
		val SANDSTONE_STAIRS: TagKey<Item> = tag("sandstone/stairs")
		val SANDSTONE_RED_BLOCKS: TagKey<Item> = tag("sandstone/red_blocks")
		val SANDSTONE_RED_SLABS: TagKey<Item> = tag("sandstone/red_slabs")
		val SANDSTONE_RED_STAIRS: TagKey<Item> = tag("sandstone/red_stairs")
		val SANDSTONE_UNCOLORED_BLOCKS: TagKey<Item> = tag("sandstone/uncolored_blocks")
		val SANDSTONE_UNCOLORED_SLABS: TagKey<Item> = tag("sandstone/uncolored_slabs")
		val SANDSTONE_UNCOLORED_STAIRS: TagKey<Item> = tag("sandstone/uncolored_stairs")

		val SEEDS: TagKey<Item> = tag("seeds")
		val SEEDS_BEETROOT: TagKey<Item> = tag("seeds/beetroot")
		val SEEDS_MELON: TagKey<Item> = tag("seeds/melon")
		val SEEDS_PUMPKIN: TagKey<Item> = tag("seeds/pumpkin")
		val SEEDS_WHEAT: TagKey<Item> = tag("seeds/wheat")
		val SHULKER_BOXES: TagKey<Item> = tag("shulker_boxes")
		val SLIMEBALLS: TagKey<Item> = tag("slimeballs")

		/**
		 * Natural stone-like blocks that can be used as a base ingredient in recipes that takes stone.
		 */
		val STONES: TagKey<Item> = tag("stones")

		val SKULLS: TagKey<Item> = existing(ItemTags.SKULLS)

		/**
		 * A storage block is generally a block that has a recipe to craft a bulk of 1 kind of resource to a block
		 * and has a mirror recipe to reverse the crafting with no loss in resources.
		 *
		 *
		 * Honey Block is special in that the reversing recipe is not a perfect mirror of the crafting recipe
		 * and so, it is considered a special case and not given a storage block tag.
		 */
		val STORAGE_BLOCKS: TagKey<Item> = tag("storage_blocks")
		val STORAGE_BLOCKS_AMETHYST: TagKey<Item> = tag("storage_blocks/amethyst")
		val STORAGE_BLOCKS_BONE_MEAL: TagKey<Item> = tag("storage_blocks/bone_meal")
		val STORAGE_BLOCKS_COAL: TagKey<Item> = tag("storage_blocks/coal")
		val STORAGE_BLOCKS_COPPER: TagKey<Item> = tag("storage_blocks/copper")
		val STORAGE_BLOCKS_DIAMOND: TagKey<Item> = tag("storage_blocks/diamond")
		val STORAGE_BLOCKS_DRIED_KELP: TagKey<Item> = tag("storage_blocks/dried_kelp")
		val STORAGE_BLOCKS_EMERALD: TagKey<Item> = tag("storage_blocks/emerald")
		val STORAGE_BLOCKS_GOLD: TagKey<Item> = tag("storage_blocks/gold")
		val STORAGE_BLOCKS_IRON: TagKey<Item> = tag("storage_blocks/iron")
		val STORAGE_BLOCKS_LAPIS: TagKey<Item> = tag("storage_blocks/lapis")
		val STORAGE_BLOCKS_NETHERITE: TagKey<Item> = tag("storage_blocks/netherite")
		val STORAGE_BLOCKS_QUARTZ: TagKey<Item> = tag("storage_blocks/quartz")
		val STORAGE_BLOCKS_RAW_COPPER: TagKey<Item> = tag("storage_blocks/raw_copper")
		val STORAGE_BLOCKS_RAW_GOLD: TagKey<Item> = tag("storage_blocks/raw_gold")
		val STORAGE_BLOCKS_RAW_IRON: TagKey<Item> = tag("storage_blocks/raw_iron")
		val STORAGE_BLOCKS_REDSTONE: TagKey<Item> = tag("storage_blocks/redstone")
		val STORAGE_BLOCKS_SLIME: TagKey<Item> = tag("storage_blocks/slime")
		val STORAGE_BLOCKS_WHEAT: TagKey<Item> = tag("storage_blocks/wheat")
		val STRINGS: TagKey<Item> = tag("strings")
		val VILLAGER_JOB_SITES: TagKey<Item> = tag("villager_job_sites")


		// Tools and Armors
		/**
		 * A tag containing all existing tools. Do not use this tag for determining a tool's behavior.
		 * Please use [net.neoforged.neoforge.common.ToolActions] instead for what action a tool can do.
		 *
		 * @see ToolAction
		 *
		 * @see ToolActions
		 */
		val TOOLS: TagKey<Item> = tag("tools")

		/**
		 * A tag containing all existing axes. Do not use this tag for determining a tool's behavior.
		 * Please use [net.neoforged.neoforge.common.ToolActions] instead for what action a tool can do.
		 *
		 * @see ToolAction
		 *
		 * @see ToolActions
		 */
		val TOOLS_AXES: TagKey<Item> = existing(ItemTags.AXES)

		/**
		 * A tag containing all existing hoes. Do not use this tag for determining a tool's behavior.
		 * Please use [net.neoforged.neoforge.common.ToolActions] instead for what action a tool can do.
		 *
		 * @see ToolAction
		 *
		 * @see ToolActions
		 */
		val TOOLS_HOES: TagKey<Item> = existing(ItemTags.HOES)

		/**
		 * A tag containing all existing pickaxes. Do not use this tag for determining a tool's behavior.
		 * Please use [net.neoforged.neoforge.common.ToolActions] instead for what action a tool can do.
		 *
		 * @see ToolAction
		 *
		 * @see ToolActions
		 */
		val TOOLS_PICKAXES: TagKey<Item> = existing(ItemTags.PICKAXES)

		/**
		 * A tag containing all existing shovels. Do not use this tag for determining a tool's behavior.
		 * Please use [net.neoforged.neoforge.common.ToolActions] instead for what action a tool can do.
		 *
		 * @see ToolAction
		 *
		 * @see ToolActions
		 */
		val TOOLS_SHOVELS: TagKey<Item> = existing(ItemTags.SHOVELS)

		/**
		 * A tag containing all existing swords. Do not use this tag for determining a tool's behavior.
		 * Please use [net.neoforged.neoforge.common.ToolActions] instead for what action a tool can do.
		 *
		 * @see ToolAction
		 *
		 * @see ToolActions
		 */
		val TOOLS_SWORDS: TagKey<Item> = existing(ItemTags.SWORDS)

		/**
		 * A tag containing all existing shields. Do not use this tag for determining a tool's behavior.
		 * Please use [net.neoforged.neoforge.common.ToolActions] instead for what action a tool can do.
		 *
		 * @see ToolAction
		 *
		 * @see ToolActions
		 */
		val TOOLS_SHIELDS: TagKey<Item> = tag("tools/shields")

		/**
		 * A tag containing all existing bows. Do not use this tag for determining a tool's behavior.
		 * Please use [net.neoforged.neoforge.common.ToolActions] instead for what action a tool can do.
		 *
		 * @see ToolAction
		 *
		 * @see ToolActions
		 */
		val TOOLS_BOWS: TagKey<Item> = tag("tools/bows")

		/**
		 * A tag containing all existing crossbows. Do not use this tag for determining a tool's behavior.
		 * Please use [net.neoforged.neoforge.common.ToolActions] instead for what action a tool can do.
		 *
		 * @see net.neoforged.neoforge.common.ToolAction
		 *
		 * @see net.neoforged.neoforge.common.ToolActions
		 */
		val TOOLS_CROSSBOWS: TagKey<Item> = tag("tools/crossbows")

		/**
		 * A tag containing all existing fishing rods. Do not use this tag for determining a tool's behavior.
		 * Please use [net.neoforged.neoforge.common.ToolActions] instead for what action a tool can do.
		 *
		 * @see net.neoforged.neoforge.common.ToolAction
		 *
		 * @see net.neoforged.neoforge.common.ToolActions
		 */
		val TOOLS_FISHING_RODS: TagKey<Item> = tag("tools/fishing_rods")

		/**
		 * A tag containing all existing spears. Other tools such as throwing knives or boomerangs
		 * should not be put into this tag and should be put into their own tool tags.
		 * Do not use this tag for determining a tool's behavior.
		 * Please use [net.neoforged.neoforge.common.ToolActions] instead for what action a tool can do.
		 *
		 * @see ToolAction
		 *
		 * @see ToolActions
		 */
		val TOOLS_SPEARS: TagKey<Item> = tag("tools/spears")

		/**
		 * A tag containing all existing shears. Do not use this tag for determining a tool's behavior.
		 * Please use [net.neoforged.neoforge.common.ToolActions] instead for what action a tool can do.
		 *
		 * @see ToolAction
		 *
		 * @see ToolActions
		 */
		val TOOLS_SHEARS: TagKey<Item> = tag("tools/shears")

		/**
		 * A tag containing all existing brushes. Do not use this tag for determining a tool's behavior.
		 * Please use [net.neoforged.neoforge.common.ToolActions] instead for what action a tool can do.
		 *
		 * @see ToolAction
		 *
		 * @see ToolActions
		 */
		val TOOLS_BRUSHES: TagKey<Item> = tag("tools/brushes")

		/**
		 * Collects the 4 vanilla armor tags into one parent collection for ease.
		 */
		val ARMORS: TagKey<Item> = tag("armors")

		/**
		 * A tag containing all existing helmets.
		 */
		val ARMORS_HELMETS: TagKey<Item> = existing(ItemTags.HEAD_ARMOR)

		/**
		 * A tag containing all chestplates.
		 */
		val ARMORS_CHESTPLATES: TagKey<Item> = existing(ItemTags.CHEST_ARMOR)

		/**
		 * A tag containing all existing leggings.
		 */
		val ARMORS_LEGGINGS: TagKey<Item> = existing(ItemTags.LEG_ARMOR)

		/**
		 * A tag containing all existing boots.
		 */
		val ARMORS_BOOTS: TagKey<Item> = existing(ItemTags.FOOT_ARMOR)

		/**
		 * Collects the many enchantable tags into one parent collection for ease.
		 */
		val ENCHANTABLES: TagKey<Item> = tag("enchantables")

	}

	object Fluids : Tags<Fluid>(Registries.FLUID)
	{
		internal fun init()
		{
		}

		/**
		 * Holds all fluids related to water.
		 * This tag is done to help out multi-loader mods/datapacks where the vanilla water tag has attached behaviors outside Neo.
		 */
		val WATER: TagKey<Fluid> = tag("water")

		/**
		 * Holds all fluids related to lava.
		 * This tag is done to help out multi-loader mods/datapacks where the vanilla lava tag has attached behaviors outside Neo.
		 */
		val LAVA: TagKey<Fluid> = tag("lava")

		/**
		 * Holds all fluids related to milk.
		 */
		val MILK: TagKey<Fluid> = tag("milk")

		/**
		 * Holds all fluids that are gaseous at room temperature.
		 */
		val GASEOUS: TagKey<Fluid> = tag("gaseous")

		/**
		 * Holds all fluids related to honey.<br></br>
		 * (Standard unit for honey bottle is 250mb per bottle)
		 */
		val HONEY: TagKey<Fluid> = tag("honey")

		/**
		 * Holds all fluids related to potions. The effects of the potion fluid should be read from NBT.
		 * The effects and color of the potion fluid should be read from [net.minecraft.core.component.DataComponents.POTION_CONTENTS]
		 * component that people should be attaching to the fluidstack of this fluid.<br></br>
		 * (Standard unit for potions is 250mb per bottle)
		 */
		val POTION: TagKey<Fluid> = tag("potion")

		/**
		 * Holds all fluids related to Suspicious Stew.
		 * The effects of the suspicious stew fluid should be read from [net.minecraft.core.component.DataComponents.SUSPICIOUS_STEW_EFFECTS]
		 * component that people should be attaching to the fluidstack of this fluid.<br></br>
		 * (Standard unit for suspicious stew is 250mb per bowl)
		 */
		val SUSPICIOUS_STEW: TagKey<Fluid> = tag("suspicious_stew")

		/**
		 * Holds all fluids related to Mushroom Stew.<br></br>
		 * (Standard unit for mushroom stew is 250mb per bowl)
		 */
		val MUSHROOM_STEW: TagKey<Fluid> = tag("mushroom_stew")

		/**
		 * Holds all fluids related to Rabbit Stew.<br></br>
		 * (Standard unit for rabbit stew is 250mb per bowl)
		 */
		val RABBIT_STEW: TagKey<Fluid> = tag("rabbit_stew")

		/**
		 * Holds all fluids related to Beetroot Soup.<br></br>
		 * (Standard unit for beetroot soup is 250mb per bowl)
		 */
		val BEETROOT_SOUP: TagKey<Fluid> = tag("beetroot_soup")

		/**
		 * Tag that holds all fluids that recipe viewers should not show to users.
		 */
		val HIDDEN_FROM_RECIPE_VIEWERS: TagKey<Fluid> = tag("hidden_from_recipe_viewers")
	}

	object Biomes : Tags<Biome>(Registries.BIOME)
	{
		internal fun init()
		{
		}

		/**
		 * For biomes that should not spawn monsters over time the normal way.
		 * In other words, their Spawners and Spawn Cost entries have the monster category empty.
		 * Example: Mushroom Biomes not having Zombies, Creepers, Skeleton, nor any other normal monsters.
		 */
		val NO_DEFAULT_MONSTERS: TagKey<Biome> = tag("no_default_monsters")

		/**
		 * Biomes that should not be locatable/selectable by modded biome-locating items or abilities.
		 */
		val HIDDEN_FROM_LOCATOR_SELECTION: TagKey<Biome> = tag("hidden_from_locator_selection")

		val IS_VOID: TagKey<Biome> = tag("is_void")

		val IS_HOT: TagKey<Biome> = tag("is_hot")
		val IS_HOT_OVERWORLD: TagKey<Biome> = tag("is_hot/overworld")
		val IS_HOT_NETHER: TagKey<Biome> = tag("is_hot/nether")
		val IS_HOT_END: TagKey<Biome> = tag("is_hot/end")

		val IS_COLD: TagKey<Biome> = tag("is_cold")
		val IS_COLD_OVERWORLD: TagKey<Biome> = tag("is_cold/overworld")
		val IS_COLD_NETHER: TagKey<Biome> = tag("is_cold/nether")
		val IS_COLD_END: TagKey<Biome> = tag("is_cold/end")

		val IS_SPARSE_VEGETATION: TagKey<Biome> = tag("is_sparse_vegetation")
		val IS_SPARSE_VEGETATION_OVERWORLD: TagKey<Biome> = tag("is_sparse_vegetation/overworld")
		val IS_SPARSE_VEGETATION_NETHER: TagKey<Biome> = tag("is_sparse_vegetation/nether")
		val IS_SPARSE_VEGETATION_END: TagKey<Biome> = tag("is_sparse_vegetation/end")
		val IS_DENSE_VEGETATION: TagKey<Biome> = tag("is_dense_vegetation")
		val IS_DENSE_VEGETATION_OVERWORLD: TagKey<Biome> = tag("is_dense_vegetation/overworld")
		val IS_DENSE_VEGETATION_NETHER: TagKey<Biome> = tag("is_dense_vegetation/nether")
		val IS_DENSE_VEGETATION_END: TagKey<Biome> = tag("is_dense_vegetation/end")

		val IS_WET: TagKey<Biome> = tag("is_wet")
		val IS_WET_OVERWORLD: TagKey<Biome> = tag("is_wet/overworld")
		val IS_WET_NETHER: TagKey<Biome> = tag("is_wet/nether")
		val IS_WET_END: TagKey<Biome> = tag("is_wet/end")
		val IS_DRY: TagKey<Biome> = tag("is_dry")
		val IS_DRY_OVERWORLD: TagKey<Biome> = tag("is_dry/overworld")
		val IS_DRY_NETHER: TagKey<Biome> = tag("is_dry/nether")
		val IS_DRY_END: TagKey<Biome> = tag("is_dry/end")

		/**
		 * Biomes that spawn in the Overworld.
		 * (This is for people who want to tag their biomes without getting
		 * side effects from [net.minecraft.tags.BiomeTags.IS_OVERWORLD]
		 *
		 *
		 * NOTE: If you do not add to the vanilla Overworld tag, be sure to add to
		 * [net.minecraft.tags.BiomeTags.HAS_STRONGHOLD] so some Strongholds do not go missing.)
		 */
		val IS_OVERWORLD: TagKey<Biome> = tag("is_overworld")

		val IS_CONIFEROUS_TREE: TagKey<Biome> = tag("is_tree/coniferous")
		val IS_SAVANNA_TREE: TagKey<Biome> = tag("is_tree/savanna")
		val IS_JUNGLE_TREE: TagKey<Biome> = tag("is_tree/jungle")
		val IS_DECIDUOUS_TREE: TagKey<Biome> = tag("is_tree/deciduous")

		/**
		 * Biomes that spawn as part of giant mountains.
		 * (This is for people who want to tag their biomes without getting
		 * side effects from [net.minecraft.tags.BiomeTags.IS_MOUNTAIN])
		 */
		val IS_MOUNTAIN: TagKey<Biome> = tag("is_mountain")
		val IS_MOUNTAIN_PEAK: TagKey<Biome> = tag("is_mountain/peak")
		val IS_MOUNTAIN_SLOPE: TagKey<Biome> = tag("is_mountain/slope")

		/**
		 * For temperate or warmer plains-like biomes.
		 * For snowy plains-like biomes, see [.IS_SNOWY_PLAINS].
		 */
		val IS_PLAINS: TagKey<Biome> = tag("is_plains")

		/**
		 * For snowy plains-like biomes.
		 * For warmer plains-like biomes, see [.IS_PLAINS].
		 */
		val IS_SNOWY_PLAINS: TagKey<Biome> = tag("is_snowy_plains")

		/**
		 * Biomes densely populated with deciduous trees.
		 * (This is for people who want to tag their biomes without getting
		 * side effects from [net.minecraft.tags.BiomeTags.IS_FOREST])
		 */
		val IS_FOREST: TagKey<Biome> = tag("is_forest")
		val IS_BIRCH_FOREST: TagKey<Biome> = tag("is_birch_forest")
		val IS_FLOWER_FOREST: TagKey<Biome> = tag("is_flower_forest")

		/**
		 * Biomes that spawn as a taiga.
		 * (This is for people who want to tag their biomes without getting
		 * side effects from [net.minecraft.tags.BiomeTags.IS_TAIGA])
		 */
		val IS_TAIGA: TagKey<Biome> = tag("is_taiga")
		val IS_OLD_GROWTH: TagKey<Biome> = tag("is_old_growth")

		/**
		 * Biomes that spawn as a hills biome. (Previously was called Extreme Hills biome in past)
		 * (This is for people who want to tag their biomes without getting
		 * side effects from [net.minecraft.tags.BiomeTags.IS_HILL])
		 */
		val IS_HILL: TagKey<Biome> = tag("is_hill")
		val IS_WINDSWEPT: TagKey<Biome> = tag("is_windswept")

		/**
		 * Biomes that spawn as a jungle.
		 * (This is for people who want to tag their biomes without getting
		 * side effects from [net.minecraft.tags.BiomeTags.IS_JUNGLE])
		 */
		val IS_JUNGLE: TagKey<Biome> = tag("is_jungle")

		/**
		 * Biomes that spawn as a savanna.
		 * (This is for people who want to tag their biomes without getting
		 * side effects from [net.minecraft.tags.BiomeTags.IS_SAVANNA])
		 */
		val IS_SAVANNA: TagKey<Biome> = tag("is_savanna")
		val IS_SWAMP: TagKey<Biome> = tag("is_swamp")
		val IS_DESERT: TagKey<Biome> = tag("is_desert")

		/**
		 * Biomes that spawn as a badlands.
		 * (This is for people who want to tag their biomes without getting
		 * side effects from [net.minecraft.tags.BiomeTags.IS_BADLANDS])
		 */
		val IS_BADLANDS: TagKey<Biome> = tag("is_badlands")

		/**
		 * Biomes that are dedicated to spawning on the shoreline of a body of water.
		 * (This is for people who want to tag their biomes without getting
		 * side effects from [net.minecraft.tags.BiomeTags.IS_BEACH])
		 */
		val IS_BEACH: TagKey<Biome> = tag("is_beach")
		val IS_STONY_SHORES: TagKey<Biome> = tag("is_stony_shores")
		val IS_MUSHROOM: TagKey<Biome> = tag("is_mushroom")

		/**
		 * Biomes that spawn as a river.
		 * (This is for people who want to tag their biomes without getting
		 * side effects from [net.minecraft.tags.BiomeTags.IS_RIVER])
		 */
		val IS_RIVER: TagKey<Biome> = tag("is_river")

		/**
		 * Biomes that spawn as part of the world's oceans.
		 * (This is for people who want to tag their biomes without getting
		 * side effects from [net.minecraft.tags.BiomeTags.IS_OCEAN])
		 */
		val IS_OCEAN: TagKey<Biome> = tag("is_ocean")

		/**
		 * Biomes that spawn as part of the world's oceans that have low depth.
		 * (This is for people who want to tag their biomes without getting
		 * side effects from [net.minecraft.tags.BiomeTags.IS_DEEP_OCEAN])
		 */
		val IS_DEEP_OCEAN: TagKey<Biome> = tag("is_deep_ocean")
		val IS_SHALLOW_OCEAN: TagKey<Biome> = tag("is_shallow_ocean")

		val IS_UNDERGROUND: TagKey<Biome> = tag("is_underground")
		val IS_CAVE: TagKey<Biome> = tag("is_cave")

		val IS_LUSH: TagKey<Biome> = tag("is_lush")
		val IS_MAGICAL: TagKey<Biome> = tag("is_magical")
		val IS_RARE: TagKey<Biome> = tag("is_rare")
		val IS_PLATEAU: TagKey<Biome> = tag("is_plateau")
		val IS_MODIFIED: TagKey<Biome> = tag("is_modified")
		val IS_SPOOKY: TagKey<Biome> = tag("is_spooky")

		/**
		 * Biomes that lack any natural life or vegetation.
		 * (Example, land destroyed and sterilized by nuclear weapons)
		 */
		val IS_WASTELAND: TagKey<Biome> = tag("is_wasteland")

		/**
		 * Biomes whose flora primarily consists of dead or decaying vegetation.
		 */
		val IS_DEAD: TagKey<Biome> = tag("is_dead")

		/**
		 * Biomes with a large amount of flowers.
		 */
		val IS_FLORAL: TagKey<Biome> = tag("is_floral")

		/**
		 * Biomes that are able to spawn sand-based blocks on the surface.
		 */
		val IS_SANDY: TagKey<Biome> = tag("is_sandy")

		/**
		 * For biomes that contains lots of naturally spawned snow.
		 * For biomes where lot of ice is present, see [IS_ICY].
		 * Biome with lots of both snow and ice may be in both tags.
		 */
		val IS_SNOWY: TagKey<Biome> = tag("is_snowy")

		/**
		 * For land biomes where ice naturally spawns.
		 * For biomes where snow alone spawns, see [IS_SNOWY].
		 */
		val IS_ICY: TagKey<Biome> = tag("is_icy")

		/**
		 * Biomes consisting primarily of water.
		 */
		val IS_AQUATIC: TagKey<Biome> = tag("is_aquatic")

		/**
		 * For water biomes where ice naturally spawns.
		 * For biomes where snow alone spawns, see [IS_SNOWY].
		 */
		val IS_AQUATIC_ICY: TagKey<Biome> = tag("is_aquatic_icy")

		/**
		 * Biomes that spawn in the Nether.
		 * (This is for people who want to tag their biomes without getting
		 * side effects from [net.minecraft.tags.BiomeTags.IS_NETHER])
		 */
		val IS_NETHER: TagKey<Biome> = tag("is_nether")
		val IS_NETHER_FOREST: TagKey<Biome> = tag("is_nether_forest")

		/**
		 * Biomes that spawn in the End.
		 * (This is for people who want to tag their biomes without getting
		 * side effects from [net.minecraft.tags.BiomeTags.IS_END])
		 */
		val IS_END: TagKey<Biome> = tag("is_end")

		/**
		 * Biomes that spawn as part of the large islands outside the center island in The End dimension.
		 */
		val IS_OUTER_END_ISLAND: TagKey<Biome> = tag("is_outer_end_island")
	}
}