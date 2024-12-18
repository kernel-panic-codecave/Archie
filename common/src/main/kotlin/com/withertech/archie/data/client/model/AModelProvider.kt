package com.withertech.archie.data.client.model

import dev.architectury.platform.Mod
import com.google.common.base.Preconditions
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.withertech.archie.Archie
import com.withertech.archie.data.IADataProvider
import net.minecraft.data.CachedOutput
import net.minecraft.data.DataProvider
import net.minecraft.data.PackOutput
import net.minecraft.resources.ResourceLocation
import org.jetbrains.annotations.VisibleForTesting
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.function.Function
import kotlin.system.exitProcess

abstract class AModelProvider<T : AModelBuilder<T>>(
	final override val output: PackOutput,
	final override val mod: Mod,
	private val folder: String,
	private val factory: Function<ResourceLocation, T>,
	final override val exitOnError: Boolean
) : IADataProvider
{
	@VisibleForTesting
	val generatedModels: MutableMap<ResourceLocation, T> = mutableMapOf()

	protected abstract fun generate()

	override fun getName(): String = format("Models")


	fun getBuilder(path: String, block: T.() -> Unit = {}): T
	{
		Preconditions.checkNotNull(path, "Path must not be null")
		val outputLoc =
			extendWithFolder(if (path.contains(":")) ResourceLocation.parse(path) else ResourceLocation.fromNamespaceAndPath(mod.modId, path))
		return generatedModels.computeIfAbsent(outputLoc, factory).apply(block)
	}

	private fun extendWithFolder(rl: ResourceLocation): ResourceLocation
	{
		if (rl.path.contains("/"))
		{
			return rl
		}
		return ResourceLocation.fromNamespaceAndPath(rl.namespace, folder + "/" + rl.path)
	}

	fun withExistingParent(name: String, parent: String, block: T.() -> Unit = {}): T
	{
		return withExistingParent(name, mcLoc(parent)).apply(block)
	}

	fun withExistingParent(name: String, parent: ResourceLocation, block: T.() -> Unit = {}): T
	{
		return getBuilder(name).parent(getExistingFile(parent)).apply(block)
	}

	fun cube(
		name: String,
		down: ResourceLocation,
		up: ResourceLocation,
		north: ResourceLocation,
		south: ResourceLocation,
		east: ResourceLocation,
		west: ResourceLocation
	): T
	{
		return withExistingParent(name, "cube")
			.texture("down", down)
			.texture("up", up)
			.texture("north", north)
			.texture("south", south)
			.texture("east", east)
			.texture("west", west)
	}

	private fun singleTexture(name: String, parent: String, texture: ResourceLocation): T
	{
		return singleTexture(name, mcLoc(parent), texture)
	}

	fun singleTexture(name: String, parent: ResourceLocation, texture: ResourceLocation): T
	{
		return singleTexture(name, parent, "texture", texture)
	}

	private fun singleTexture(name: String, parent: String, textureKey: String, texture: ResourceLocation): T
	{
		return singleTexture(name, mcLoc(parent), textureKey, texture)
	}

	fun singleTexture(name: String, parent: ResourceLocation, textureKey: String, texture: ResourceLocation): T
	{
		return withExistingParent(name, parent)
			.texture(textureKey, texture)
	}

	fun cubeAll(name: String, texture: ResourceLocation): T
	{
		return singleTexture(name, "$BLOCK_FOLDER/cube_all", "all", texture)
	}

	fun cubeTop(name: String, side: ResourceLocation, top: ResourceLocation): T
	{
		return withExistingParent(name, "$BLOCK_FOLDER/cube_top")
			.texture("side", side)
			.texture("top", top)
	}

	private fun sideBottomTop(
		name: String,
		parent: String,
		side: ResourceLocation,
		bottom: ResourceLocation,
		top: ResourceLocation
	): T
	{
		return withExistingParent(name, parent)
			.texture("side", side)
			.texture("bottom", bottom)
			.texture("top", top)
	}

	fun cubeBottomTop(name: String, side: ResourceLocation, bottom: ResourceLocation, top: ResourceLocation): T
	{
		return sideBottomTop(name, "$BLOCK_FOLDER/cube_bottom_top", side, bottom, top)
	}

	fun cubeColumn(name: String, side: ResourceLocation, end: ResourceLocation): T
	{
		return withExistingParent(name, "$BLOCK_FOLDER/cube_column")
			.texture("side", side)
			.texture("end", end)
	}

	fun cubeColumnHorizontal(name: String, side: ResourceLocation, end: ResourceLocation): T
	{
		return withExistingParent(name, "$BLOCK_FOLDER/cube_column_horizontal")
			.texture("side", side)
			.texture("end", end)
	}

	fun orientableVertical(name: String, side: ResourceLocation, front: ResourceLocation): T
	{
		return withExistingParent(name, "$BLOCK_FOLDER/orientable_vertical")
			.texture("side", side)
			.texture("front", front)
	}

	fun orientableWithBottom(
		name: String,
		side: ResourceLocation,
		front: ResourceLocation,
		bottom: ResourceLocation,
		top: ResourceLocation
	): T
	{
		return withExistingParent(name, "$BLOCK_FOLDER/orientable_with_bottom")
			.texture("side", side)
			.texture("front", front)
			.texture("bottom", bottom)
			.texture("top", top)
	}

	fun orientable(name: String, side: ResourceLocation, front: ResourceLocation, top: ResourceLocation): T
	{
		return withExistingParent(name, "$BLOCK_FOLDER/orientable")
			.texture("side", side)
			.texture("front", front)
			.texture("top", top)
	}

	fun crop(name: String, crop: ResourceLocation): T
	{
		return singleTexture(name, "$BLOCK_FOLDER/crop", "crop", crop)
	}

	fun cross(name: String, cross: ResourceLocation): T
	{
		return singleTexture(name, "$BLOCK_FOLDER/cross", "cross", cross)
	}

	fun stairs(name: String, side: ResourceLocation, bottom: ResourceLocation, top: ResourceLocation): T
	{
		return sideBottomTop(name, "$BLOCK_FOLDER/stairs", side, bottom, top)
	}

	fun stairsOuter(name: String, side: ResourceLocation, bottom: ResourceLocation, top: ResourceLocation): T
	{
		return sideBottomTop(name, "$BLOCK_FOLDER/outer_stairs", side, bottom, top)
	}

	fun stairsInner(name: String, side: ResourceLocation, bottom: ResourceLocation, top: ResourceLocation): T
	{
		return sideBottomTop(name, "$BLOCK_FOLDER/inner_stairs", side, bottom, top)
	}

	fun slab(name: String, side: ResourceLocation, bottom: ResourceLocation, top: ResourceLocation): T
	{
		return sideBottomTop(name, "$BLOCK_FOLDER/slab", side, bottom, top)
	}

	fun slabTop(name: String, side: ResourceLocation, bottom: ResourceLocation, top: ResourceLocation): T
	{
		return sideBottomTop(name, "$BLOCK_FOLDER/slab_top", side, bottom, top)
	}

	fun button(name: String, texture: ResourceLocation): T
	{
		return singleTexture(name, "$BLOCK_FOLDER/button", texture)
	}

	fun buttonPressed(name: String, texture: ResourceLocation): T
	{
		return singleTexture(name, "$BLOCK_FOLDER/button_pressed", texture)
	}

	fun buttonInventory(name: String, texture: ResourceLocation): T
	{
		return singleTexture(name, "$BLOCK_FOLDER/button_inventory", texture)
	}

	fun pressurePlate(name: String, texture: ResourceLocation): T
	{
		return singleTexture(name, "$BLOCK_FOLDER/pressure_plate_up", texture)
	}

	fun pressurePlateDown(name: String, texture: ResourceLocation): T
	{
		return singleTexture(name, "$BLOCK_FOLDER/pressure_plate_down", texture)
	}

	fun sign(name: String, texture: ResourceLocation): T
	{
		return getBuilder(name).texture("particle", texture)
	}

	fun fencePost(name: String, texture: ResourceLocation): T
	{
		return singleTexture(name, "$BLOCK_FOLDER/fence_post", texture)
	}

	fun fenceSide(name: String, texture: ResourceLocation): T
	{
		return singleTexture(name, "$BLOCK_FOLDER/fence_side", texture)
	}

	fun fenceInventory(name: String, texture: ResourceLocation): T
	{
		return singleTexture(name, "$BLOCK_FOLDER/fence_inventory", texture)
	}

	fun fenceGate(name: String, texture: ResourceLocation): T
	{
		return singleTexture(name, "$BLOCK_FOLDER/template_fence_gate", texture)
	}

	fun fenceGateOpen(name: String, texture: ResourceLocation): T
	{
		return singleTexture(name, "$BLOCK_FOLDER/template_fence_gate_open", texture)
	}

	fun fenceGateWall(name: String, texture: ResourceLocation): T
	{
		return singleTexture(name, "$BLOCK_FOLDER/template_fence_gate_wall", texture)
	}

	fun fenceGateWallOpen(name: String, texture: ResourceLocation): T
	{
		return singleTexture(name, "$BLOCK_FOLDER/template_fence_gate_wall_open", texture)
	}

	fun wallPost(name: String, wall: ResourceLocation): T
	{
		return singleTexture(name, "$BLOCK_FOLDER/template_wall_post", "wall", wall)
	}

	fun wallSide(name: String, wall: ResourceLocation): T
	{
		return singleTexture(name, "$BLOCK_FOLDER/template_wall_side", "wall", wall)
	}

	fun wallSideTall(name: String, wall: ResourceLocation): T
	{
		return singleTexture(name, "$BLOCK_FOLDER/template_wall_side_tall", "wall", wall)
	}

	fun wallInventory(name: String, wall: ResourceLocation): T
	{
		return singleTexture(name, "$BLOCK_FOLDER/wall_inventory", "wall", wall)
	}

	private fun pane(name: String, parent: String, pane: ResourceLocation, edge: ResourceLocation): T
	{
		return withExistingParent(name, "$BLOCK_FOLDER/$parent")
			.texture("pane", pane)
			.texture("edge", edge)
	}

	fun panePost(name: String, pane: ResourceLocation, edge: ResourceLocation): T
	{
		return pane(name, "template_glass_pane_post", pane, edge)
	}

	fun paneSide(name: String, pane: ResourceLocation, edge: ResourceLocation): T
	{
		return pane(name, "template_glass_pane_side", pane, edge)
	}

	fun paneSideAlt(name: String, pane: ResourceLocation, edge: ResourceLocation): T
	{
		return pane(name, "template_glass_pane_side_alt", pane, edge)
	}

	fun paneNoSide(name: String, pane: ResourceLocation): T
	{
		return singleTexture(name, "$BLOCK_FOLDER/template_glass_pane_noside", "pane", pane)
	}

	fun paneNoSideAlt(name: String, pane: ResourceLocation): T
	{
		return singleTexture(name, "$BLOCK_FOLDER/template_glass_pane_noside_alt", "pane", pane)
	}

	private fun door(name: String, model: String, bottom: ResourceLocation, top: ResourceLocation): T
	{
		return withExistingParent(name, "$BLOCK_FOLDER/$model")
			.texture("bottom", bottom)
			.texture("top", top)
	}

	fun doorBottomLeft(name: String, bottom: ResourceLocation, top: ResourceLocation): T
	{
		return door(name, "door_bottom_left", bottom, top)
	}

	fun doorBottomLeftOpen(name: String, bottom: ResourceLocation, top: ResourceLocation): T
	{
		return door(name, "door_bottom_left_open", bottom, top)
	}

	fun doorBottomRight(name: String, bottom: ResourceLocation, top: ResourceLocation): T
	{
		return door(name, "door_bottom_right", bottom, top)
	}

	fun doorBottomRightOpen(name: String, bottom: ResourceLocation, top: ResourceLocation): T
	{
		return door(name, "door_bottom_right_open", bottom, top)
	}

	fun doorTopLeft(name: String, bottom: ResourceLocation, top: ResourceLocation): T
	{
		return door(name, "door_top_left", bottom, top)
	}

	fun doorTopLeftOpen(name: String, bottom: ResourceLocation, top: ResourceLocation): T
	{
		return door(name, "door_top_left_open", bottom, top)
	}

	fun doorTopRight(name: String, bottom: ResourceLocation, top: ResourceLocation): T
	{
		return door(name, "door_top_right", bottom, top)
	}

	fun doorTopRightOpen(name: String, bottom: ResourceLocation, top: ResourceLocation): T
	{
		return door(name, "door_top_right_open", bottom, top)
	}

	fun trapdoorBottom(name: String, texture: ResourceLocation): T
	{
		return singleTexture(name, "$BLOCK_FOLDER/template_trapdoor_bottom", texture)
	}

	fun trapdoorTop(name: String, texture: ResourceLocation): T
	{
		return singleTexture(name, "$BLOCK_FOLDER/template_trapdoor_top", texture)
	}

	fun trapdoorOpen(name: String, texture: ResourceLocation): T
	{
		return singleTexture(name, "$BLOCK_FOLDER/template_trapdoor_open", texture)
	}

	fun trapdoorOrientableBottom(name: String, texture: ResourceLocation): T
	{
		return singleTexture(name, "$BLOCK_FOLDER/template_orientable_trapdoor_bottom", texture)
	}

	fun trapdoorOrientableTop(name: String, texture: ResourceLocation): T
	{
		return singleTexture(name, "$BLOCK_FOLDER/template_orientable_trapdoor_top", texture)
	}

	fun trapdoorOrientableOpen(name: String, texture: ResourceLocation): T
	{
		return singleTexture(name, "$BLOCK_FOLDER/template_orientable_trapdoor_open", texture)
	}

	fun torch(name: String, torch: ResourceLocation): T
	{
		return singleTexture(name, "$BLOCK_FOLDER/template_torch", "torch", torch)
	}

	fun torchWall(name: String, torch: ResourceLocation): T
	{
		return singleTexture(name, "$BLOCK_FOLDER/template_torch_wall", "torch", torch)
	}

	fun carpet(name: String, wool: ResourceLocation): T
	{
		return singleTexture(name, "$BLOCK_FOLDER/carpet", "wool", wool)
	}

	/**
	 * {@return a model builder that's not directly saved to disk. Meant for use in custom model loaders.}
	 */
	fun nested(): T
	{
		return factory.apply(ResourceLocation.parse("dummy:dummy"))
	}

	fun getExistingFile(path: ResourceLocation): AModelFile
	{
		val ret =
			AModelFile(
				extendWithFolder(path)
			)
		return ret
	}

	fun clear()
	{
		generatedModels.clear()
	}

	override fun run(cache: CachedOutput): CompletableFuture<*>
	{
		clear()
		runCatching {
			generate()
		}.onFailure {
			Archie.LOGGER.error(
				"Data Provider $name failed with exception: ${it.message}\n" +
						"Stacktrace: ${it.stackTraceToString()}"
			)
			if (exitOnError) exitProcess(-1)
		}
		return generateAll(cache)
	}

	fun generateAll(cache: CachedOutput): CompletableFuture<*>
	{
		val futures: Array<CompletableFuture<*>?> = arrayOfNulls(
			generatedModels.size
		)

		for ((i, model) in generatedModels.values.withIndex())
		{
			val target = getPath(model)
			futures[i] = DataProvider.saveStable(cache, model.toJson(), target)
		}

		return CompletableFuture.allOf(*futures)
	}

	protected fun getPath(model: T): Path
	{
		val loc: ResourceLocation = model.location
		return output.getOutputFolder(PackOutput.Target.RESOURCE_PACK).resolve(loc.namespace).resolve("models")
			.resolve(loc.path + ".json")
	}

	companion object
	{
		const val BLOCK_FOLDER: String = "block"
		const val ITEM_FOLDER: String = "item"

		private val GSON: Gson = GsonBuilder().setPrettyPrinting().create()
	}
}
