import utils.bundleMod
import utils.bundleRuntimeLibrary

plugins {
	alias(libs.plugins.shadow)
	id("utils.kotlin-runtime-library")
	id("utils.mod-resources")
}

architectury {
	platformSetupLoomIde()
	fabric()
}

configurations {
	create("common")
	create("shadowCommon")
	compileClasspath.get().extendsFrom(configurations["common"])
	runtimeClasspath.get().extendsFrom(configurations["common"])
//	getByName("developmentFabric").extendsFrom(configurations["common"])
}

loom {
	accessWidenerPath.set(project(":common").loom.accessWidenerPath)
	runs {
		// This adds a new gradle task that runs the datagen API: "gradlew runDatagen"
		create("datagen") {
			client()
			name = "Minecraft Datagen"
			property("archie.datagen", "true")
			property("archie.datagen.client", project.properties["client_datagen"] as String)
			property("archie.datagen.server", project.properties["server_datagen"] as String)
			property("fabric-api.datagen")
			property("fabric-api.datagen.modid", properties["mod_id"] as String)
			property("fabric-api.datagen.output-dir", file("src/main/generated").absolutePath)

			runDir = "build/datagen"
		}
		create("gametest") {
			server()
			name = "Minecraft GameTest"
			property("fabric-api.gametest")
		}
	}
}

fabricApi.configureDataGeneration {
	createRunConfiguration = false
	outputDirectory.set(file("src/main/generated"))
}

sourceSets {
	main {
		resources {
		}
		kotlin {
			srcDir("src/main/gametest")
		}
		java {
			srcDir("src/main/mixin")
		}
	}
}

dependencies {
	modImplementation(libs.fabric.loader)
	modApi(libs.fabric.api)
	modApi(libs.architectury.fabric)
	modImplementation(libs.kotlin.fabric)
	bundleRuntimeLibrary(libs.kotlinx.serialization.nbt)
	bundleRuntimeLibrary(libs.kotlinx.serialization.toml)
	bundleRuntimeLibrary(libs.kotlinx.serialization.json5)
	bundleRuntimeLibrary(libs.kotlinx.serialization.cbor)
	bundleRuntimeLibrary(compose.runtime)
	modLocalRuntime(libs.rei.fabric)
	modCompileOnlyApi(libs.modmenu)
	modImplementation(libs.catalogue.fabric)
	modLocalRuntime(libs.menulogue.fabric)
	bundleMod(libs.clothConfig.fabric)
	bundleMod(libs.storage.fabric)

	"common"(project(":common", "namedElements")) { isTransitive = false }
	"shadowCommon"(project(":common", "transformProductionFabric")) { isTransitive = false }
}

modResources {
	filesMatching.add("fabric.mod.json")
}

tasks {
	base.archivesName.set(base.archivesName.get() + "-fabric")

	processResources {
		from(project(":common").sourceSets.main.get().resources) {
			include("assets/${project.properties["mod_id"]}/*.png")
		}
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
		val commonSources = project(":common").tasks.sourcesJar
		dependsOn(commonSources)
		duplicatesStrategy = DuplicatesStrategy.EXCLUDE
		from(commonSources.get().archiveFile.map { zipTree(it) })
	}
}


publishing {
	publications.create<MavenPublication>("mavenFabric") {
		artifactId = base.archivesName.get()
		from(components["java"])
	}

	repositories {
		mavenLocal()
		maven {
			val releasesRepoUrl = "https://example.com/releases"
			val snapshotsRepoUrl = "https://example.com/snapshots"
			url = uri(
				if (project.version.toString().endsWith("SNAPSHOT") || project.version.toString()
						.startsWith("0")
				) snapshotsRepoUrl else releasesRepoUrl
			)
			name = "ExampleRepo"
			credentials {
				username = project.properties["repoLogin"]?.toString()
				password = project.properties["repoPassword"]?.toString()
			}
		}
	}
}