import dev.kikugie.stonecutter.build.StonecutterBuildExtension
import net.kernelpanicsoft.archie.plugin.bundleRuntimeLibrary
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

// Cross-tree references: gametest and core are separate Stonecutter trees, so node.sibling()
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
		create("gametest") {
			server()
			name = "Minecraft GameTest"
			property("fabric-api.gametest")
			property("archie.gametest", "true")
			property("archie.gametest.side", "server")
			property("archie.gametest.modid", "archie")
		}
		create("gametestClient") {
			client()
			name = "Minecraft GameTest Client"
			property("fabric-api.gametest")
			property("archie.gametest", "true")
			property("archie.gametest.side", "client")
			property("archie.gametest.modid", "archie")
		}
	}
}

dependencies {
	modImplementation(libs.fabric.loader)
	modApi(libs.fabric.api)
	modImplementation(libs.kotlin.fabric)
	compileOnly(libs.kotlinx.serialization)
	modLocalRuntime(libs.clothConfig.fabric)

	implementation(libs.junit.jupiter.api)
	implementation(libs.kotlinx.coroutines.test)
	testImplementation(libs.junit.jupiter.api)
	testRuntimeOnly(libs.junit.jupiter.engine)

	"common"(files(common.tasks.named<Jar>("jar").flatMap { it.archiveFile }))
	api(files(coreFabric.tasks.named<Jar>("jar").flatMap { it.archiveFile }))
	modApi(libs.architectury.fabric)
	modImplementation(libs.storage.common)
	modImplementation(libs.storage.resources.common)
	runtimeLibrary(libs.kotlinx.serialization.nbt)
	runtimeLibrary(libs.kotlinx.serialization.toml)
	runtimeLibrary(libs.kotlinx.serialization.json5)
	runtimeLibrary(compose.runtime)
}

modResources {
	filesMatching.add("fabric.mod.json")
}

tasks {
	base.archivesName.set(base.archivesName.get() + "-gametest-fabric")

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
