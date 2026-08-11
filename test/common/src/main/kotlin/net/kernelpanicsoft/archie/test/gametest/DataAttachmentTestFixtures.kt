package net.kernelpanicsoft.archie.test.gametest

import net.kernelpanicsoft.archie.serialization.AttachmentRegistry
import net.kernelpanicsoft.archie.test.ArchieTest
import net.minecraft.world.entity.Entity

/**
 * Attachment fixtures for [DataAttachmentTests], covering both mechanisms
 * [net.kernelpanicsoft.archie.serialization.ArchieDataAttachment] offers: reactive
 * Entity/BlockEntity sync ([counter], also usable directly against a BlockEntity - a single
 * attachment works on any supported holder kind) and vanilla item-component replication
 * ([label]).
 *
 * Declared as a real [AttachmentRegistry], the same way a consuming mod would - its property
 * initializers (which queue Common Storage Lib `DeferredRegister` entries) run at class-load
 * time, so [AttachmentRegistry.init] (which actually registers them against the mod event bus)
 * must be called separately, at real mod-init time. Same underlying reason as
 * `CapabilityLookupTestFixtures`: this can't be deferred into the `@GameTest` methods themselves.
 */
internal object DataAttachmentTestFixtures : AttachmentRegistry(ArchieTest.MOD_ID)
{
	val counter by intAttachment(sync = true, copyOnDeath = true, default = { 0 })
	val label by stringAttachment(itemComponent = true, default = { "" })
}

/** Exercises `ArchieDataAttachment`'s `getValue`/`setValue` property-delegate contract directly, not just its `get`/`set` methods, matching real consuming-mod usage. */
internal var Entity.testCounter by DataAttachmentTestFixtures.counter
