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
	// Plain api, explicit "namedElements" target - not modApi. mod* on a project(...) reference
	// makes Loom eagerly read that project's output jar during *configuration*, which can't
	// possibly exist yet on a from-scratch build (mod* is for real remapping needs; this and
	// archie-core-common are already namespace-symmetric, nothing to remap).
	api(project(":archie-core-common", "namedElements"))
	modApi(libs.architectury.common)

	compileOnly(kotlin("reflect"))
	implementation(libs.junit.jupiter.api)
	testImplementation(libs.junit.jupiter.api)
	testImplementation(kotlin("reflect"))
	testRuntimeOnly(libs.junit.jupiter.engine)
}

tasks {
	base.archivesName.set(base.archivesName.get() + "-datagen-common")

	jar {
		from(sourceSets.main.get().output)
		exclude("**/*StubKt.class")
	}

	sourcesJar {
		exclude("**/*Stub.kt")
	}
}
