package net.kernelpanicsoft.archie.registries

import dev.architectury.registry.registries.DeferredRegister
import dev.architectury.registry.registries.RegistrySupplier
import net.minecraft.core.registries.Registries
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block

/**
 * A [RegistryHelper] specialised for [Block] registration that also automatically registers
 * a corresponding [BlockItem] in the item registry.
 *
 * Extend this class for each set of blocks in your mod. Each call to [block] registers both
 * the block and (optionally) its item form.
 *
 * ### Example
 * ```kotlin
 * object MyBlocks : BlockRegistryHelper<Block>(modId) {
 *     val MY_BLOCK by block("my_block") { MyBlock(BlockBehaviour.Properties.of()) }
 * }
 *
 * // In mod init:
 * MyBlocks.init()
 * ```
 *
 * @param T The base block type; must extend [Block].
 * @param modId The mod ID used as the namespace for registered entries.
 */
@Suppress("UNCHECKED_CAST")
open class BlockRegistryHelper<T : Block>(modId: String) : RegistryHelper<T>(
    DeferredRegister.create(modId, Registries.BLOCK) as DeferredRegister<T>,
) {
    /** The [DeferredRegister] for the item registry, used to register [BlockItem]s. */
    open val itemRegistry: DeferredRegister<Item> = DeferredRegister.create(modId, Registries.ITEM)

    override fun init() = super.init().also { itemRegistry.register() }

    /**
     * Registers a block and its associated [BlockItem].
     *
     * The [itemSupplier] defaults to a plain [BlockItem]. Pass `null` to suppress item
     * registration entirely (useful for technical blocks that should not appear in inventories).
     *
     * @param id The registry name for both the block and its item.
     * @param itemSupplier A factory that produces the [BlockItem] given the registered block
     *   and default [Item.Properties]. Pass `null` to skip item registration.
     * @param supplier Factory that produces the block instance.
     * @return A [RegistrySupplier] for the registered block.
     */
    open fun <V : T> block(
        id: String,
        itemSupplier: ((V, Item.Properties) -> BlockItem)? = { block, props -> BlockItem(block, props) },
        supplier: () -> V,
    ): RegistrySupplier<V> {
        val holder = register(id, supplier)
        itemRegistry.register(id) {
            val block = holder.get()
            itemSupplier?.invoke(block, Item.Properties()) ?: BlockItem(block, Item.Properties())
        }
        return holder
    }
}
