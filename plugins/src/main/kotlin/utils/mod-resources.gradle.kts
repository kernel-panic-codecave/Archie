package utils

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

val versionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
extension.versions.convention(provider {
	versionCatalog.versionAliases.associate {
		// both "." and "-" cause issues with expand :/
		it.replace(".", "_") to versionCatalog.findVersion(it).get().requiredVersion
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
