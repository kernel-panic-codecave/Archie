package net.kernelpanicsoft.archie.registries

import dev.architectury.registry.CreativeTabRegistry
import dev.architectury.registry.registries.DeferredRegister
import dev.architectury.registry.registries.RegistrySupplier
import net.minecraft.core.registries.Registries
import net.minecraft.world.item.CreativeModeTab

/**
 * A [RegistryHelper] specialised for [CreativeModeTab] registration via Architectury's
 * [CreativeTabRegistry].
 *
 * ### Example
 * ```kotlin
 * object MyTabs : CreativeTabRegistryHelper<CreativeModeTab>(modId) {
 *     val MY_TAB by create("my_tab") {
 *         title(Component.translatable("itemGroup.mymod.my_tab"))
 *         icon { ItemStack(MyBlocks.MY_BLOCK) }
 *         displayItems { _, output ->
 *             output.accept(MyBlocks.MY_BLOCK)
 *         }
 *     }
 * }
 *
 * // In mod init:
 * MyTabs.init()
 * ```
 *
 * @param T The creative tab type; must extend [CreativeModeTab].
 * @param modId The mod ID used as the namespace for registered entries.
 */
@Suppress("UNCHECKED_CAST")
open class CreativeTabRegistryHelper<T : CreativeModeTab>(modId: String) : RegistryHelper<T>(
    DeferredRegister.create(modId, Registries.CREATIVE_MODE_TAB) as DeferredRegister<T>,
) {
    /**
     * Registers a new [CreativeModeTab] using an Architectury [CreativeTabRegistry] builder.
     *
     * @param id The registry name of the creative tab.
     * @param block A builder lambda applied to [CreativeModeTab.Builder] to configure the tab.
     * @return A [RegistrySupplier] for the registered creative tab.
     */
    open fun <V : T> create(id: String, block: CreativeModeTab.Builder.() -> Unit): RegistrySupplier<V> =
        register<V>(id) { CreativeTabRegistry.create(block) as V }
}
