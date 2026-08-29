architectury {
	common("fabric", "neoforge")
}

actualizer {
	stubUnfulfilledExpects()
}

// Cross-tree references: test, core, datagen and gametest are separate Stonecutter trees, so
// node.sibling() (which only searches within the current tree) doesn't reach them - resolve the
// paths directly instead.
val coreCommon = rootProject.project(":core:common:${stonecutter.current.version}")
val datagenCommon = rootProject.project(":datagen:common:${stonecutter.current.version}")
val gametestCommon = rootProject.project(":gametest:common:${stonecutter.current.version}")

// Stonecutter's real projectDir for a node is its `versions/<version>/` folder, two levels below
// this branch's own directory (where the shared `src/` this build script's paths mean actually
// lives) - branchDir undoes that so plain file(...)-style paths below resolve correctly.
val branchDir = projectDir.parentFile.parentFile

loom {
	log4jConfigs.from(rootDir.resolve("log4j-dev.xml"))
	accessWidenerPath = branchDir.resolve("src/main/resources/archie_test.accesswidener")
	enableTransitiveAccessWideners = true
}

dependencies {
	// See core/fabric/build.gradle.kts and datagen/common/build.gradle.kts for why these depend on
	// each sibling's "jar" task output directly rather than through a project(path) reference, and
	// why the Compose/serialization deps are repeated below - files() dependencies carry no
	// transitive module metadata, and core-common's own `api` surface is needed here too.
	api(files(coreCommon.tasks.named<org.gradle.api.tasks.bundling.Jar>("jar").flatMap { it.archiveFile }))
	api(files(datagenCommon.tasks.named<org.gradle.api.tasks.bundling.Jar>("jar").flatMap { it.archiveFile }))
	api(files(gametestCommon.tasks.named<org.gradle.api.tasks.bundling.Jar>("jar").flatMap { it.archiveFile }))
	api(compose.runtime)
	api(libs.kotlinx.serialization)
	api(libs.kotlinx.serialization.json)
	api(libs.kotlinx.serialization.nbt) { isTransitive = false }
	api(libs.kotlinx.serialization.toml) { isTransitive = false }
	api(libs.kotlinx.serialization.json5) { isTransitive = false }
	api(libs.kotlinx.serialization.cbor) { isTransitive = false }

	testImplementation(libs.junit.jupiter.api)
	testImplementation(kotlin("reflect"))
	testRuntimeOnly(libs.junit.jupiter.engine)
	// We depend on fabric loader here to use the fabric @Environment annotations and get the mixin dependencies
	// Do NOT use other classes from fabric loader
	modImplementation(libs.fabric.loader)

	modApi(libs.architectury.common)
	modApi(libs.rei.common)
	modApi(libs.storage.common)
	modApi(libs.storage.resources.common)
}

tasks {
	base.archivesName.set(base.archivesName.get() + "-test-common")

	test {
		testClassesDirs = sourceSets.test.get().output.classesDirs
		classpath = sourceSets.test.get().runtimeClasspath
		useJUnitPlatform()
		systemProperty("archie.junit.gametest", "true")
		systemProperty(
			"archie.junit.gametest.matrix",
			System.getProperty("archie.junit.gametest.matrix") ?: "fabric:server,fabric:client,neoforge:server,neoforge:client",
		)
		systemProperty("archie.junit.gametest.timeoutMinutes", "20")
		systemProperty("archie.junit.gametest.root", rootProject.rootDir.absolutePath)
	}
}
