package net.kernelpanicsoft.archie.gametest.internal.tests

import net.kernelpanicsoft.archie.gui.modifiers.Constraints
import net.kernelpanicsoft.archie.gui.modifiers.position.MarginModifier
import net.kernelpanicsoft.archie.gui.modifiers.position.MarginValues
import net.kernelpanicsoft.archie.gui.modifiers.position.PaddingModifier
import net.kernelpanicsoft.archie.gui.modifiers.position.PaddingValues
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper

object PaddingMarginTests {

	@GameTest
	fun testPaddingModifierReducesConstraints(helper: GameTestHelper) {
		val padding = PaddingValues(left = 10, right = 10, top = 5, bottom = 5)
		val modifier = PaddingModifier(padding)

		val constraints = Constraints(
			minWidth = 100,
			maxWidth = 200,
			minHeight = 50,
			maxHeight = 100
		)

		val modified = modifier.modifyInnerConstraints(constraints)

		assert(modified.maxWidth == 180) { "Expected maxWidth 180, got ${modified.maxWidth}" }
		assert(modified.maxHeight == 90) { "Expected maxHeight 90, got ${modified.maxHeight}" }
		helper.succeed()
	}

	@GameTest
	fun testMarginModifierHorizontal(helper: GameTestHelper) {
		val margin = MarginValues(left = 5, right = 5, top = 3, bottom = 3)
		val modifier = MarginModifier(margin)

		assert(modifier.horizontal == 10) { "Expected horizontal margin 10, got ${modifier.horizontal}" }
		assert(modifier.vertical == 6) { "Expected vertical margin 6, got ${modifier.vertical}" }
		helper.succeed()
	}

	@GameTest
	fun testPaddingValuesGetOffset(helper: GameTestHelper) {
		val padding = PaddingValues(left = 8, right = 12, top = 4, bottom = 6)
		val offset = padding.getOffset()

		assert(offset.x == 8) { "Expected offset x=8, got ${offset.x}" }
		assert(offset.y == 4) { "Expected offset y=4, got ${offset.y}" }
		helper.succeed()
	}

	@GameTest
	fun testPaddingModifierNeverNegative(helper: GameTestHelper) {
		val largePadding = PaddingValues(left = 100, right = 100, top = 100, bottom = 100)
		val modifier = PaddingModifier(largePadding)

		val constraints = Constraints(
			minWidth = 0,
			maxWidth = 50,
			minHeight = 0,
			maxHeight = 50
		)

		val modified = modifier.modifyInnerConstraints(constraints)

		assert(modified.maxWidth >= 0) { "Max width should never be negative" }
		assert(modified.maxHeight >= 0) { "Max height should never be negative" }
		helper.succeed()
	}

	@GameTest
	fun testAsymmetricPadding(helper: GameTestHelper) {
		val padding = PaddingValues(
			left = 5,
			right = 15,
			top = 10,
			bottom = 20
		)
		val modifier = PaddingModifier(padding)

		assert(modifier.horizontal == 20) { "Expected horizontal=20, got ${modifier.horizontal}" }
		assert(modifier.vertical == 30) { "Expected vertical=30, got ${modifier.vertical}" }
		helper.succeed()
	}

	@GameTest
	fun testPaddingMerge(helper: GameTestHelper) {
		val padding1 = PaddingValues(left = 5, top = 5, right = 0, bottom = 0)
		val padding2 = PaddingValues(left = 0, top = 0, right = 5, bottom = 5)

		val merged = padding1 + padding2

		assert(merged.left == 5) { "Expected left=5, got ${merged.left}" }
		assert(merged.right == 5) { "Expected right=5, got ${merged.right}" }
		assert(merged.top == 5) { "Expected top=5, got ${merged.top}" }
		assert(merged.bottom == 5) { "Expected bottom=5, got ${merged.bottom}" }
		helper.succeed()
	}

	@GameTest
	fun testMarginMerge(helper: GameTestHelper) {
		val margin1 = MarginValues(left = 2, top = 2, right = 0, bottom = 0)
		val margin2 = MarginValues(left = 0, top = 0, right = 3, bottom = 3)

		val merged = margin1 + margin2

		assert(merged.left == 2) { "Expected left=2, got ${merged.left}" }
		assert(merged.right == 3) { "Expected right=3, got ${merged.right}" }
		assert(merged.top == 2) { "Expected top=2, got ${merged.top}" }
		assert(merged.bottom == 3) { "Expected bottom=3, got ${merged.bottom}" }
		helper.succeed()
	}

	@GameTest
	fun testPaddingReducesMinConstraints(helper: GameTestHelper) {
		val padding = PaddingValues(left = 10, right = 10, top = 10, bottom = 10)
		val modifier = PaddingModifier(padding)

		val constraints = Constraints(
			minWidth = 50,
			maxWidth = 200,
			minHeight = 50,
			maxHeight = 200
		)

		val modified = modifier.modifyInnerConstraints(constraints)

		assert(modified.minWidth == 30) { "Expected minWidth=30, got ${modified.minWidth}" }
		assert(modified.minHeight == 30) { "Expected minHeight=30, got ${modified.minHeight}" }
		helper.succeed()
	}
}
