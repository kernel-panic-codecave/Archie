package net.kernelpanicsoft.archie.test.data

import net.kernelpanicsoft.archie.Archie
import net.kernelpanicsoft.archie.data.ADataGenerator
import net.kernelpanicsoft.archie.data.ADatagenEventObject
import net.kernelpanicsoft.archie.data.client.ALanguageProvider
import net.kernelpanicsoft.archie.data.client.model.ABlockStateProvider
import net.kernelpanicsoft.archie.data.client.model.AItemModelProvider
import net.kernelpanicsoft.archie.test.ArchieTest
import net.kernelpanicsoft.archie.test.BlockRegistry
import net.kernelpanicsoft.archie.test.ItemRegistry

/** Datagen for the test showcase blocks/items so resource files can be regenerated automatically. */
internal object ArchieTestDatagen : ADatagenEventObject(ArchieTest.MOD) {
    override fun ADataGenerator.handler() {
        client {
            // Write test showcase assets into the archie namespace because test registries use Archie.MOD.
            blockStates { output ->
                object : ABlockStateProvider(output, Archie.MOD, false) {
                    override fun generate() {
                        simpleBlockWithItem(BlockRegistry.TestBlock)
                    }
                }
            }

            itemModels { output ->
                object : AItemModelProvider(output, Archie.MOD, false) {
                    override fun generate() {
                        withExistingParent("test_item", "minecraft:item/compass")
                    }
                }
            }

            languages { output ->
                object : ALanguageProvider(output, Archie.MOD, false, "en_us") {
                    override fun generate() {
                        add(BlockRegistry.TestBlock, "Compose Test Block")
                        add(ItemRegistry.TestItem, "Compose Test Item")
                    }
                }
            }
        }
    }
}


