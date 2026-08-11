import net.kernelpanicsoft.archie.plugin.bundleRuntimeLibrary
import org.gradle.api.tasks.bundling.AbstractArchiveTask

plugins {
	alias(libs.plugins.archie)
}

architectury {
	platformSetupLoomIde()
	neoForge()
}

actualizer {
	actualizes(project(":archie-datagen-common"))
}

// See the dependencies{} block below - project(...) inside dependencies{} resolves to
// DependencyHandler.project(...) (a ProjectDependency), not the real Project, so it can't be
// chased for .tasks there; look it up here instead, where project(...) is still the real Project.
val coreNeoForgeRemapJar = project(":archie-core-neoforge").tasks.named("remapJar", AbstractArchiveTask::class).flatMap { it.archiveFile }

configurations {
	create("common")
	configureEach {
		exclude(group = "thedarkcolour", module = "kotlinforforge-neoforge")
		exclude(group = "remapped.thedarkcolour", module = "kotlinforforge-neoforge-1d1bcbf2")
	}
	compileClasspath.get().extendsFrom(configurations["common"])
	runtimeClasspath.get().extendsFrom(configurations["common"])
	testCompileClasspath.get().extendsFrom(compileClasspath.get())
	testRuntimeClasspath.get().extendsFrom(runtimeClasspath.get())
}

loom {
	accessWidenerPath.set(project(":archie-core-common").loom.accessWidenerPath)

	mods {
		maybeCreate("main").apply {
			sourceSet(sourceSets.main.get())
		}
	}

	runs {
		getByName("client") {
			name = "Minecraft Client"
			source(sourceSets.main.get())
			vmArgs("-XX:+AllowEnhancedClassRedefinition")
			property("kotlinx.coroutines.debug", "off")
		}
		getByName("server") {
			name = "Minecraft Server"
			source(sourceSets.main.get())
			vmArgs("-XX:+AllowEnhancedClassRedefinition")
			property("kotlinx.coroutines.debug", "off")
		}
		create("datagen") {
			data()
			name = "Minecraft Datagen"
			property("archie.datagen", "true")
			property("archie.datagen.client", providers.gradleProperty("client_datagen").orElse("true").get())
			property("archie.datagen.server", providers.gradleProperty("server_datagen").orElse("true").get())
			property("kotlinx.coroutines.debug", "off")
			programArgs("--all", "--mod", "archie_datagen")
			programArgs("--output", file("src/main/generated").absolutePath)
		}
	}
}

dependencies {
	"neoForge"(libs.neoforge)
	implementation(libs.kotlin.neoforge)
	compileOnly(libs.kotlinx.serialization)
	// See the matching comment in gametest/fabric/build.gradle.kts.
	modRuntimeOnly(libs.clothConfig.neoforge)
	// archie-core-neoforge's own namedElements/remapJar (below) is compileOnly and only carries
	// its own classes, not its transitive mod dependencies - declare architectury-api directly
	// (matching core-neoforge's own dependency) so it's genuinely on this project's own runtime
	// classpath too, needed by mixins that reference dev.architectury.platform.Mod.
	modApi(libs.architectury.neoforge)
	// Archie's own mod init (ArchieNeoForge -> Archie.init -> ConfigContainer) touches these at
	// class-load time regardless of what this product actually needs - archie-core-neoforge's own
	// bundleRuntimeLibrary calls only wire up ITS OWN dev-mode "userdev mods and services" locator,
	// which doesn't carry over to a project consuming it as a dependency, so this needs its own
	// copies (matching core/neoforge/build.gradle.kts's set exactly).
	bundleRuntimeLibrary(libs.kotlinx.serialization)
	bundleRuntimeLibrary(libs.kotlinx.serialization.json)
	bundleRuntimeLibrary(libs.kotlinx.serialization.nbt)
	bundleRuntimeLibrary(libs.kotlinx.serialization.toml)
	bundleRuntimeLibrary(libs.kotlinx.serialization.json5)
	bundleRuntimeLibrary(libs.kotlinx.serialization.cbor)
	bundleRuntimeLibrary(compose.runtime)

	implementation(libs.junit.jupiter.api)
	testImplementation(libs.junit.jupiter.api)
	testRuntimeOnly(libs.junit.jupiter.engine)

	"common"(project(":archie-datagen-common", "namedElements")) { isTransitive = false }
	// Compile-only: archie-core-common's/archie-core-neoforge's real classes to compile against.
	// NOT added to the runtime classpath - see the matching comment in
	// gametest/neoforge/build.gradle.kts for why (archie-core-neoforge's own dev jars are
	// production-shaped but not production-complete, and duplicate registration under two
	// different FML-recognized mods breaks NeoForge's per-mod module layer).
	compileOnly(project(":archie-core-common", "namedElements")) { isTransitive = false }
	compileOnly(project(":archie-core-neoforge", "namedElements"))
	modRuntimeOnly(files(coreNeoForgeRemapJar))
}

modResources {
	filesMatching.add("META-INF/neoforge.mods.toml")
}

tasks {
	base.archivesName.set(base.archivesName.get() + "-datagen-neoforge")

	test {
		useJUnitPlatform()
	}

	processResources {
		dependsOn(processTestResources)
	}

	processTestResources {
	}

	classes {
		finalizedBy(testClasses)
	}

	sourcesJar {
		val commonSources = project(":archie-datagen-common").tasks.sourcesJar
		dependsOn(commonSources)
		duplicatesStrategy = DuplicatesStrategy.EXCLUDE
		from(commonSources.get().archiveFile.map { zipTree(it) })
	}
}
