import org.gradle.api.tasks.testing.logging.TestExceptionFormat

architectury {
	common("fabric", "neoforge")
}

actualizer {
	stubUnfulfilledExpects()
}

loom {
	accessWidenerPath.set(project(":archie-core-common").loom.accessWidenerPath)
}

dependencies {
	api(project(":archie-core-common", "namedElements"))
	modApi(libs.architectury.common)
	modApi(libs.storage.common)
	modApi(libs.storage.resources.common)

	compileOnly(kotlin("reflect"))
	implementation(libs.junit.jupiter.api)
	// Gives ComposeScreen a virtual clock/dispatcher during tests - never on a real player's
	// classpath (archie-gametest is dev/test-only, never shipped in a production jar).
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
