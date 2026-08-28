import dev.kikugie.stonecutter.build.StonecutterBuildExtension
import net.kernelpanicsoft.archie.plugin.bundleMod
import net.kernelpanicsoft.archie.plugin.bundleRuntimeLibrary
import net.kernelpanicsoft.archie.plugin.runtimeLibrary

plugins {
	alias(libs.plugins.shadow)
	alias(libs.plugins.archie)
}

architectury {
	platformSetupLoomIde()
	fabric()
}

// Stonecutter's sibling-lookup API (node.sibling(branchName)) replaces the old static
// project(":archie-core-common") reference every one of these was hardcoded to before Stonecutter.
// ProjectNode.project resolves straight to the sibling's Gradle Project - confirmed against
// Stonecutter 0.9.7's own sources (GradleMember.project), not just the older reference template.
val commonNode = requireNotNull(extensions.getByType<StonecutterBuildExtension>().node.sibling("common")) {
	"No common project for $project"
}
val common: Project = commonNode.project

actualizer {
	actualizes(common)
}

configurations {
	create("common")
	create("shadowCommon")
	compileClasspath.get().extendsFrom(configurations["common"])
	runtimeClasspath.get().extendsFrom(configurations["common"])
	testCompileClasspath.get().extendsFrom(compileClasspath.get())
	testRuntimeClasspath.get().extendsFrom(runtimeClasspath.get())
}

loom {
	accessWidenerPath.set(common.loom.accessWidenerPath)

	mods {
		maybeCreate("main").apply {
			sourceSet(sourceSets.main.get())
		}
	}

	runs {
		getByName("client") {
			name = "Minecraft Client"
			source(sourceSets.main.get())
			vmArg("-XX:+AllowEnhancedClassRedefinition")
		}
		getByName("server") {
			name = "Minecraft Server"
			source(sourceSets.main.get())
			vmArgs("-XX:+AllowEnhancedClassRedefinition")
		}
	}
}

dependencies {
	modImplementation(libs.fabric.loader)
	modApi(libs.fabric.api)
	modApi(libs.architectury.fabric)
	modImplementation(libs.kotlin.fabric)
	compileOnly(libs.kotlinx.serialization)
	bundleRuntimeLibrary(libs.kotlinx.serialization)
	bundleRuntimeLibrary(libs.kotlinx.serialization.json)
	bundleRuntimeLibrary(libs.kotlinx.serialization.nbt)
	bundleRuntimeLibrary(libs.kotlinx.serialization.toml)
	bundleRuntimeLibrary(libs.kotlinx.serialization.json5)
	bundleRuntimeLibrary(libs.kotlinx.serialization.cbor)
	bundleRuntimeLibrary(compose.runtime)
	modLocalRuntime(libs.rei.fabric)
	modCompileOnlyApi(libs.modmenu)
	modCompileOnlyApi(libs.catalogue.fabric)
	modLocalRuntime(libs.catalogue.fabric)
	modLocalRuntime(libs.menulogue.fabric)
	modCompileOnlyApi(libs.clothConfig.fabric)
	modLocalRuntime(libs.clothConfig.fabric)
	bundleMod(libs.storage.fabric)

	implementation(libs.junit.jupiter.api)
	testImplementation(libs.junit.jupiter.api)
	testRuntimeOnly(libs.junit.jupiter.engine)
	runtimeLibrary(libs.kotlinx.coroutines.test)

	// Depends directly on common's own "jar" task output (a real zip) rather than through a
	// project(path, configuration) reference or Loom's common()/transformProductionX mechanism -
	// both produce a circular task dependency / broken variant lookup under Stonecutter's nested
	// per-version project paths (confirmed live; not present pre-Stonecutter). A raw SourceSetOutput
	// FileCollection (plain class/resource directories) almost works the same way, but breaks
	// shadowJar - Shadow's copy action expects zip-safe entries, not directories, and throws
	// MissingPropertyException: No such property: mode. Safe here since fabric and neoforge already
	// share one mapping namespace (officialMojangMappings), so transformProductionX's per-platform
	// remap was never doing anything for this project anyway.
	"common"(files(common.tasks.named<org.gradle.api.tasks.bundling.Jar>("jar").flatMap { it.archiveFile }))
	"shadowCommon"(files(common.tasks.named<org.gradle.api.tasks.bundling.Jar>("jar").flatMap { it.archiveFile }))
}

modResources {
	filesMatching.add("fabric.mod.json")
}

tasks {
	base.archivesName.set(base.archivesName.get() + "-fabric")

	test {
		useJUnitPlatform()
	}

	processResources {
		from(common.sourceSets.main.get().resources) {
			include("assets/archie/**")
			include("data/archie/**")
			include("archie-common.mixins.json")
			include("archie.common.json")
			include("archie.accesswidener")
		}
		dependsOn(processTestResources)
	}

	processTestResources {
	}

	classes {
		finalizedBy(testClasses)
	}

	shadowJar {
		configurations =
			listOf(project.configurations.getByName("shadowCommon"), project.configurations.getByName("shadow"))
		archiveClassifier.set("dev-shadow")
	}

	remapJar {
		injectAccessWidener.set(true)
		inputFile.set(shadowJar.get().archiveFile)
		dependsOn(shadowJar)
	}

	jar.get().archiveClassifier.set("dev")

	jar {
		duplicatesStrategy = DuplicatesStrategy.EXCLUDE
		from(common.sourceSets.main.get().output)
	}

	sourcesJar {
		val commonSources = common.tasks.sourcesJar
		dependsOn(commonSources)
		duplicatesStrategy = DuplicatesStrategy.EXCLUDE
		from(commonSources.get().archiveFile.map { zipTree(it) })
	}
}
