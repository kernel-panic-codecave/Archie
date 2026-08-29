import org.gradle.api.tasks.testing.logging.TestExceptionFormat

architectury {
	common("fabric", "neoforge")
}

actualizer {
	stubUnfulfilledExpects()
}

// Cross-tree reference: gametest and core are separate Stonecutter trees, so node.sibling() (which
// only searches within the current tree) doesn't reach core - resolve the path directly instead.
val coreCommon = rootProject.project(":core:common:${stonecutter.current.version}")

loom {
	accessWidenerPath.set(coreCommon.loom.accessWidenerPath)
}

dependencies {
	// See core/fabric/build.gradle.kts and datagen/common/build.gradle.kts for why this depends on
	// core-common's "jar" task output directly, and why the Compose dependency is repeated below.
	api(files(coreCommon.tasks.named<org.gradle.api.tasks.bundling.Jar>("jar").flatMap { it.archiveFile }))
	api(compose.runtime)
	api(libs.kotlinx.serialization)
	api(libs.kotlinx.serialization.json)
	api(libs.kotlinx.serialization.nbt) { isTransitive = false }
	api(libs.kotlinx.serialization.toml) { isTransitive = false }
	api(libs.kotlinx.serialization.json5) { isTransitive = false }
	api(libs.kotlinx.serialization.cbor) { isTransitive = false }
	modApi(libs.architectury.common)
	modApi(libs.storage.common)
	modApi(libs.storage.resources.common)

	compileOnly(kotlin("reflect"))
	implementation(libs.junit.jupiter.api)
	implementation(libs.kotlinx.coroutines.test)
	testImplementation(libs.junit.jupiter.api)
	testImplementation(kotlin("reflect"))
	testRuntimeOnly(libs.junit.jupiter.engine)
}

tasks {
	base.archivesName.set(base.archivesName.get() + "-gametest-common")

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
		// Gradle project property, so a CI job matrix reaches every product's :test at once).
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
