import org.jetbrains.kotlin.konan.properties.loadProperties

architectury {
	common("fabric", "neoforge")
}

actualizer {
	stubUnfulfilledExpects()
}

val localProperties = kotlin.runCatching {
	val localPropsFile = rootDir.resolve("local.properties")
	val sharedPropsFile = rootDir.resolve("../local.properties")
	when {
		localPropsFile.exists() -> loadProperties(localPropsFile.path)
		sharedPropsFile.exists() -> loadProperties(sharedPropsFile.path)
		else -> null
	}
}.getOrNull()

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

val String.local: String?
	get() = localProperties?.get(this)?.toString()

val String.env: String?
	get() = System.getenv(this)

val String.localOrEnv: String?
	get() = localProperties?.get(this)?.toString() ?: System.getenv(this.uppercase())



loom {
	log4jConfigs.from(rootProject.file("log4j-dev.xml"))
//	accessWidenerPath = file("src/main/resources/${"mod_id".prop}.accesswidener")
}

sourceSets {
	main {
		kotlin {
			srcDir("src/main/gametest")
			srcDir("src/main/datagen")
		}
		java {
			srcDir("src/main/mixin")
		}
	}
}

dependencies {
	api("net.kernelpanicsoft:common")
	compileOnly(libs.kotlinx.serialization)
	compileOnly(libs.kotlinx.serialization.json)
	compileOnly(kotlin("reflect"))
	implementation(libs.junit.jupiter.api)
	testImplementation(libs.junit.jupiter.api)
	testImplementation(libs.kotlinx.serialization)
	testImplementation(kotlin("reflect"))
	testRuntimeOnly(libs.junit.jupiter.engine)
	testRuntimeOnly(libs.kotlinx.serialization)
	testRuntimeOnly(libs.kotlinx.serialization.json)
	api(libs.kotlinx.serialization.nbt) { isTransitive = false }
	api(libs.kotlinx.serialization.toml) { isTransitive = false }
	api(libs.kotlinx.serialization.json5) { isTransitive = false }
	api(libs.kotlinx.serialization.cbor) { isTransitive = false }
	api(compose.runtime)
	// We depend on fabric loader here to use the fabric @Environment annotations and get the mixin dependencies
	// Do NOT use other classes from fabric loader
	modImplementation(libs.fabric.loader)

	modApi(libs.rei.common)
	modCompileOnly(libs.catalogue.common)
	modCompileOnly(libs.clothConfig.common)
	modCompileOnly(libs.yacl.common)
	modApi(libs.architectury.common)
	modApi(libs.storage.common)
	modApi(libs.storage.resources.common)
}

tasks {
	base.archivesName.set(base.archivesName.get() + "-archie-test")
}
