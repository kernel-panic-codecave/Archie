package net.kernelpanicsoft.archie.gui.nodes

import androidx.compose.runtime.AbstractApplier
import net.kernelpanicsoft.archie.gui.layout.LayoutNode

class AUINodeApplier(root: LayoutNode) : AbstractApplier<LayoutNode>(root) {
	override fun insertTopDown(index: Int, instance: LayoutNode) {
		// Ignored, we insert bottom-up.
	}

	override fun insertBottomUp(index: Int, instance: LayoutNode) {
		current.children.add(index, instance)
		current.invalidateChildrenZCache()
		check(instance.parent == null) {
			"$instance must not have a parent when being inserted."
		}
		instance.parent = current
	}

	override fun remove(index: Int, count: Int) {
		repeat(count) {
			current.children.removeAt(index).parent = null
		}
		current.invalidateChildrenZCache()
	}

	override fun move(from: Int, to: Int, count: Int) {
		current.children.move(from, to, count)
		current.invalidateChildrenZCache()
	}

	override fun onClear() {
		current.children.forEach { it.parent = null }
		current.children.clear()
		current.invalidateChildrenZCache()
	}
}