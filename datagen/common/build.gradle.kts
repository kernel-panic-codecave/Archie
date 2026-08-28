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
	// A cross-tree api(project(...)) dependency between two "common"-mode (architectury.common(...))
	// projects triggers a circular compileJava<->compileKotlin task dependency under Stonecutter's
	// nested per-version project paths (confirmed live by temporarily removing this line - the
	// cycle disappeared) - same family of issue as the core/fabric<->core/common one, just between
	// two common-mode projects instead of a loader depending on its own common. Depend on
	// core-common's "jar" task output directly instead; see core/fabric/build.gradle.kts.
	//
	// files() dependencies carry no transitive module metadata, unlike the old
	// project(path, "namedElements") dependency this replaces - so core-common's own `api`/`modApi`
	// surface (which datagen-common's code also relies on, e.g. Compose types) has to be repeated
	// here explicitly. Keep this in sync with core/common/build.gradle.kts's own dependencies block.
	api(files(coreCommon.tasks.named<org.gradle.api.tasks.bundling.Jar>("jar").flatMap { it.archiveFile }))
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
