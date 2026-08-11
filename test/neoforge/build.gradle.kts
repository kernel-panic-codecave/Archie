import net.kernelpanicsoft.archie.plugin.bundleMod
import org.gradle.api.tasks.bundling.AbstractArchiveTask

plugins {
	alias(libs.plugins.shadow)
	alias(libs.plugins.archie)
}

architectury {
	platformSetupLoomIde()
	neoForge()
}

actualizer {
	actualizes(project(":archie-test-common"))
}

// See the dependencies{} block below - project(...) inside dependencies{} resolves to
// DependencyHandler.project(...) (a ProjectDependency), not the real Project, so it can't be
// chased for .tasks there; look these up here instead, where project(...) is still the real
// Project. Each of these products' own "namedElements" dev jar is production-shaped but not
// production-complete (their shadowJar-merged classes are missing until a real build), so this
// project depends on their real remapJar output for its own runtime classpath instead.
val coreNeoForgeRemapJar = project(":archie-core-neoforge").tasks.named("remapJar", AbstractArchiveTask::class).flatMap { it.archiveFile }
val datagenNeoForgeRemapJar = project(":archie-datagen-neoforge").tasks.named("remapJar", AbstractArchiveTask::class).flatMap { it.archiveFile }
val gametestNeoForgeRemapJar = project(":archie-gametest-neoforge").tasks.named("remapJar", AbstractArchiveTask::class).flatMap { it.archiveFile }

configurations {
	create("common")
	create("shadowCommon")
	configureEach {
		// Keep NeoForge Kotlin runtime provided by KotlinLangForge only.
		exclude(group = "thedarkcolour", module = "kotlinforforge-neoforge")
		exclude(group = "remapped.thedarkcolour", module = "kotlinforforge-neoforge-1d1bcbf2")
	}
	compileClasspath.get().extendsFrom(configurations["common"])
	runtimeClasspath.get().extendsFrom(configurations["common"])
	testCompileClasspath.get().extendsFrom(compileClasspath.get())
	testRuntimeClasspath.get().extendsFrom(runtimeClasspath.get())
}

loom {
	log4jConfigs.from(project(":archie-test-common").loom.log4jConfigs)
	accessWidenerPath.set(project(":archie-test-common").loom.accessWidenerPath)

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
			property("kotlinx.coroutines.debug", "off")
			vmArgs("-XX:+AllowEnhancedClassRedefinition")
		}
		create("datagen") {
			data()
			name = "Minecraft Datagen"
			property("archie.datagen", "true")
			property("archie.datagen.client", providers.gradleProperty("client_datagen").orElse("true").get())
			property("archie.datagen.server", providers.gradleProperty("server_datagen").orElse("true").get())
			property("kotlinx.coroutines.debug", "off")
			programArgs("--all", "--mod", "archie_test")
			programArgs("--output", file("src/main/generated").absolutePath)
		}

		create("gametest") {
			server()
			name = "Minecraft GameTest"
			property("neoforge.enableGameTest", "true")
			property("neoforge.gameTestServer", "true")
			property("archie.gametest", "true")
			property("archie.gametest.modid", "archie_test")
			property("kotlinx.coroutines.debug", "off")
			providers.gradleProperty("archie.junit.gametest.function").orNull?.let { property("archie.junit.gametest.function", it) }
		}

		create("gametestClient") {
			client()
			name = "Minecraft GameTest Client"
			property("neoforge.enableGameTest", "true")
			property("archie.gametest.side", "client")
			property("archie.gametest", "true")
			property("archie.gametest.modid", "archie_test")
			property("kotlinx.coroutines.debug", "off")
			providers.gradleProperty("archie.junit.gametest.function").orNull?.let { property("archie.junit.gametest.function", it) }
		}
	}
}

sourceSets {
	main {
		resources {
			srcDir("src/main/generated")
		}
	}
}

dependencies {
	"neoForge"(libs.neoforge)
	modApi(libs.architectury.neoforge)
	implementation(libs.kotlin.neoforge)
	modRuntimeOnly(libs.rei.neoforge)
	modRuntimeOnly(libs.catalogue.neoforge)
	modRuntimeOnly(libs.clothConfig.neoforge)
	bundleMod(libs.storage.neoforge) { exclude(group = "curse.maven") }

	implementation(libs.junit.jupiter.api)
	testImplementation(libs.junit.jupiter.api)
	testRuntimeOnly(libs.junit.jupiter.engine)

	"common"(project(":archie-test-common", "namedElements")) { isTransitive = false }
	"shadowCommon"(project(":archie-test-common", "transformProductionNeoForge")) { isTransitive = false }
	// Compile-only: real classes to compile against, kept off the runtime classpath - see the
	// matching comment in gametest/neoforge/build.gradle.kts for why (each of these products' own
	// dev jar is production-shaped but not production-complete, and duplicate registration under
	// two different FML-recognized mods breaks NeoForge's per-mod module layer).
	compileOnly(project(":archie-core-common", "namedElements")) { isTransitive = false }
	compileOnly(project(":archie-core-neoforge", "namedElements"))
	compileOnly(project(":archie-datagen-neoforge", "namedElements"))
	compileOnly(project(":archie-gametest-neoforge", "namedElements"))
	modRuntimeOnly(files(coreNeoForgeRemapJar))
	modRuntimeOnly(files(datagenNeoForgeRemapJar))
	modRuntimeOnly(files(gametestNeoForgeRemapJar))
}

modResources {
	filesMatching.add("META-INF/neoforge.mods.toml")
}

tasks {
	base.archivesName.set(base.archivesName.get() + "-test-neoforge")

	test {
		useJUnitPlatform()
	}

	processResources {
		from(project(":archie-test-common").sourceSets.main.get().resources) {
			include("assets/archie_test/**")
			include("data/archie_test/**")
			include("archie_test.common.json")
			include("archie_test.accesswidener")
		}
		dependsOn(processTestResources)
	}

	processTestResources {
	}

	classes {
		finalizedBy(testClasses)
	}

	shadowJar {
		exclude("fabric.mod.json")
		configurations =
			listOf(project.configurations.getByName("shadowCommon"), project.configurations.getByName("shadow"))
		archiveClassifier.set("dev-shadow")
	}

	remapJar {
		inputFile.set(shadowJar.get().archiveFile)
		atAccessWideners.set(setOf(loom.accessWidenerPath.get().asFile.name))
		dependsOn(shadowJar)
	}

	jar.get().archiveClassifier.set("dev")

	jar {
		duplicatesStrategy = DuplicatesStrategy.EXCLUDE
		from(project(":archie-test-common").sourceSets.main.get().output)
	}

	sourcesJar {
		val commonSources = project(":archie-test-common").tasks.sourcesJar
		dependsOn(commonSources)
		duplicatesStrategy = DuplicatesStrategy.EXCLUDE
		from(commonSources.get().archiveFile.map { zipTree(it) })
	}
}
