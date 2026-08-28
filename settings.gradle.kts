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

plugins {
	id("dev.kikugie.stonecutter") version "0.9.7"
}

// Validation slice: only `core` is Stonecutter-managed for now. datagen/gametest/test stay on the
// old includeModule() scheme until this is proven out - see the migration plan.
stonecutter {
	create("core") {
		branch("common") { versions("1.21.1") }
		branch("fabric") { versions("1.21.1") }
		branch("neoforge") { versions("1.21.1") }
	}
}

rootProject.name = "Archie"

// includeModule("datagen")
// includeModule("gametest")
// includeModule("test")

fun includeModulePlatform(name: String, platform: String) {
	include("$name/$platform")
	project(":$name/$platform").name = "archie-$name-$platform"
}

fun includeModule(name: String) {
	includeModulePlatform(name, "common")
	includeModulePlatform(name, "fabric")
	includeModulePlatform(name, "neoforge")
}
