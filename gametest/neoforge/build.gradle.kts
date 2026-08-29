import dev.kikugie.stonecutter.build.StonecutterBuildExtension
import net.kernelpanicsoft.archie.plugin.bundleRuntimeLibrary
import net.kernelpanicsoft.archie.plugin.runtimeLibrary
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.bundling.Jar

plugins {
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

// Cross-tree references: gametest and core are separate Stonecutter trees, so node.sibling()
// (which only searches within the current tree) doesn't reach core - resolve the path directly.
val coreCommon = rootProject.project(":core:common:${stonecutter.current.version}")
val coreNeoforge = rootProject.project(":core:neoforge:${stonecutter.current.version}")

actualizer {
	actualizes(common)
}

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
	modRuntimeOnly(libs.clothConfig.neoforge)
	modApi(libs.architectury.neoforge)

	implementation(libs.junit.jupiter.api)
	implementation(libs.kotlinx.coroutines.test)
	testImplementation(libs.junit.jupiter.api)
	testRuntimeOnly(libs.junit.jupiter.engine)

	"common"(files(common.tasks.named<Jar>("jar").flatMap { it.archiveFile }))
	api(files(coreNeoforge.tasks.named<Jar>("jar").flatMap { it.archiveFile }))
	modImplementation(libs.storage.neoforge) { exclude(group = "curse.maven") }
	runtimeLibrary(libs.kotlinx.serialization.nbt)
	runtimeLibrary(libs.kotlinx.serialization.toml)
	runtimeLibrary(libs.kotlinx.serialization.json5)
	runtimeLibrary(compose.runtime)
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
		val commonSources = common.tasks.sourcesJar
		dependsOn(commonSources)
		duplicatesStrategy = DuplicatesStrategy.EXCLUDE
		from(commonSources.get().archiveFile.map { zipTree(it) })
	}
}
