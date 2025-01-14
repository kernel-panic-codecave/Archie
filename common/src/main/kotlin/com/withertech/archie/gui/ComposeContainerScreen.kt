package com.withertech.archie.gui

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.snapshots.Snapshot
import com.withertech.archie.gui.layout.Alignment
import com.withertech.archie.gui.layout.Box
import com.withertech.archie.gui.layout.LayoutNode
import com.withertech.archie.gui.modifiers.Constraints
import com.withertech.archie.gui.modifiers.Modifier
import com.withertech.archie.gui.modifiers.fillMaxSize
import com.withertech.archie.gui.nodes.AUINodeApplier
import kotlinx.coroutines.*
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.AbstractContainerMenu
import kotlin.coroutines.CoroutineContext

abstract class ComposeContainerScreen<T : AbstractContainerMenu>(
	menu: T, playerInventory: Inventory, title: Component
) : AbstractContainerScreen<T>(menu, playerInventory, title), CoroutineScope
{

	private var hasFrameWaiters = false
	private val clock = BroadcastFrameClock { hasFrameWaiters = true }

	private val composeScope = CoroutineScope(Dispatchers.Default) + clock
	final override val coroutineContext: CoroutineContext = composeScope.coroutineContext

	private val rootNode = LayoutNode()

	private val recomposer = Recomposer(coroutineContext)
	private val composition = Composition(AUINodeApplier(rootNode), recomposer)

	private var applyScheduled = false
	private val snapshotHandle = Snapshot.registerGlobalWriteObserver {
		if (!applyScheduled) {
			applyScheduled = true
			composeScope.launch {
				applyScheduled = false
				Snapshot.sendApplyNotifications()
			}
		}
	}

	protected fun start(content: @Composable () -> Unit)
	{
		AUIScopeManager.scopes += composeScope
		launch {
			recomposer.runRecomposeAndApplyChanges()
		}

		setContent {
			Box(
				modifier = Modifier.fillMaxSize(),
				contentAlignment = Alignment.Center
			) {
				content()
			}
		}
	}

	private fun setContent(content: @Composable () -> Unit)
	{
		composition.setContent {
			content()
		}
	}

	open fun renderNodes(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float)
	{
		if (hasFrameWaiters)
		{
			hasFrameWaiters = false
			clock.sendFrame(System.nanoTime()) // Frame time value is not used by Compose runtime.
		}
		rootNode.measure(Constraints(maxWidth = width, maxHeight = height))
		rootNode.render(rootNode.x, rootNode.y, guiGraphics, mouseX, mouseY, partialTick)
	}

	override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float)
	{
		super.render(guiGraphics, mouseX, mouseY, partialTick)
		renderTooltip(guiGraphics, mouseX, mouseY)
	}

	override fun renderBg(guiGraphics: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int)
	{
		renderNodes(guiGraphics, mouseX, mouseY, partialTick)
	}

	override fun onClose()
	{
		super.onClose()
		recomposer.close()
		snapshotHandle.dispose()
		composition.dispose()
		composeScope.cancel()
	}
}