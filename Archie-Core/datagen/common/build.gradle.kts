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
	// modApi, not plain api - the architectury transformer needs archie-core-common as a tracked
	// mod dependency to resolve its own classes (e.g. gui types referenced by datagen providers).
	modApi(project(":archie-core-common"))
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
