import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.konan.properties.loadProperties

architectury {
	common("fabric", "neoforge")
}

actualizer {
	stubUnfulfilledExpects()
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


loom {
	log4jConfigs.from(rootDir.resolve("../log4j-dev.xml"))
	accessWidenerPath = file("src/main/resources/${"mod_id".prop}.accesswidener")
}

sourceSets {
	main {
		kotlin {
			srcDir("src/main/gametest")
			srcDir("src/main/datagen")
		}
		java {
			srcDir("src/main/mixin")
		}
	}
}

dependencies {
	compileOnly(kotlin("reflect"))
	implementation(libs.junit.jupiter.api)
	// Used by the client GameTest harness only (AClientGameTestHarness.kt) to give ComposeScreen a
	// virtual clock/dispatcher during tests. compileOnly (not implementation/api) deliberately -
	// this must never end up in the shipped jar. ComposeScreen itself never references
	// kotlinx.coroutines.test.* symbols directly (only AClientGameTestHarness.kt's method bodies
	// do, and those only ever run under AGameTestPlatform.isGameTest), so a real player's game -
	// which never has this on its classpath - never needs to resolve it. Loader modules add it
	// back as runtimeOnly (not bundled - see AGENTS.md) so local `runGametestClient` has it.
	compileOnly(libs.kotlinx.coroutines.test)
	testImplementation(libs.junit.jupiter.api)
	testImplementation(kotlin("reflect"))
	testRuntimeOnly(libs.junit.jupiter.engine)
	api(libs.kotlinx.serialization)
	api(libs.kotlinx.serialization.json)
	api(libs.kotlinx.serialization.nbt) { isTransitive = false }
	api(libs.kotlinx.serialization.toml) { isTransitive = false }
	api(libs.kotlinx.serialization.json5) { isTransitive = false }
	api(libs.kotlinx.serialization.cbor) { isTransitive = false }
	api(compose.runtime)
	// We depend on fabric loader here to use the fabric @Environment annotations and get the mixin dependencies
	// Do NOT use other classes from fabric loader
	modImplementation(libs.fabric.loader)

    modApi(libs.rei.common)
	modCompileOnly(libs.catalogue.common)
	modCompileOnly(libs.clothConfig.common)
	modCompileOnly(libs.yacl.common)
	modApi(libs.architectury.common)
	modApi(libs.storage.common)
	modApi(libs.storage.resources.common)
}

tasks {
	base.archivesName.set(base.archivesName.get() + "-common")

	val verifyGuiSpriteAssets by registering {
		group = "verification"
		description = "Verifies GUI sprite metadata files have matching PNG assets."

		doLast {
			val spritesDir = file("src/main/resources/assets/archie/textures/gui/sprites")
			if (!spritesDir.exists()) return@doLast

			val missingPng = spritesDir
				.walkTopDown()
				.filter { it.isFile && it.name.endsWith(".png.mcmeta") }
				.map { it to file(it.path.removeSuffix(".mcmeta")) }
				.filter { (_, png) -> !png.exists() }
				.map { (meta, _) -> meta.relativeTo(projectDir).invariantSeparatorsPath }
				.toList()

			if (missingPng.isNotEmpty()) {
				val details = missingPng.joinToString(separator = "\n") { " - $it" }
				throw GradleException(
					"Found GUI sprite metadata files without matching PNGs:\n$details"
				)
			}
		}
	}

	named("check") {
		dependsOn(verifyGuiSpriteAssets)
	}

	// Keep stubUnfulfilledExpects()'s generated throwing-actual stubs out of what gets published -
	// a consumer with both this jar and a real actual on its classpath must only ever see the
	// real one, or Kotlin's actual-resolution can end up preferring the stub.
	jar {
		from(sourceSets.main.get().output)
		exclude("**/*StubKt.class")
	}

	sourcesJar {
		exclude("**/*Stub.kt")
	}

	test {
		testClassesDirs = sourceSets.test.get().output.classesDirs
		classpath = sourceSets.test.get().runtimeClasspath
		useJUnitPlatform()
		systemProperty("archie.junit.gametest", "true")
		// Overridable via -Darchie.junit.gametest.matrix=... (a plain JVM system property, not a
		// Gradle project property, since -P doesn't propagate across includeBuild() boundaries in
		// this composite build - a CI job matrix needs to reach both Archie's and Archie-Test's
		// :common:test at once).
		systemProperty(
			"archie.junit.gametest.matrix",
			System.getProperty("archie.junit.gametest.matrix") ?: "fabric:server,fabric:client,neoforge:server,neoforge:client",
		)
		systemProperty("archie.junit.gametest.timeoutMinutes", "20")
		systemProperty("archie.junit.gametest.root", rootProject.rootDir.absolutePath)
		testLogging {
			exceptionFormat = TestExceptionFormat.FULL
		}
	}
}

publishing {
	publications.create<MavenPublication>("mavenCommon") {
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
