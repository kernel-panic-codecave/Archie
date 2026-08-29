architectury {
	common("fabric", "neoforge")
}

actualizer {
	stubUnfulfilledExpects()
}

// Cross-tree reference: datagen and core are separate Stonecutter trees, so node.sibling() (which
// only searches within the current tree) doesn't reach core - resolve the path directly instead.
val coreCommon = rootProject.project(":core:common:${stonecutter.current.version}")

loom {
	accessWidenerPath.set(coreCommon.loom.accessWidenerPath)
}

dependencies {
	api(files(coreCommon.tasks.named<Jar>("jar").flatMap { it.archiveFile }))
	api(libs.kotlinx.serialization)
	api(libs.kotlinx.serialization.json)
	api(libs.kotlinx.serialization.nbt) { isTransitive = false }
	api(libs.kotlinx.serialization.toml) { isTransitive = false }
	api(libs.kotlinx.serialization.json5) { isTransitive = false }
	api(libs.kotlinx.serialization.cbor) { isTransitive = false }
	api(compose.runtime)
	modCompileOnly(libs.clothConfig.common)
	compileOnlyApi(libs.cloth.basic.math)
	modApi(libs.architectury.common)
	modApi(libs.storage.common)
	modApi(libs.storage.resources.common)

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
