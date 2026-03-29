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
	compileClasspath.get().extendsFrom(configurations["common"])
	runtimeClasspath.get().extendsFrom(configurations["common"])
	testCompileClasspath.get().extendsFrom(compileClasspath.get())
	testRuntimeClasspath.get().extendsFrom(runtimeClasspath.get())
//	getByName("developmentNeoForge").extendsFrom(configurations["common"])
}

loom {
	accessWidenerPath.set(project(":common").loom.accessWidenerPath)

	mods {
		maybeCreate("main").apply {
			sourceSet(project.sourceSets.main.get())
			sourceSet(project(":common").sourceSets.main.get())
		}
		create("test") {
			sourceSet(project.sourceSets.test.get())
			sourceSet(project(":common").sourceSets.test.get())
		}
	}

	runs {
		getByName("client") {
			source(sourceSets.test.get())
			vmArg("-XX:+AllowEnhancedClassRedefinition")
		}
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

//val bundleRuntimeLibrary: Configuration by configurations.creating {
//	exclude(group = "com.mojang")
//	exclude(group = "org.jetbrains.kotlin")
//	exclude(group = "org.jetbrains.kotlinx")
//}

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

	testImplementation(project.project(":common").sourceSets.test.get().output)

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

	processResources {
		from(project(":common").sourceSets.main.get().resources) {
			include("assets/${project.properties["mod_id"]}/**")
		}
		dependsOn(processTestResources)
	}

	processTestResources {
		from(project(":common").sourceSets.test.get().resources) {
			include("assets/${project.properties["mod_id"]}_test/**")
		}
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