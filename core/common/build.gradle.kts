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

loom {
	accessWidenerPath = file("src/main/resources/${"mod_id".prop}.accesswidener")
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
	// Used only for the fabric @Environment annotations + mixin deps. Do NOT use other classes
	// from fabric loader from common code.
	modImplementation(libs.fabric.loader)

	modApi(libs.rei.common)
	modCompileOnly(libs.clothConfig.common)
	// Cloth Config's own transitive dependency, kept visible at compile time only (like Cloth
	// Config itself) since the config system exposes `Color` directly in its own public API -
	// NOT bundled: Cloth Config's own distributed jar already jar-in-jars this and exports
	// `me.shedaniel.math` itself, so embedding a second copy makes NeoForge's ModLauncher refuse
	// to even build its module layer ("Modules basic.math and cloth_config export package
	// me.shedaniel.math") the moment both are present - confirmed by actually hitting that crash.
	// [ColorSerializer]/[SColor] stay gated behind isClothConfigLoaded instead, same as ModifierKeyCode.
	compileOnlyApi(libs.cloth.basic.math)
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
