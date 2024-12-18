package com.withertech.archie.gui.nodes

import androidx.compose.runtime.AbstractApplier
import com.withertech.archie.gui.layout.LayoutNode

internal class AUINodeApplier(root: LayoutNode) : AbstractApplier<LayoutNode>(root) {
	override fun insertTopDown(index: Int, instance: LayoutNode) {
		// Ignored, we insert bottom-up.
	}

	override fun insertBottomUp(index: Int, instance: LayoutNode) {
		current.children.add(index, instance)
		check(instance.parent == null) {
			"$instance must not have a parent when being inserted."
		}
		instance.parent = current
	}

	override fun remove(index: Int, count: Int) {
		current.children.remove(index, count)
	}

	override fun move(from: Int, to: Int, count: Int) {
		current.children.move(from, to, count)
	}

	override fun onClear() {
		current.children.clear()
	}
}