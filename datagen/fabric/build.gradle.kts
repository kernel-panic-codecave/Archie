import net.kernelpanicsoft.archie.plugin.bundleRuntimeLibrary

plugins {
	alias(libs.plugins.archie)
}

architectury {
	platformSetupLoomIde()
	fabric()
}

actualizer {
	actualizes(project(":archie-datagen-common"))
}

configurations {
	create("common")
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
			property("fabric-api.datagen.modid", "archie_datagen")
			property("fabric-api.datagen.output-dir", file("src/main/generated").absolutePath)

			runDir = "build/datagen"
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
	modImplementation(libs.kotlin.fabric)
	compileOnly(libs.kotlinx.serialization)
	// See the matching comment in gametest/fabric/build.gradle.kts.
	modLocalRuntime(libs.clothConfig.fabric)
	// Archie's own mod init (ArchieFabric -> Archie.init -> ConfigContainer) touches these at
	// class-load time regardless of what this product actually needs - archie-core-fabric's own
	// bundleRuntimeLibrary calls only cover ITS OWN dev-mode run, which doesn't carry over to a
	// project consuming it as a dependency, so this needs its own copies (matching
	// core/fabric/build.gradle.kts's set exactly).
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
	api(project(":archie-core-fabric", "namedElements"))
	// See the matching comment in gametest/fabric/build.gradle.kts.
	runtimeOnly(project(":archie-core-common", "namedElements")) { isTransitive = false }
}

modResources {
	filesMatching.add("fabric.mod.json")
}

tasks {
	base.archivesName.set(base.archivesName.get() + "-datagen-fabric")

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
