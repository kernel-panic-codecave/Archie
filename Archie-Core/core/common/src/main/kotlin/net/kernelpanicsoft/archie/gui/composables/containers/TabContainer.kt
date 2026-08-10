package net.kernelpanicsoft.archie.gui.composables.containers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import net.kernelpanicsoft.archie.gui.composables.basic.Text
import net.kernelpanicsoft.archie.gui.composables.basic.Texture
import net.kernelpanicsoft.archie.gui.composables.input.ButtonCore
import net.kernelpanicsoft.archie.gui.composables.theme.TextureStates
import net.kernelpanicsoft.archie.gui.composables.theme.WidgetState
import net.kernelpanicsoft.archie.gui.layout.Alignment
import net.kernelpanicsoft.archie.gui.layout.Arrangement
import net.kernelpanicsoft.archie.gui.layout.BoxMeasurePolicy
import net.kernelpanicsoft.archie.gui.layout.Column
import net.kernelpanicsoft.archie.gui.layout.Layout
import net.kernelpanicsoft.archie.gui.layout.Renderer
import net.kernelpanicsoft.archie.gui.layout.Row
import net.kernelpanicsoft.archie.gui.layout.dp
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.modifiers.sizeIn
import net.kernelpanicsoft.archie.gui.modifiers.position.padding
import net.kernelpanicsoft.archie.gui.modifiers.position.offset
import net.kernelpanicsoft.archie.gui.modifiers.position.zIndex
import net.kernelpanicsoft.archie.gui.nodes.UINode
import net.kernelpanicsoft.archie.gui.theme.LocalTheme
import net.kernelpanicsoft.archie.gui.theme.SimpleThemeState
import net.kernelpanicsoft.archie.gui.theme.ThemeVariants
import net.kernelpanicsoft.archie.gui.util.extension.drawThemeState
import net.kernelpanicsoft.archie.gui.util.extension.invoke
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.client.gui.GuiGraphics

/** Built-in themed texture keys for [Tab]/[TabContainer], matching vanilla tab styles. */
object TabTextures
{
    /** The in-game pause-menu tab style (e.g. Create World screen). */
    const val GAME = "tab_game"
    /** The main-menu tab style. */
    const val MENU = "tab_menu"
}

private const val SELECTED_ELEVATION_PX = 2
private const val DEFAULT_ICON_SPACING = 4
private const val DEFAULT_CONTENT_SPACING = 6
private const val DEFAULT_CONTENT_WIDTH = 9 * 18

/**
 * Declarative tab bar modeled after the vanilla Create World screen tabs.
 *
 * @param tabs        Ordered list of tab specs to render.
 * @param state       External state holder controlling the selected tab.
 * @param modifier    Modifier applied to the outer container (or scrollable wrapper).
 * @param onTabSelected Callback invoked after a tab becomes selected.
 * @param tabSpacing  Horizontal spacing between neighboring tabs, in pixels.
 * @param scrollable  When true, wraps the tab row in a horizontal [Scrollable] viewport.
 * @param scrollState Optional externally managed [ScrollableState] (only used when [scrollable]).
 * @param contentSpacing Vertical spacing between the tab row and the selected tab content, in pixels.
 */
