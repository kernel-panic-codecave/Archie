import net.fabricmc.loom.api.LoomGradleExtensionAPI
import net.fabricmc.loom.util.ModPlatform
import net.kernelpanicsoft.archie.plugin.bundleMod
import net.kernelpanicsoft.archie.plugin.bundleRuntimeLibrary
import org.jetbrains.compose.compose


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
	configureEach {
		// Keep NeoForge Kotlin runtime provided by KotlinLangForge only.
		exclude(group = "thedarkcolour", module = "kotlinforforge-neoforge")
		exclude(group = "remapped.thedarkcolour", module = "kotlinforforge-neoforge-1d1bcbf2")
	}
	compileClasspath.get().extendsFrom(configurations["common"])
	runtimeClasspath.get().extendsFrom(configurations["common"])
	testCompileClasspath.get().extendsFrom(compileClasspath.get())
	testRuntimeClasspath.get().extendsFrom(runtimeClasspath.get())
//	getByName("developmentNeoForge").extendsFrom(configurations["common"])
}

loom {
	log4jConfigs.from(project(":common").loom.log4jConfigs)
	accessWidenerPath.set(project(":common").loom.accessWidenerPath)

	mods {
		maybeCreate("main").apply {
			sourceSet(project.sourceSets.main.get())
//			sourceSet(project(":common").sourceSets.main.get())
		}
		create("test") {
			sourceSet(project.sourceSets.test.get())
//			sourceSet(project(":common").sourceSets.test.get())
		}
	}

	runs {
		getByName("client") {
			name = "Minecraft Client"
			source(sourceSets.main.get())
			source(sourceSets.test.get())
			vmArgs("-XX:+AllowEnhancedClassRedefinition")
		}
		getByName("server") {
			name = "Minecraft Server"
			source(sourceSets.main.get())
			source(sourceSets.test.get())
			vmArgs("-XX:+AllowEnhancedClassRedefinition")
		}
		create("datagen") {
			data()
			name = "Minecraft Datagen"
			property("archie.datagen", "true")
			property("archie.datagen.client", providers.gradleProperty("client_datagen").orElse("true").get())
			property("archie.datagen.server", providers.gradleProperty("server_datagen").orElse("true").get())
			programArgs("--all", "--mod", providers.gradleProperty("mod_id").orElse("archie").get())
			programArgs("--output", file("src/main/generated").absolutePath)
		}

		create("gametest") {
			server()
			name = "Minecraft GameTest"
			property("neoforge.enableGameTest", "true")
			property("neoforge.gameTestServer", "true")
			providers.gradleProperty("archie.junit.gametest.function").orNull?.let { property("archie.junit.gametest.function", it) }
		}

		create("gametestClient") {
			client()
			name = "Minecraft GameTest Client"
			property("neoforge.enableGameTest", "true")
			property("archie.gametest.side", "client")
			providers.gradleProperty("archie.junit.gametest.function").orNull?.let { property("archie.junit.gametest.function", it) }
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

//val bundleRuntimeLibrary: Configuration by configurations.creating {
//	exclude(group = "com.mojang")
//	exclude(group = "org.jetbrains.kotlin")
//	exclude(group = "org.jetbrains.kotlinx")
//}

dependencies {
	"neoForge"(libs.neoforge)
	modApi(libs.architectury.neoforge)
	implementation(libs.kotlin.neoforge)
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
	modCompileOnlyApi(libs.yacl.neoforge)
//	modRuntimeOnly(libs.yacl.neoforge)
//	modRuntimeOnly(libs.quilt.parsers.json)
//	modRuntimeOnly(libs.quilt.parsers.gson)
//	runtimeOnly(libs.quilt.parsers.json)
//	runtimeOnly(libs.quilt.parsers.gson)
	bundleMod(libs.storage.neoforge) {
		exclude(group = "curse.maven")
	}

	implementation(libs.junit.jupiter.api)
	testImplementation(libs.junit.jupiter.api)
	testRuntimeOnly(libs.junit.jupiter.engine)

	"common"(project(":common", "namedElements")) { isTransitive = false }
	"shadowCommon"(project(":common", "transformProductionNeoForge")) { isTransitive = false }
//	bundleRuntimeLibrary.resolvedConfiguration.resolvedArtifacts.forEach {
//		include(it.moduleVersion.id.toString())
//		implementation(it.moduleVersion.id.toString())
//		localRuntime(it.moduleVersion.id.toString()) {
//			attributes {
//				attribute(patchedFMLModType, true)
//			}
//		}
//	}
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
		from(project(":common").sourceSets.main.get().resources) {
			include("assets/${project.properties["mod_id"]}/**")
			include("data/${project.properties["mod_id"]}/**")
			include("archie-common.mixins.json")
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
//		atAccessWideners.set(setOf(loom.accessWidenerPath.get().asFile.path))
		dependsOn(shadowJar)
	}

	jar.get().archiveClassifier.set("dev")

	jar {
		duplicatesStrategy = DuplicatesStrategy.EXCLUDE
		from(project(":common").sourceSets.main.get().output)
	}

	sourcesJar {
		val commonSources = project(":common").tasks.sourcesJar
		dependsOn(commonSources)
		duplicatesStrategy = DuplicatesStrategy.EXCLUDE
		from(commonSources.get().archiveFile.map { zipTree(it) })
	}

//	task("printRuntimeClasspath") {
//		val runtimeClasspath = sourceSets.test.get().runtimeClasspath
//		inputs.files( runtimeClasspath )
//		doLast {
//			println(runtimeClasspath.joinToString("\n") { it.path })
//		}
//	}
}

//publishing {
//	publications.create<MavenPublication>("mavenNeoForge") {
//		artifactId = base.archivesName.get()
//		from(components["java"])
//	}
//
//	repositories {
//		mavenLocal()
//		maven {
//			val releasesRepoUrl = "https://example.com/releases"
//			val snapshotsRepoUrl = "https://example.com/snapshots"
//			url = uri(
//				if (project.version.toString().endsWith("SNAPSHOT") || project.version.toString()
//						.startsWith("0")
//				) snapshotsRepoUrl else releasesRepoUrl
//			)
//			name = "ExampleRepo"
//			credentials {
//				username = project.properties["repoLogin"]?.toString()
//				password = project.properties["repoPassword"]?.toString()
//			}
//		}
//	}
//}