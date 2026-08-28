import dev.kikugie.stonecutter.build.StonecutterBuildExtension
import net.kernelpanicsoft.archie.plugin.bundleMod
import net.kernelpanicsoft.archie.plugin.bundleRuntimeLibrary
import net.kernelpanicsoft.archie.plugin.runtimeLibrary

plugins {
	alias(libs.plugins.shadow)
	alias(libs.plugins.archie)
}

architectury {
	platformSetupLoomIde()
	neoForge()
}

// See core/fabric/build.gradle.kts for why this goes through node.sibling() rather than a
// hardcoded project path.
val commonNode = requireNotNull(extensions.getByType<StonecutterBuildExtension>().node.sibling("common")) {
	"No common project for $project"
}
val common: Project = commonNode.project

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
	accessWidenerPath.set(common.loom.accessWidenerPath)

	mods {
		maybeCreate("main").apply {
			sourceSet(sourceSets.main.get())
			// actualizer only merges Kotlin expect/actual source into this project's own
			// compilation - plain Java files in archie-core-common (e.g. mixin classes with no
			// actual/expect involvement) never get copied in, so they're invisible to FML's
			// dev-mode module layer unless their sourceSet is also registered here directly.
			sourceSet(common.sourceSets.main.get())
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
	}
}

dependencies {
	neoForge(libs.neoforge)
	modApi(libs.architectury.neoforge)
	implementation(libs.kotlin.neoforge)
	compileOnly(libs.kotlinx.serialization)
	bundleRuntimeLibrary(libs.kotlinx.serialization)
	bundleRuntimeLibrary(libs.kotlinx.serialization.json)
	bundleRuntimeLibrary(libs.kotlinx.serialization.nbt)
	bundleRuntimeLibrary(libs.kotlinx.serialization.toml)
	bundleRuntimeLibrary(libs.kotlinx.serialization.json5)
	bundleRuntimeLibrary(libs.kotlinx.serialization.cbor)
	bundleRuntimeLibrary(compose.runtime)
	// compose.runtime's own transitive deps; Loom's dev-run GAMELIBRARY discovery doesn't walk
	// transitive deps of a bundled library the way production JarJar packaging does, so each needs
	// its own explicit declaration to be visible during runClient/runClientNeoForge.
	bundleRuntimeLibrary(libs.androidx.annotation)
	bundleRuntimeLibrary(libs.androidx.collection)
	bundleRuntimeLibrary(libs.okio)
	modRuntimeOnly(libs.rei.neoforge)
	modCompileOnlyApi(libs.catalogue.neoforge)
	modRuntimeOnly(libs.catalogue.neoforge)
	modCompileOnlyApi(libs.clothConfig.neoforge)
	modRuntimeOnly(libs.clothConfig.neoforge)
	bundleMod(libs.storage.neoforge) {
		exclude(group = "curse.maven")
	}

	implementation(libs.junit.jupiter.api)
	testImplementation(libs.junit.jupiter.api)
	testRuntimeOnly(libs.junit.jupiter.engine)
	runtimeLibrary(libs.kotlinx.coroutines.test)

	// See core/fabric/build.gradle.kts for why this depends on common's "jar" task output directly
	// rather than through a project(path, configuration) reference or a raw SourceSetOutput.
	"common"(files(common.tasks.named<org.gradle.api.tasks.bundling.Jar>("jar").flatMap { it.archiveFile }))
	"shadowCommon"(files(common.tasks.named<org.gradle.api.tasks.bundling.Jar>("jar").flatMap { it.archiveFile }))
}

modResources {
	filesMatching.add("META-INF/neoforge.mods.toml")
}

tasks {
	base.archivesName.set(base.archivesName.get() + "-neoforge")

	test {
		useJUnitPlatform()
	}

	processResources {
		from(common.sourceSets.main.get().resources) {
			include("assets/archie/**")
			include("data/archie/**")
			include("archie-common.mixins.json")
			include("archie.common.json")
			include("archie.accesswidener")
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
