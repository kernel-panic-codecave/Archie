package utils

import net.fabricmc.loom.api.LoomGradleExtensionAPI
import net.fabricmc.loom.util.ModPlatform
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType

// plugin config

interface ModResourcesExtension
{
	val filesMatching: ListProperty<String>
	val versions: MapProperty<String, String>
	val properties: MapProperty<String, String>
}

val extension = extensions.create<ModResourcesExtension>("modResources")
val loom = extensions.getByType<LoomGradleExtensionAPI>()

val versionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
extension.versions.convention(provider {
	val ret = versionCatalog.versionAliases.associate {
		// both "." and "-" cause issues with expand :/
		it.replace(".", "_") to versionCatalog.findVersion(it).get().requiredVersion
	}
	when (loom.platform.get())
	{
		ModPlatform.FABRIC -> ret.mapValues { (_, version) ->
			version
				.replace(",", " ")
				.replace(Regex("""\s+"""), " ")
				.replace(Regex("""\[(\S+)"""), ">=$1")
				.replace(Regex("""(\S+)\]"""), "<=$1")
				.replace(Regex("""\](\S+)"""), ">$1")
				.replace(Regex("""(\S+)\["""), "<$1")
		}
		else -> ret
	}
})
extension.properties.convention(provider {
	project.properties.mapKeys {
		it.key.replace(".", "_")
	}.mapValues { it.value.toString() }
})

// build logic


tasks.withType<ProcessResources>().configureEach {
	exclude(".cache")

	// allow referencing values from libs.versions.toml in Fabric/Forge mod configs
	val resourceValues = buildMap {
		put("versions", extension.versions.get())
		putAll(extension.properties.get())
	}

	// for incremental builds
	inputs.properties(resourceValues)

	filesMatching(extension.filesMatching.get()) {
		expand(resourceValues)
	}
}