@Composable
fun TabContainer(
    tabs: List<TabSpec>,
    state: TabContainerState = rememberTabContainerState(tabs),
    modifier: Modifier = Modifier,
    onTabSelected: (TabSpec) -> Unit = {},
    tabSpacing: Int = 2,
    scrollable: Boolean = true,
    scrollState: ScrollableState? = null,
    contentSpacing: Int = DEFAULT_CONTENT_SPACING,
    elevateSelected: Boolean = false,
    tabTexture: String = TabTextures.GAME,
) {
    state.ensureSelection(tabs)

    if (tabs.isEmpty()) {
        if (scrollable) {
            Scrollable(
                direction = ScrollDirection.HORIZONTAL,
                modifier = modifier,
                state = scrollState ?: rememberScrollableState(),
            ) {}
        }
        return
    }

    val rowContent: @Composable (Modifier) -> Unit = { rowModifier ->
        Row(
            modifier = rowModifier,
            horizontalArrangement = Arrangement.spacedBy(tabSpacing.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            tabs.forEach { tab ->
                Tab(
                    spec = tab,
                    selected = state.isSelected(tab.id),
                    texture = tabTexture,
                    elevateSelected = elevateSelected,
                    onClick = {
                        if (!tab.enabled) return@Tab
                        state.select(tab.id)
                        onTabSelected(tab)
                    },
                )
            }
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(contentSpacing.dp),
    ) {
        if (scrollable) {
            Scrollable(
                direction = ScrollDirection.HORIZONTAL,
                state = scrollState ?: rememberScrollableState(),
            ) {
                rowContent(Modifier.padding(horizontal = 4, vertical = 2))
            }
        } else {
            rowContent(Modifier)
        }

        state.selectedTab(tabs)?.content?.let { content ->
            content()
        }
    }
}

/**
 * DSL overload allowing tabs to be declared inline via [TabContainerScope.tab] without
 * manually building a [TabSpec] list.
 *
 * @param contentWrapper Wraps each tab's content composable, e.g. to add common padding.
 *   Defaults to rendering the content unwrapped.
 * @param builder Declares the tabs, in order, via [TabContainerScope.tab].
 */
@Composable
fun TabContainer(
    modifier: Modifier = Modifier,
    state: TabContainerState? = null,
    onTabSelected: (TabSpec) -> Unit = {},
    tabSpacing: Int = 2,
    scrollable: Boolean = true,
    scrollState: ScrollableState? = null,
    contentSpacing: Int = DEFAULT_CONTENT_SPACING,
    contentWrapper: @Composable ((@Composable (() -> Unit)) -> Unit)? = null,
    tabTexture: String = TabTextures.GAME,
    builder: TabContainerScope.() -> Unit,
) {
    val scope = remember { TabContainerScope(contentWrapper = contentWrapper ?: { content -> content()}) }
    scope.reset()
    scope.builder()
    val tabs = scope.build()
    val resolvedState = state ?: rememberTabContainerState(tabs)

    TabContainer(
        tabs = tabs,
        state = resolvedState,
        modifier = modifier,
        onTabSelected = onTabSelected,
        tabSpacing = tabSpacing,
        scrollable = scrollable,
        scrollState = scrollState,
        contentSpacing = contentSpacing,
        tabTexture = tabTexture,
    )
}

/**
 * A [TabContainer] whose selected tab content is wrapped in a [Panel] by default, elevated
 * (drawn above neighboring tabs) when selected. Used by [TabContainerPanel].
 *
 * @param contentWrapper Wraps each tab's content; defaults to a [Panel] offset to sit flush
 *   under the tab row.
 * @param builder Declares the tabs, in order, via [TabContainerScope.tab].
 */
@Composable
fun TabPanel(
    modifier: Modifier = Modifier,
    state: TabContainerState? = null,
    onTabSelected: (TabSpec) -> Unit = {},
    tabSpacing: Int = 2,
    scrollable: Boolean = true,
    scrollState: ScrollableState? = null,
    contentWrapper: @Composable ((@Composable (() -> Unit)) -> Unit)? = null,
    builder: TabContainerScope.() -> Unit,
) {
    val scope = remember { TabContainerScope(contentWrapper = contentWrapper ?: { content ->
        Panel(modifier = Modifier.offset(y = -12)) {
            content()
        }
    }) }
    scope.reset()
    scope.builder()
    val tabs = scope.build()
    val resolvedState = state ?: rememberTabContainerState(tabs)

    TabContainer(
        tabs = tabs,
        state = resolvedState,
        modifier = modifier,
        onTabSelected = onTabSelected,
        tabSpacing = tabSpacing,
        scrollable = scrollable,
        scrollState = scrollState,
        tabTexture = TabTextures.GAME,
        elevateSelected = true,
    )
}

/** Restricts the [TabContainerScope.tab] DSL to its own receiver scope. */
@DslMarker
annotation class TabContainerDsl

/** Receiver scope for the [TabContainer]/[TabPanel] DSL `builder` lambda. */
@TabContainerDsl
class TabContainerScope internal constructor(val contentWrapper: @Composable (@Composable () -> Unit) -> Unit = {it()}) {
    private val specs = mutableListOf<TabSpec>()

    /** Declares a tab with the given [id], [title], and [content]. */
    fun tab(
        id: String,
        title: Component,
        icon: TabIcon? = null,
        enabled: Boolean = true,
        content: @Composable (() -> Unit),
    ) {
        specs += TabSpec(
            id = id,
            title = title,
            icon = icon,
            enabled = enabled,
            content = { contentWrapper(content) },
        )
    }

    /** Declares a tab from a pre-built [TabSpec], bypassing [contentWrapper]. */
    fun tab(spec: TabSpec) {
        specs += spec
    }

    internal fun reset() = specs.clear()

    internal fun build(): List<TabSpec> = specs.toList()
}

/** Data describing a single Create World style tab. */
data class TabSpec(
    val id: String,
    val title: Component,
    val icon: TabIcon? = null,
    val enabled: Boolean = true,
    val content: @Composable (() -> Unit),
)

/** Sprite descriptor used for optional tab icons. */
data class TabIcon(
    val texture: ResourceLocation,
    val uOffset: Float = 0f,
    val vOffset: Float = 0f,
    val regionWidth: Int = 28,
    val regionHeight: Int = 32,
    val textureWidth: Int = 256,
    val textureHeight: Int = 256,
    val displayWidth: Int = regionWidth,
    val displayHeight: Int = regionHeight,
)

/**
 * Tracks which tab id is selected in a [TabContainer]. Create via [rememberTabContainerState].
 */
@Stable
class TabContainerState internal constructor(initialSelectedId: String?) {
    private var selectedId by mutableStateOf(initialSelectedId)

    /** The currently selected tab's id, or `null` if nothing is selected yet. */
    val selectedTabId: String? get() = selectedId

    /** Whether [tabId] is the currently selected tab. */
    fun isSelected(tabId: String): Boolean = selectedId == tabId

    /** Selects the tab with the given [tabId]. */
    fun select(tabId: String) {
        selectedId = tabId
    }

    /**
     * Ensures the selection is valid for [tabs]: falls back to the first enabled tab (or the
     * very first tab, if none are enabled) when there's no selection or the selected id no
     * longer exists/is disabled among [tabs].
     */
    fun ensureSelection(tabs: List<TabSpec>) {
        if (tabs.isEmpty()) {
            selectedId = null
            return
        }
        val current = selectedId
        val active = current?.let { id -> tabs.firstOrNull { it.id == id && it.enabled } }
        if (active != null) return
        selectedId = tabs.firstOrNull { it.enabled }?.id ?: tabs.first().id
    }

    /** The index of the selected tab within [tabs], or -1 if none is selected. */
    fun selectedIndex(tabs: List<TabSpec>): Int =
        tabs.indexOfFirst { it.id == selectedId }

    /** The [TabSpec] currently selected within [tabs], or `null` if none is selected. */
    fun selectedTab(tabs: List<TabSpec>): TabSpec? =
        tabs.firstOrNull { it.id == selectedId }
}

/** Creates and remembers a [TabContainerState], initially selecting [initialSelectedId]. */
@Composable
fun rememberTabContainerState(
    tabs: List<TabSpec>,
    initialSelectedId: String? = tabs.firstOrNull { it.enabled }?.id,
): TabContainerState = remember(initialSelectedId) { TabContainerState(initialSelectedId) }

/**
 * A single clickable tab button, rendering [spec]'s icon/title and switching its themed
 * texture state based on [selected]/hover/press. Used internally by [TabContainer]; use that
 * (or the DSL/[TabPanel] variants) rather than calling this directly in most cases.
 */
@Composable
fun Tab(
	spec: TabSpec,
	selected: Boolean,
	modifier: Modifier = Modifier,
	elevateSelected: Boolean = false,
	enabled: Boolean = spec.enabled,
	texture: String = TabTextures.GAME,
	variant: String = ThemeVariants.DEFAULT,
	iconSpacing: Int = DEFAULT_ICON_SPACING,
	onClick: (TabSpec) -> Unit,
) {
    val theme = LocalTheme.current
    val composableTheme = theme.getComposableTheme(texture)
    val measurePolicy = remember { BoxMeasurePolicy(Alignment.CenterStart) }

    ButtonCore(
        onClick = { onClick(spec) },
        enabled = enabled,
        modifier = modifier,
    ) { isHovered, isPressed ->
        val stateKey = WidgetState.resolve(
            composableTheme, variant,
            WidgetState.clicked(selected || isPressed), WidgetState.hovered(isHovered),
            enabled = enabled,
        )
        val state = composableTheme.getState(stateKey, variant)
        val offsetModifier = Modifier
            .zIndex(if (selected && elevateSelected) 1f else 0f)
            .offset(x = 0, y = if (selected && !elevateSelected) -SELECTED_ELEVATION_PX else 0)
            .padding(horizontal = 10, vertical = 6)
        val sizeModifier = if (!composableTheme.isNineslice) {
            val defaultState = composableTheme.states[TextureStates.DEFAULT] as SimpleThemeState
            Modifier.sizeIn(minWidth = defaultState.width, minHeight = defaultState.height)
        } else Modifier

        Layout(
            name = "Tab",
            measurePolicy = measurePolicy,
            renderer = object : Renderer {
                override fun render(
                    node: UINode,
                    x: Int,
                    y: Int,
                    guiGraphics: GuiGraphics,
                    mouseX: Int,
                    mouseY: Int,
                    partialTick: Float,
                ) = guiGraphics {
                    node.renderState = stateKey
                    drawThemeState(state, x, y, node.width, node.height)
                }
            },
            modifier = sizeModifier.then(offsetModifier),
        ) {
            Row(
                modifier = Modifier,
                horizontalArrangement = Arrangement.spacedBy(iconSpacing.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                spec.icon?.let { icon ->
                    Texture(
                        loc = icon.texture,
                        uOffset = icon.uOffset,
                        vOffset = icon.vOffset,
                        u = icon.regionWidth,
                        v = icon.regionHeight,
                        textureWidth = icon.textureWidth,
                        textureHeight = icon.textureHeight,
                        modifier = Modifier.sizeIn(
                            minWidth = icon.displayWidth,
                            minHeight = icon.displayHeight,
                        ),
                    )
                }
                Text(
                    text = spec.title,
                    color = if (selected) theme.darkTextColor else theme.lightTextColor,
                    dropShadow = !selected
                )
            }
        }
    }
}
