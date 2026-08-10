enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "Archie-Core"

pluginManagement {
	repositories {
		maven("https://maven.fabricmc.net/")
		maven("https://maven.architectury.dev/")
		maven("https://maven.minecraftforge.net/")
		maven("https://maven.neoforged.net/releases/")
		maven("https://maven.firstdarkdev.xyz/releases")
		maven {
			name = "kernelpanic releases"
			url = uri("https://maven.kernelpanicsoft.net/releases")
		}
		maven {
			name = "kernelpanic snapshots"
			url = uri("https://maven.kernelpanicsoft.net/snapshots")
		}
		mavenLocal()
		gradlePluginPortal()
	}
}

dependencyResolutionManagement {
	versionCatalogs {
		create("libs") {
			from(files("../gradle/libs.versions.toml"))
		}
	}
}

// Matches terrarium-earth/Common-Storage-Lib's settings.gradle.kts layout: one nested
// <module>/<platform> directory per platform, flattened into a single-level Gradle project name
// (e.g. core/fabric -> archie-core-fabric). archie-core is today's module; archie-datagen and
// archie-gametest (and eventually the test mod) join later via more includeModule(...) calls,
// without needing any further settings.gradle.kts restructuring.
includeCorePlatform("common")
includeCorePlatform("fabric")
includeCorePlatform("neoforge")

fun includeModule(name: String, platform: String) {
	include("$name/$platform")
	project(":$name/$platform").name = "archie-$name-$platform"
}

fun includeCorePlatform(platform: String) {
	include("core/$platform")
	project(":core/$platform").name = "archie-core-$platform"
}
