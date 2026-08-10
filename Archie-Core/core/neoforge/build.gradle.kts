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

actualizer {
	actualizes(project(":archie-core-common"))
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

	"common"(project(":archie-core-common", "namedElements")) { isTransitive = false }
	"shadowCommon"(project(":archie-core-common", "transformProductionNeoForge")) { isTransitive = false }
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
		from(project(":archie-core-common").sourceSets.main.get().resources) {
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
		from(project(":archie-core-common").sourceSets.main.get().output) {
			// That output is common's own independently-compiled (stub-linked) classes - this
			// module's own sourceSets.main.output already has a correctly-actualized copy of all
			// of them via actualizes(project(":archie-core-common")) above. Exclude so the
			// stub-linked copy can't win the duplicatesStrategy race - it did once, and threw at
			// runtime (see today's Archie/neoforge/build.gradle.kts's matching comment).
			exclude("net/kernelpanicsoft/archie/**")
		}
	}

	sourcesJar {
		val commonSources = project(":archie-core-common").tasks.sourcesJar
		dependsOn(commonSources)
		duplicatesStrategy = DuplicatesStrategy.EXCLUDE
		from(commonSources.get().archiveFile.map { zipTree(it) })
	}
}
