package com.withertech.archie.test

import com.withertech.archie.serialization.NBTHolder
import earth.terrarium.botarium.context.ItemContext
import earth.terrarium.botarium.item.util.ItemProvider
import earth.terrarium.botarium.resources.item.ItemResource
import earth.terrarium.botarium.storage.base.CommonStorage
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level

class TestItem(properties: Properties) : Item(properties), ItemProvider.Item
{
	override fun use(level: Level, player: Player, usedHand: InteractionHand): InteractionResultHolder<ItemStack>
	{
		val data = Data(player.getItemInHand(usedHand))
		data.test++
		if (level.isClientSide)
			player.sendSystemMessage(Component.literal("Num: ${data.test}"))
		return InteractionResultHolder.consume(player.getItemInHand(usedHand))
	}

	override fun getItems(stack: ItemStack, context: ItemContext): CommonStorage<ItemResource>
	{
		val data = Data(stack)
		return data.items
	}

	class Data(stack: ItemStack) : NBTHolder by NBTHolder.item(stack)
	{
		var test by intField()
		val items by itemField(9)
	}
}