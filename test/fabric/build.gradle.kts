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
	fabric()
}

// Same-tree sibling. See core/fabric/build.gradle.kts for why node.sibling() is used here.
val commonNode = requireNotNull(extensions.getByType<StonecutterBuildExtension>().node.sibling("common")) {
	"No common project for $project"
}
val common: Project = commonNode.project

// Cross-tree references: test, core, datagen and gametest are separate Stonecutter trees, so
// node.sibling() (which only searches within the current tree) doesn't reach them - resolve the
// paths directly instead.
val coreCommon = rootProject.project(":core:common:${stonecutter.current.version}")
val coreFabric = rootProject.project(":core:fabric:${stonecutter.current.version}")
val datagenFabric = rootProject.project(":datagen:fabric:${stonecutter.current.version}")
val gametestFabric = rootProject.project(":gametest:fabric:${stonecutter.current.version}")

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
			vmArg("-XX:+AllowEnhancedClassRedefinition")
		}
		getByName("server") {
			name = "Minecraft Server"
			source(sourceSets.main.get())
			vmArgs("-XX:+AllowEnhancedClassRedefinition")
		}
		// This adds a new gradle task that runs the datagen API: "gradlew runDatagen"
		create("datagen") {
			client()
			name = "Minecraft Datagen"
			property("archie.datagen", "true")
			property("archie.datagen.client", providers.gradleProperty("client_datagen").orElse("true").get())
			property("archie.datagen.server", providers.gradleProperty("server_datagen").orElse("true").get())
			property("fabric-api.datagen")
			property("fabric-api.datagen.modid", "archie_test")
			property("fabric-api.datagen.output-dir", file("src/main/generated").absolutePath)

			runDir = "build/datagen"
		}
		create("gametest") {
			server()
			name = "Minecraft GameTest"
			property("fabric-api.gametest")
			property("archie.gametest", "true")
			property("archie.gametest.side", "server")
			property("archie.gametest.modid", "archie_test")
		}
		create("gametestClient") {
			client()
			name = "Minecraft GameTest Client"
			property("fabric-api.gametest")
			property("archie.gametest", "true")
			property("archie.gametest.side", "client")
			property("archie.gametest.modid", "archie_test")
		}
	}
}

fabricApi.configureDataGeneration {
	createRunConfiguration = false
	outputDirectory.set(file("src/main/generated"))
}

dependencies {
	modImplementation(libs.fabric.loader)
	modApi(libs.fabric.api)
	modApi(libs.architectury.fabric)
	modImplementation(libs.kotlin.fabric)
	modLocalRuntime(libs.rei.fabric)
	modLocalRuntime(libs.catalogue.fabric)
	modLocalRuntime(libs.menulogue.fabric)
	modLocalRuntime(libs.clothConfig.fabric)
	bundleMod(libs.storage.fabric)

	implementation(libs.junit.jupiter.api)
	implementation(libs.kotlinx.coroutines.test)
	testImplementation(libs.junit.jupiter.api)
	testRuntimeOnly(libs.junit.jupiter.engine)

	// See core/fabric/build.gradle.kts and gametest/fabric/build.gradle.kts for why these depend on
	// the sibling's "jar" task output directly, and why the Compose/storage/coroutines deps above
	// and below are repeated - the actualizer merges test-common's own source files into this
	// project's own compilation, so it needs test-common's compile-time deps directly too, not just
	// its output.
	"common"(files(common.tasks.named<Jar>("jar").flatMap { it.archiveFile }))
	"shadowCommon"(files(common.tasks.named<Jar>("jar").flatMap { it.archiveFile }))
	api(files(coreFabric.tasks.named<Jar>("jar").flatMap { it.archiveFile }))
	api(files(datagenFabric.tasks.named<Jar>("jar").flatMap { it.archiveFile }))
	api(files(gametestFabric.tasks.named<Jar>("jar").flatMap { it.archiveFile }))
	runtimeOnly(files(coreCommon.tasks.named<Jar>("jar").flatMap { it.archiveFile }))
	modImplementation(libs.storage.common)
	modImplementation(libs.storage.resources.common)
	// files() dependencies carry no runtime GAMELIBRARY discovery either - the serialization format
	// add-ons core-fabric bundles at runtime (nbt/toml/json5, needed by Archie's own Config system
	// at init) don't propagate, so they're repeated here too. Confirmed missing live: a
	// NoClassDefFoundError for io.github.xn32.json5k.ConfigBuilder when actually launching this
	// project.
	runtimeLibrary(libs.kotlinx.serialization.nbt)
	runtimeLibrary(libs.kotlinx.serialization.toml)
	runtimeLibrary(libs.kotlinx.serialization.json5)
	runtimeLibrary(compose.runtime)
}

modResources {
	filesMatching.add("fabric.mod.json")
}

tasks {
	base.archivesName.set(base.archivesName.get() + "-test-fabric")

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

	sourcesJar {
		val commonSources = common.tasks.sourcesJar
		dependsOn(commonSources)
		duplicatesStrategy = DuplicatesStrategy.EXCLUDE
		from(commonSources.get().archiveFile.map { zipTree(it) })
	}
}
