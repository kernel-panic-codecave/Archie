import net.kernelpanicsoft.archie.plugin.bundleMod
import net.kernelpanicsoft.archie.plugin.bundleRuntimeLibrary
import net.kernelpanicsoft.archie.plugin.runtimeLibrary

plugins {
	alias(libs.plugins.shadow)
	alias(libs.plugins.archie)
}

architectury {
	platformSetupLoomIde()
	fabric()
}

actualizer {
	actualizes(project(":archie-core-common"))
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
	}
}

dependencies {
	modImplementation(libs.fabric.loader)
	modApi(libs.fabric.api)
	modApi(libs.architectury.fabric)
	modImplementation(libs.kotlin.fabric)
	compileOnly(libs.kotlinx.serialization)
	bundleRuntimeLibrary(libs.kotlinx.serialization)
	bundleRuntimeLibrary(libs.kotlinx.serialization.json)
	bundleRuntimeLibrary(libs.kotlinx.serialization.nbt)
	bundleRuntimeLibrary(libs.kotlinx.serialization.toml)
	bundleRuntimeLibrary(libs.kotlinx.serialization.json5)
	bundleRuntimeLibrary(libs.kotlinx.serialization.cbor)
	bundleRuntimeLibrary(compose.runtime)
	modLocalRuntime(libs.rei.fabric)
	modCompileOnlyApi(libs.modmenu)
	modCompileOnlyApi(libs.catalogue.fabric)
	modLocalRuntime(libs.catalogue.fabric)
	modLocalRuntime(libs.menulogue.fabric)
	modCompileOnlyApi(libs.clothConfig.fabric)
	modLocalRuntime(libs.clothConfig.fabric)
	bundleMod(libs.storage.fabric)

	implementation(libs.junit.jupiter.api)
	testImplementation(libs.junit.jupiter.api)
	testRuntimeOnly(libs.junit.jupiter.engine)
	runtimeLibrary(libs.kotlinx.coroutines.test)

	"common"(project(":archie-core-common", "namedElements")) { isTransitive = false }
	"shadowCommon"(project(":archie-core-common", "transformProductionFabric")) { isTransitive = false }
}

modResources {
	filesMatching.add("fabric.mod.json")
}

tasks {
	base.archivesName.set(base.archivesName.get() + "-fabric")

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
		val commonSources = project(":archie-core-common").tasks.sourcesJar
		dependsOn(commonSources)
		duplicatesStrategy = DuplicatesStrategy.EXCLUDE
		from(commonSources.get().archiveFile.map { zipTree(it) })
	}
}
