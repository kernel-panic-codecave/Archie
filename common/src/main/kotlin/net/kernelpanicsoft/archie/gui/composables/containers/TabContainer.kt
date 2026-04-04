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
import net.kernelpanicsoft.archie.gui.layout.Alignment
import net.kernelpanicsoft.archie.gui.layout.Arrangement
import net.kernelpanicsoft.archie.gui.layout.BoxMeasurePolicy
import net.kernelpanicsoft.archie.gui.layout.Layout
import net.kernelpanicsoft.archie.gui.layout.Renderer
import net.kernelpanicsoft.archie.gui.layout.Row
import net.kernelpanicsoft.archie.gui.layout.dp
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.modifiers.sizeIn
import net.kernelpanicsoft.archie.gui.modifiers.position.padding
import net.kernelpanicsoft.archie.gui.modifiers.position.offset
import net.kernelpanicsoft.archie.gui.nodes.AUINode
import net.kernelpanicsoft.archie.gui.theme.LocalTheme
import net.kernelpanicsoft.archie.gui.theme.SimpleThemeState
import net.kernelpanicsoft.archie.gui.util.KColor
import net.kernelpanicsoft.archie.gui.util.extension.drawThemeState
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.client.gui.GuiGraphics

private const val DEFAULT_TAB_TEXTURE = "tab"
private const val SELECTED_ELEVATION_PX = 2
private const val DEFAULT_ICON_SPACING = 4

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
                    onClick = {
                        if (!tab.enabled) return@Tab
                        state.select(tab.id)
                        onTabSelected(tab)
                    },
                )
            }
        }
    }

    if (scrollable) {
        Scrollable(
            direction = ScrollDirection.HORIZONTAL,
            modifier = modifier,
            state = scrollState ?: rememberScrollableState(),
        ) {
            rowContent(Modifier.padding(horizontal = 4, vertical = 2))
        }
    } else {
        rowContent(modifier)
    }
}

/** Data describing a single Create World style tab. */
data class TabSpec(
    val id: String,
    val title: Component,
    val icon: TabIcon? = null,
    val enabled: Boolean = true,
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

@Stable
class TabContainerState internal constructor(initialSelectedId: String?) {
    private var selectedId by mutableStateOf(initialSelectedId)

    val selectedTabId: String? get() = selectedId

    fun isSelected(tabId: String): Boolean = selectedId == tabId

    fun select(tabId: String) {
        selectedId = tabId
    }

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

    fun selectedIndex(tabs: List<TabSpec>): Int =
        tabs.indexOfFirst { it.id == selectedId }

    fun selectedTab(tabs: List<TabSpec>): TabSpec? =
        tabs.firstOrNull { it.id == selectedId }
}

@Composable
fun rememberTabContainerState(
    tabs: List<TabSpec>,
    initialSelectedId: String? = tabs.firstOrNull { it.enabled }?.id,
): TabContainerState = remember(initialSelectedId) { TabContainerState(initialSelectedId) }

@Composable
fun Tab(
    spec: TabSpec,
    selected: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = spec.enabled,
    texture: String = DEFAULT_TAB_TEXTURE,
    indicatorColor: KColor = KColor.ofRgb(0xFCD472),
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
        val stateKey = when {
            !enabled -> TextureStates.DISABLED
            selected && isHovered -> TextureStates.CLICKED_AND_HOVERED
            selected || isPressed -> TextureStates.CLICKED
            isHovered -> TextureStates.HOVERED
            else -> TextureStates.DEFAULT
        }
        val state = composableTheme.getState(stateKey, theme.mode)
        val offsetModifier = Modifier
            .offset(x = 0, y = if (selected) -SELECTED_ELEVATION_PX else 0)
            .padding(horizontal = 10, vertical = 6)
        val sizeModifier = if (!composableTheme.isNinepatch) {
            val defaultState = composableTheme.states[TextureStates.DEFAULT] as? SimpleThemeState
            if (defaultState != null) Modifier.sizeIn(minWidth = defaultState.width, minHeight = defaultState.height) else Modifier
        } else Modifier

        Layout(
            name = "Tab",
            measurePolicy = measurePolicy,
            renderer = object : Renderer {
                override fun render(
                    node: AUINode,
                    x: Int,
                    y: Int,
                    guiGraphics: GuiGraphics,
                    mouseX: Int,
                    mouseY: Int,
                    partialTick: Float,
                ) {
                    guiGraphics.drawThemeState(state, x, y, node.width, node.height)
                    if (selected) {
                        guiGraphics.fill(
                            x,
                            y,
                            x + node.width,
                            y + SELECTED_ELEVATION_PX,
                            indicatorColor.argb,
                        )
                    }
                    super.render(node, x, y, guiGraphics, mouseX, mouseY, partialTick)
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
                    color = if (selected) theme.lightTextColor else theme.darkTextColor,
                )
            }
        }
    }
}
