package net.kernelpanicsoft.archie.gui.composables.containers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import net.kernelpanicsoft.archie.gui.composables.basic.Text
import net.kernelpanicsoft.archie.gui.layout.Alignment
import net.kernelpanicsoft.archie.gui.layout.Layout
import net.kernelpanicsoft.archie.gui.layout.LayoutDirection
import net.kernelpanicsoft.archie.gui.layout.MeasurePolicy
import net.kernelpanicsoft.archie.gui.layout.MeasureResult
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.theme.LocalTheme
import net.minecraft.network.chat.Component
import kotlin.math.max

/** Default gap between columns and between rows, in pixels. */
private const val DEFAULT_COLUMN_SPACING = 6
private const val DEFAULT_ROW_SPACING = 1

/**
 * One column of a [Table].
 *
 * @property header Shown in a header row above the body, or `null` for a column that needs no label.
 *   A table with no headers at all skips the row entirely.
 * @property alignment How cells sit within the column's width. [Alignment.End] is what makes a
 *   column of numbers line up on its last digit, which is most of the reason to reach for a table
 *   over a list of sentences.
 * @property width Fixes the column's width instead of sizing it to its widest cell. Worth setting
 *   only for a column whose content varies enough that self-sizing would make the table jump about
 *   as rows change.
 */
data class TableColumn(
    val header: Component? = null,
    val alignment: Alignment.Horizontal = Alignment.Start,
    val width: Int? = null,
)

/** Restricts the [TableScope.row] DSL to its own receiver scope. */
@DslMarker
annotation class TableDsl

/** Receiver scope for [Table]'s `content` lambda. */
@TableDsl
class TableScope internal constructor() {
    internal val rows = mutableListOf<List<@Composable () -> Unit>>()

    /**
     * Adds a row.
     *
     * Short rows are padded with blanks and long ones are ignored past the last column, so a row
     * that disagrees with the column list degrades into a gap rather than throwing or misaligning
     * everything after it.
     */
    fun row(vararg cells: @Composable () -> Unit) {
        rows += cells.toList()
    }

    internal fun reset() = rows.clear()
}

/**
 * Sizes one axis of a table: [trackCount] tracks, each as large as the largest of the
 * [crossCount] cells crossing it, unless [fixedSize] pins it.
 *
 * This is what makes a table a table rather than a stack of rows - a track is measured against
 * *every* cell in it before anything is placed, so a column of numbers shares an edge no matter
 * how many digits each row has. Used for both axes: columns measured across rows, rows across
 * columns.
 *
 * @param sizeOf The cell's extent along this axis, given (track-crossing index, track index) in
 *   the caller's own order - see the call sites, which swap the arguments between axes.
 */
internal fun tableTrackSizes(
    trackCount: Int,
    crossCount: Int,
    fixedSize: (Int) -> Int?,
    sizeOf: (Int, Int) -> Int,
): IntArray = IntArray(trackCount) { track ->
    fixedSize(track) ?: (0 until crossCount).maxOfOrNull { cross -> sizeOf(cross, track) } ?: 0
}

/** Total extent of [sizes] laid end to end with [spacing] between neighbours - no trailing gap. */
internal fun tableExtent(sizes: IntArray, spacing: Int): Int =
    sizes.sum() + spacing * max(0, sizes.size - 1)

/**
 * A grid whose columns size themselves to their widest cell, so entries line up down the page.
 *
 * The point over a [Column] of pre-formatted lines - `"12 x Iron Ingot"` and friends - is that
 * every column is measured across *all* rows before anything is placed, so numbers right-aligned
 * into their own column share an edge no matter how many digits each has. A line of text cannot do
 * that without the caller padding strings by hand against a proportional font.
 *
 * Cells are ordinary composables, not strings: an icon, a progress bar and a label are all equally
 * valid contents.
 *
 * Sizes to its content rather than filling its parent. Wrap it in a
 * [net.kernelpanicsoft.archie.gui.composables.containers.Scrollable] for a body that may outgrow
 * the room available.
 *
 * @param columns The column specs, left to right. Determines how many cells a row may hold.
 * @param content Declares the rows, in order, via [TableScope.row].
 */
@Composable
fun Table(
    columns: List<TableColumn>,
    modifier: Modifier = Modifier,
    columnSpacing: Int = DEFAULT_COLUMN_SPACING,
    rowSpacing: Int = DEFAULT_ROW_SPACING,
    content: TableScope.() -> Unit,
) {
    if (columns.isEmpty()) return

    val scope = remember { TableScope() }
    scope.reset()
    scope.content()
    val bodyRows = scope.rows.toList()

    val theme = LocalTheme.current
    val hasHeader = columns.any { it.header != null }

    // The header is just another row of cells, so it takes part in column measurement and cannot
    // drift out of alignment with the body beneath it.
    val allRows: List<List<@Composable () -> Unit>> = buildList {
        if (hasHeader) {
            add(columns.map { column ->
                { column.header?.let { Text(it, dropShadow = false, color = theme.darkTextColor) } ?: Unit }
            })
        }
        addAll(bodyRows)
    }
    if (allRows.isEmpty()) return

    val columnCount = columns.size
    // Not remembered: it closes over the column specs and row count, both of which can change with
    // any recomposition that rebuilds the content lambda.
    val measurePolicy = MeasurePolicy { _, measurables, constraints ->
        val cellConstraints = constraints.copy(minWidth = 0, minHeight = 0, maxWidth = Int.MAX_VALUE, maxHeight = Int.MAX_VALUE)
        val placeables = measurables.map { it.measure(cellConstraints) }
        val rowCount = allRows.size

        fun cellAt(row: Int, column: Int) = placeables.getOrNull(row * columnCount + column)

        val columnWidths = tableTrackSizes(columnCount, rowCount, { column -> columns[column].width }) { row, column ->
            cellAt(row, column)?.width ?: 0
        }
        val rowHeights = tableTrackSizes(rowCount, columnCount, { null }) { row, column ->
            cellAt(row, column)?.height ?: 0
        }

        val totalWidth = tableExtent(columnWidths, columnSpacing)
        val totalHeight = tableExtent(rowHeights, rowSpacing)

        MeasureResult(totalWidth, totalHeight) {
            var y = 0
            for (row in 0 until rowCount) {
                var x = 0
                for (column in 0 until columnCount) {
                    val placeable = cellAt(row, column)
                    if (placeable != null) {
                        val offset = columns[column].alignment.align(placeable.width, columnWidths[column], LayoutDirection.Ltr)
                        placeable.placeAt(x + offset, y)
                    }
                    x += columnWidths[column] + columnSpacing
                }
                y += rowHeights[row] + rowSpacing
            }
        }
    }

    Layout(name = "Table", measurePolicy = measurePolicy, modifier = modifier) {
        for (row in allRows) {
            // Row-major, always exactly columnCount cells, so the measure policy can index by
            // (row * columnCount + column) without tracking ragged rows.
            for (column in 0 until columnCount) {
                val cell = row.getOrNull(column)
                if (cell != null) cell() else Text(Component.empty(), dropShadow = false)
            }
        }
    }
}
