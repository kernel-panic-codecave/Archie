import net.kernelpanicsoft.archie.plugin.bundleMod

plugins {
	alias(libs.plugins.shadow)
	alias(libs.plugins.archie)
}

architectury {
	platformSetupLoomIde()
	fabric()
}

actualizer {
	actualizes(project(":archie-test-common"))
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
	testImplementation(libs.junit.jupiter.api)
	testRuntimeOnly(libs.junit.jupiter.engine)

	"common"(project(":archie-test-common", "namedElements")) { isTransitive = false }
	"shadowCommon"(project(":archie-test-common", "transformProductionFabric")) { isTransitive = false }
	api(project(":archie-core-fabric", "namedElements"))
	api(project(":archie-datagen-fabric", "namedElements"))
	api(project(":archie-gametest-fabric", "namedElements"))
	// See the matching comment in gametest/fabric/build.gradle.kts.
	runtimeOnly(project(":archie-core-common", "namedElements")) { isTransitive = false }
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
		val commonSources = project(":archie-test-common").tasks.sourcesJar
		dependsOn(commonSources)
		duplicatesStrategy = DuplicatesStrategy.EXCLUDE
		from(commonSources.get().archiveFile.map { zipTree(it) })
	}
}
