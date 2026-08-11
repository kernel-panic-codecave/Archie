enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "Archie"

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

// libs.versions.toml sits at the conventional gradle/libs.versions.toml location now (it used to
// live one level up, outside archie-core's own project dir, hence the old explicit
// dependencyResolutionManagement { versionCatalogs { create("libs") { from(...) } } } block - Gradle
// auto-registers it from here, so that block is gone; adding it back double-registers "libs".

// Matches terrarium-earth/Common-Storage-Lib's settings.gradle.kts layout: one nested
// <module>/<platform> directory per platform, flattened into a single-level Gradle project name
// (e.g. core/fabric -> archie-core-fabric). archie-core is the library; archie-datagen and
// archie-gametest are its dev-time-only sibling modules; archie-test is the dev-playground mod
// that exercises all three.
includeCorePlatform("common")
includeCorePlatform("fabric")
includeCorePlatform("neoforge")

includeModule("datagen", "common")
includeModule("datagen", "fabric")
includeModule("datagen", "neoforge")

includeModule("gametest", "common")
includeModule("gametest", "fabric")
includeModule("gametest", "neoforge")

includeModule("test", "common")
includeModule("test", "fabric")
includeModule("test", "neoforge")

fun includeModule(name: String, platform: String) {
	include("$name/$platform")
	project(":$name/$platform").name = "archie-$name-$platform"
}

fun includeCorePlatform(platform: String) {
	include("core/$platform")
	project(":core/$platform").name = "archie-core-$platform"
}
