import org.gradle.api.tasks.bundling.AbstractArchiveTask

plugins {
	alias(libs.plugins.archie)
}

architectury {
	platformSetupLoomIde()
	neoForge()
}

actualizer {
	actualizes(project(":archie-gametest-common"))
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
		create("gametest") {
			server()
			name = "Minecraft GameTest"
			property("neoforge.enableGameTest", "true")
			property("neoforge.gameTestServer", "true")
			property("archie.gametest", "true")
			property("archie.gametest.modid", "archie")
			property("kotlinx.coroutines.debug", "off")
		}
		create("gametestClient") {
			client()
			name = "Minecraft GameTest Client"
			property("neoforge.enableGameTest", "true")
			property("archie.gametest.side", "client")
			property("archie.gametest", "true")
			property("archie.gametest.modid", "archie")
			property("kotlinx.coroutines.debug", "off")
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

	implementation(libs.junit.jupiter.api)
	testImplementation(libs.junit.jupiter.api)
	testRuntimeOnly(libs.junit.jupiter.engine)

	"common"(project(":archie-gametest-common", "namedElements")) { isTransitive = false }
	// Compile-only: archie-core-common's real classes for archie-gametest-common's source to
	// compile against. NOT added to the runtime classpath (unlike the "common" config above) -
	// its classes are also physically present in coreNeoForgeRemapJar below (merged in via
	// archie-core-neoforge's shadowJar), and having both on the runtime classpath means two
	// different FML-registered mods ("archie_gametest" folding in these raw classes, "archie" from
	// the real jar) would each claim the same packages, which NeoForge's module layer rejects.
	compileOnly(project(":archie-core-common", "namedElements")) { isTransitive = false }
	// Compile-only for the same reason as above: archie-core-neoforge's own "namedElements" dev jar
	// is production-*shaped* but not production-*complete* (it lacks archie-core-common's classes,
	// only merged in via archie-core-neoforge's shadowJar, which dev mode skips), so mixin prepare
	// fails "not found" at runtime despite compiling fine against it. Depend on its real remapJar
	// output for the actual dev-mode run classpath instead (files(), not project(...), so this
	// stays lazy/task-output-driven and doesn't hit the "mod* on a project reference reads the jar
	// during configuration" issue namedElements avoids elsewhere in this build) - same jar a real
	// downstream consumer would use, so its mixin refmap and merged classes are both genuinely
	// complete. Kept off the runtime classpath alongside namedElements above, to avoid archie-core-
	// neoforge's own classes appearing twice (dev jar + remapJar) at runtime.
	compileOnly(project(":archie-core-neoforge", "namedElements"))
	modRuntimeOnly(files(coreNeoForgeRemapJar))
}

modResources {
	filesMatching.add("META-INF/neoforge.mods.toml")
}

tasks {
	base.archivesName.set(base.archivesName.get() + "-gametest-neoforge")

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
		val commonSources = project(":archie-gametest-common").tasks.sourcesJar
		dependsOn(commonSources)
		duplicatesStrategy = DuplicatesStrategy.EXCLUDE
		from(commonSources.get().archiveFile.map { zipTree(it) })
	}
}
