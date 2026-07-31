import org.gradle.api.tasks.testing.logging.TestExceptionFormat
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
	log4jConfigs.from(rootDir.resolve("../log4j-dev.xml"))
	accessWidenerPath = file("src/main/resources/${"mod_id".prop}.accesswidener")
	enableTransitiveAccessWideners = true
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
	// namedElements is the mapped-name variant - without it this resolves to the intermediary-mapped
	// variant that remapJar publishes (see fabric/neoforge's "common"(...) dependency for the same fix),
	// which is fine at compile time but breaks reflection over Minecraft types at test runtime.
	api("net.kernelpanicsoft:common") { targetConfiguration = "namedElements" }
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
	base.archivesName.set(base.archivesName.get() + "-common")

	test {
		testClassesDirs = sourceSets.test.get().output.classesDirs
		classpath = sourceSets.test.get().runtimeClasspath
		useJUnitPlatform()
		systemProperty("archie.junit.gametest", "true")
		systemProperty("archie.junit.gametest.matrix", "fabric:server,fabric:client,neoforge:server,neoforge:client")
		systemProperty("archie.junit.gametest.timeoutMinutes", "20")
		systemProperty("archie.junit.gametest.root", rootProject.rootDir.absolutePath)
	}
}
