import net.kernelpanicsoft.archie.plugin.bundleMod
import net.kernelpanicsoft.archie.plugin.bundleRuntimeLibrary
import org.jetbrains.kotlin.konan.properties.loadProperties


plugins {
	alias(libs.plugins.shadow)
	alias(libs.plugins.archie)
}

architectury {
	platformSetupLoomIde()
	fabric()
}

actualizer {
	actualizes(project(":common-test"))
	actualizes("net.kernelpanicsoft:common")
}

val localProperties = kotlin.runCatching {
	val localPropsFile = rootDir.resolve("local.properties")
	val sharedPropsFile = rootDir.resolve("../local.properties")
	when {
		localPropsFile.exists() -> loadProperties(localPropsFile.path)
		sharedPropsFile.exists() -> loadProperties(sharedPropsFile.path)
		else -> null
	}
}.getOrNull()

val sharedProperties = kotlin.runCatching {
	val localPropsFile = rootDir.resolve("gradle.properties")
	val sharedPropsFile = rootDir.resolve("../gradle.properties")
	when {
		localPropsFile.exists() -> loadProperties(localPropsFile.path)
		sharedPropsFile.exists() -> loadProperties(sharedPropsFile.path)
		else -> null
	}
}.getOrNull()

val String.prop: String?
	get() = sharedProperties?.get(this)?.toString()

val String.local: String?
	get() = localProperties?.get(this)?.toString()

val String.env: String?
	get() = System.getenv(this)

val String.localOrEnv: String?
	get() = localProperties?.get(this)?.toString() ?: System.getenv(this.uppercase())


configurations {
	create("common")
	create("archie")
	create("shadowCommon")
	compileClasspath.get().extendsFrom(configurations["common"], configurations["archie"])
	runtimeClasspath.get().extendsFrom(configurations["common"], configurations["archie"])
	testCompileClasspath.get().extendsFrom(compileClasspath.get())
	testRuntimeClasspath.get().extendsFrom(runtimeClasspath.get())
//	getByName("developmentFabric").extendsFrom(configurations["common"])
}

loom {
	log4jConfigs.from(project(":common-test").loom.log4jConfigs)
	accessWidenerPath.set(project(":common-test").loom.accessWidenerPath)

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
			property("fabric-api.datagen.modid", providers.gradleProperty("mod_id").orElse("archie").get())
			property("fabric-api.datagen.output-dir", file("src/main/generated").absolutePath)

			runDir = "build/datagen"
		}
		create("gametest") {
			server()
			name = "Minecraft GameTest"
			property("fabric-api.gametest")
			property("archie.gametest", "true")
			property("archie.gametest.side", "server")
		}
		create("gametestClient") {
			client()
			name = "Minecraft GameTest Client"
			property("fabric-api.gametest")
			property("archie.gametest", "true")
			property("archie.gametest.side", "client")
		}
	}
}

fabricApi.configureDataGeneration {
	createRunConfiguration = false
	outputDirectory.set(file("src/main/generated"))
}

sourceSets {
	main {
		resources {
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
	"archie"("net.kernelpanicsoft:fabric") { targetConfiguration = "namedElements" }
	modImplementation(libs.fabric.loader)
	modApi(libs.fabric.api)
	modApi(libs.architectury.fabric)
	modImplementation(libs.kotlin.fabric)
	modLocalRuntime(libs.rei.fabric)
	modLocalRuntime(libs.catalogue.fabric)
	modLocalRuntime(libs.menulogue.fabric)
	modLocalRuntime(libs.clothConfig.fabric)
	bundleMod(libs.storage.fabric)

	"common"(project(":common-test", "namedElements")) { isTransitive = false }
	"common"("net.kernelpanicsoft:common") { targetConfiguration = "namedElements" }
	"shadowCommon"(project(":common-test", "transformProductionFabric")) { isTransitive = false }
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
		from(project(":common-test").sourceSets.main.get().resources) {
			include("assets/${"mod_id".prop}/**")
			include("data/${"mod_id".prop}/**")
			include("${"mod_id".prop}-common.mixins.json")
			include("${"mod_id".prop}.common.json")
			include("${"mod_id".prop}.accesswidener")
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
		val commonSources = project(":common-test").tasks.sourcesJar
		dependsOn(commonSources)
		duplicatesStrategy = DuplicatesStrategy.EXCLUDE
		from(commonSources.get().archiveFile.map { zipTree(it) })
	}
}

//publishing {
//	publications.create<MavenPublication>("mavenFabric") {
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