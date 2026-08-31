import dev.kikugie.stonecutter.build.StonecutterBuildExtension
import net.kernelpanicsoft.archie.plugin.bundleMod
import net.kernelpanicsoft.archie.plugin.runtimeLibrary
import org.gradle.api.tasks.bundling.Jar

plugins {
	alias(libs.plugins.shadow)
	alias(libs.plugins.archie)
}

architectury {
	platformSetupLoomIde()
	neoForge()
}

// Same-tree sibling. See core/fabric/build.gradle.kts for why node.sibling() is used here.
val commonNode = requireNotNull(extensions.getByType<StonecutterBuildExtension>().node.sibling("common")) {
	"No common project for $project"
}
val common: Project = commonNode.project

// Cross-tree references: test, core, datagen and gametest are separate Stonecutter trees, so
// node.sibling() (which only searches within the current tree) doesn't reach them - resolve the
// paths directly instead.
val coreNeoforge = rootProject.project(":core:neoforge:${stonecutter.current.version}")
val datagenNeoforge = rootProject.project(":datagen:neoforge:${stonecutter.current.version}")
val gametestNeoforge = rootProject.project(":gametest:neoforge:${stonecutter.current.version}")

actualizer {
	actualizes(common)
}

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
	log4jConfigs.from(common.loom.log4jConfigs)
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
	modLocalRuntime(libs.rei.neoforge)
	modRuntimeOnly(libs.catalogue.neoforge)
	modRuntimeOnly(libs.clothConfig.neoforge)
	bundleMod(libs.storage.neoforge) { exclude(group = "curse.maven") }

	implementation(libs.junit.jupiter.api)
	implementation(libs.kotlinx.coroutines.test)
	testImplementation(libs.junit.jupiter.api)
	testRuntimeOnly(libs.junit.jupiter.engine)

	// See core/fabric/build.gradle.kts and gametest/neoforge/build.gradle.kts for why these depend
	// on the sibling's "jar" task output directly, and why the Compose/coroutines deps above and
	// below are repeated - the actualizer merges test-common's own source files into this project's
	// own compilation, so it needs test-common's compile-time deps directly too, not just its output.
	"common"(files(common.tasks.named<Jar>("jar").flatMap { it.archiveFile }))
	"shadowCommon"(files(common.tasks.named<Jar>("jar").flatMap { it.archiveFile }))
	api(files(coreNeoforge.tasks.named<Jar>("jar").flatMap { it.archiveFile }))
	api(files(datagenNeoforge.tasks.named<Jar>("jar").flatMap { it.archiveFile }))
	api(files(gametestNeoforge.tasks.named<Jar>("jar").flatMap { it.archiveFile }))
	// files() dependencies carry no runtime GAMELIBRARY discovery either - the serialization format
	// add-ons core-neoforge bundles at runtime (nbt/toml/json5, needed by Archie's own Config system
	// at init) don't propagate, so they're repeated here too. Confirmed missing live: a
	// NoClassDefFoundError for io.github.xn32.json5k.ConfigBuilder when actually launching this
	// project.
	runtimeLibrary(libs.kotlinx.serialization.nbt)
	runtimeLibrary(libs.kotlinx.serialization.toml)
	runtimeLibrary(libs.kotlinx.serialization.json5)
	runtimeLibrary(compose.runtime)
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
		from(common.sourceSets.main.get().resources) {
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
		from(common.sourceSets.main.get().output)
	}

	sourcesJar {
		val commonSources = common.tasks.sourcesJar
		dependsOn(commonSources)
		duplicatesStrategy = DuplicatesStrategy.EXCLUDE
		from(commonSources.get().archiveFile.map { zipTree(it) })
	}
}
