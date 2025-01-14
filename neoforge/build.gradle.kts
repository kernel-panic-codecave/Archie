import net.kernelpanicsoft.archie.plugin.bundleMod
import net.kernelpanicsoft.archie.plugin.bundleRuntimeLibrary


plugins {
	alias(libs.plugins.shadow)
	alias(libs.plugins.archie)
}

architectury {
	platformSetupLoomIde()
	neoForge()
}

configurations {
	create("common")
	create("shadowCommon")
	compileClasspath.get().extendsFrom(configurations["common"])
	runtimeClasspath.get().extendsFrom(configurations["common"])
//	getByName("developmentNeoForge").extendsFrom(configurations["common"])
}


loom {
	accessWidenerPath.set(project(":common").loom.accessWidenerPath)

	// NroForge Datagen Gradle config.  Remove if not using NeoForge datagen
	runs {
		create("datagen") {
			data()
			property("archie.datagen", "true")
			property("archie.datagen.client", project.properties["client_datagen"] as String)
			property("archie.datagen.server", project.properties["server_datagen"] as String)
			programArgs("--all", "--mod", project.properties["mod_id"] as String)
			programArgs("--output", file("src/main/generated").absolutePath)
		}

		create("gametest") {
			server()
			name = "Minecraft GameTest"
			property("neoforge.enableGameTest", "true")
			property("neoforge.gameTestServer", "true")
		}
	}

}

sourceSets {
	main {
		resources {
			srcDir("src/main/generated")
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
	compileOnly(libs.kotlin.stdlib)
	neoForge(libs.neoforge)
	modApi(libs.architectury.neoforge)
	implementation(libs.kotlin.neoforge) {
		exclude(group = "net.neoforged.fancymodloader", module = "loader")
	}
	bundleRuntimeLibrary(libs.kotlinx.serialization.nbt)
	bundleRuntimeLibrary(libs.kotlinx.serialization.toml)
	bundleRuntimeLibrary(libs.kotlinx.serialization.json5)
	bundleRuntimeLibrary(libs.kotlinx.serialization.cbor)
	bundleRuntimeLibrary(compose.runtime)
	modRuntimeOnly(libs.rei.neoforge)
	modImplementation(libs.catalogue.neoforge)
	bundleMod(libs.clothConfig.neoforge)
	bundleMod(libs.storage.neoforge) {
		exclude(group = "curse.maven")
	}

	"common"(project(":common", "namedElements")) { isTransitive = false }
	"shadowCommon"(project(":common", "transformProductionNeoForge")) { isTransitive = false }
}

modResources {
	filesMatching.add("META-INF/neoforge.mods.toml")
}

tasks {
	base.archivesName.set(base.archivesName.get() + "-neoforge")

	processResources {
		from(project(":common").sourceSets.main.get().resources) {
			include("assets/${project.properties["mod_id"]}/**")
		}
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

	sourcesJar {
		val commonSources = project(":common").tasks.sourcesJar
		dependsOn(commonSources)
		duplicatesStrategy = DuplicatesStrategy.EXCLUDE
		from(commonSources.get().archiveFile.map { zipTree(it) })
	}
}

publishing {
	publications.create<MavenPublication>("mavenNeoForge") {
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