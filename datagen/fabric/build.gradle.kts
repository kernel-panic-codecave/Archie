import dev.kikugie.stonecutter.build.StonecutterBuildExtension
import net.kernelpanicsoft.archie.plugin.runtimeLibrary
import org.gradle.api.tasks.bundling.Jar

plugins {
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

// Cross-tree references: datagen and core are separate Stonecutter trees, so node.sibling()
// (which only searches within the current tree) doesn't reach core - resolve the path directly.
val coreCommon = rootProject.project(":core:common:${stonecutter.current.version}")
val coreFabric = rootProject.project(":core:fabric:${stonecutter.current.version}")

actualizer {
	actualizes(common)
}

configurations {
	create("common")
	compileClasspath.get().extendsFrom(configurations["common"])
	runtimeClasspath.get().extendsFrom(configurations["common"])
	testCompileClasspath.get().extendsFrom(compileClasspath.get())
	testRuntimeClasspath.get().extendsFrom(runtimeClasspath.get())
}

loom {
	accessWidenerPath.set(coreCommon.loom.accessWidenerPath)

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
	modLocalRuntime(libs.clothConfig.fabric)

	implementation(libs.junit.jupiter.api)
	testImplementation(libs.junit.jupiter.api)
	testRuntimeOnly(libs.junit.jupiter.engine)

	"common"(files(common.tasks.named<Jar>("jar").flatMap { it.archiveFile }))
	api(files(coreFabric.tasks.named<Jar>("jar").flatMap { it.archiveFile }))
	modApi(libs.architectury.fabric)
	runtimeLibrary(libs.kotlinx.serialization.nbt)
	runtimeLibrary(libs.kotlinx.serialization.toml)
	runtimeLibrary(libs.kotlinx.serialization.json5)
	runtimeLibrary(compose.runtime)
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
		val commonSources = common.tasks.sourcesJar
		dependsOn(commonSources)
		duplicatesStrategy = DuplicatesStrategy.EXCLUDE
		from(commonSources.get().archiveFile.map { zipTree(it) })
	}
}
