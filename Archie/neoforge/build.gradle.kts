import net.kernelpanicsoft.archie.plugin.bundleMod
import net.kernelpanicsoft.archie.plugin.bundleRuntimeLibrary
import net.kernelpanicsoft.archie.plugin.runtimeLibrary
import org.jetbrains.kotlin.konan.properties.loadProperties


plugins {
	alias(libs.plugins.shadow)
	alias(libs.plugins.archie)
}

architectury {
	platformSetupLoomIde()
	neoForge()
}

actualizer {
	actualizes(project(":common"))
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
		create("datagen") {
			data()
			name = "Minecraft Datagen"
			property("archie.datagen", "true")
			property("archie.datagen.client", providers.gradleProperty("client_datagen").orElse("true").get())
			property("archie.datagen.server", providers.gradleProperty("server_datagen").orElse("true").get())
			property("kotlinx.coroutines.debug", "off")
			programArgs("--all", "--mod", providers.gradleProperty("mod_id").orElse("archie").get())
			programArgs("--output", file("src/main/generated").absolutePath)
		}

		create("gametest") {
			server()
			name = "Minecraft GameTest"
			property("neoforge.enableGameTest", "true")
			property("neoforge.gameTestServer", "true")
			property("archie.gametest", "true")
			property("archie.gametest.modid", providers.gradleProperty("mod_id").orElse("archie").get())
			property("kotlinx.coroutines.debug", "off")
			providers.gradleProperty("archie.junit.gametest.function").orNull?.let { property("archie.junit.gametest.function", it) }
		}

		create("gametestClient") {
			client()
			name = "Minecraft GameTest Client"
			property("neoforge.enableGameTest", "true")
			property("archie.gametest.side", "client")
			property("archie.gametest", "true")
			property("archie.gametest.modid", providers.gradleProperty("mod_id").orElse("archie").get())
			property("kotlinx.coroutines.debug", "off")
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
	// dev/test-only, not shipped - see the compileOnly note in common/build.gradle.kts
	runtimeLibrary(libs.kotlinx.coroutines.test)

	"common"(project(":common", "namedElements")) { isTransitive = false }
	"shadowCommon"(project(":common", "transformProductionNeoForge")) { isTransitive = false }
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
		from(project(":common").sourceSets.main.get().output)
	}

	sourcesJar {
		val commonSources = project(":common").tasks.sourcesJar
		dependsOn(commonSources)
		duplicatesStrategy = DuplicatesStrategy.EXCLUDE
		from(commonSources.get().archiveFile.map { zipTree(it) })
	}
}

publishing {
	publications.create<MavenPublication>("mavenNeoForge") {
		artifactId = base.archivesName.get()
		from(components["java"])
	}

	repositories {
		mavenLocal()
		maven {
			name = "Reposilite"
			val releasesUrl = "https://maven.kernelpanicsoft.net/releases"
			val snapshotsUrl = "https://maven.kernelpanicsoft.net/snapshots"

			url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsUrl else releasesUrl)

			credentials {
				username = localProperties?.getProperty("reposilite.username")
					?: System.getenv("REPOSILITE_USERNAME")
				password = localProperties?.getProperty("reposilite.password")
					?: System.getenv("REPOSILITE_PASSWORD")
			}
		}
	}
}