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
	// Used by the client GameTest harness (archie-gametest) only, to give ComposeScreen a virtual
	// clock/dispatcher during tests - never on a real player's classpath. compileOnly deliberately.
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
	// catalogue.common deliberately omitted - common source never references it directly.
	modCompileOnly(libs.clothConfig.common)
	// yacl.common deliberately omitted too - unused, and it's actually a Fabric-only build (its
	// version coordinate ends in "-fabric").
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

	// Keep stubUnfulfilledExpects()'s generated throwing-actual stubs out of what gets published -
	// a consumer with both this jar and a real actual on its classpath must only ever see the
	// real one, or Kotlin's actual-resolution can end up preferring the stub.
	jar {
		from(sourceSets.main.get().output)
		exclude("**/*StubKt.class")
	}

	sourcesJar {
		exclude("**/*Stub.kt")
	}
}
