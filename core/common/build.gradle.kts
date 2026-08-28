import org.jetbrains.kotlin.konan.properties.loadProperties

architectury {
	common("fabric", "neoforge")
}

actualizer {
	stubUnfulfilledExpects()
}

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

val branchDir = projectDir.parentFile.parentFile

loom {
	accessWidenerPath = branchDir.resolve("src/main/resources/${"mod_id".prop}.accesswidener")
}


dependencies {
	compileOnly(kotlin("reflect"))
	implementation(libs.junit.jupiter.api)
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
	modImplementation(libs.fabric.loader)

	modApi(libs.rei.common)
	modCompileOnly(libs.clothConfig.common)
	compileOnlyApi(libs.cloth.basic.math)
	modApi(libs.architectury.common)
	modApi(libs.storage.common)
	modApi(libs.storage.resources.common)
}

tasks {
	base.archivesName.set(base.archivesName.get() + "-common")

	val verifyGuiSpriteAssets = register("verifyGuiSpriteAssets") {
		group = "verification"
		description = "Verifies GUI sprite metadata files have matching PNG assets."

		doLast {
			val spritesDir = branchDir.resolve("src/main/resources/assets/archie/textures/gui/sprites")
			if (!spritesDir.exists()) return@doLast

			val missingPng = spritesDir
				.walkTopDown()
				.filter { it.isFile && it.name.endsWith(".png.mcmeta") }
				.map { it to File(it.path.removeSuffix(".mcmeta")) }
				.filter { (_, png) -> !png.exists() }
				.map { (meta, _) -> meta.relativeTo(branchDir).invariantSeparatorsPath }
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

	jar {
		from(sourceSets.main.get().output)
		exclude("**/*StubKt.class")
	}

	sourcesJar {
		exclude("**/*Stub.kt")
	}

	test {
		useJUnitPlatform()
	}
}
