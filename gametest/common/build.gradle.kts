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
}
