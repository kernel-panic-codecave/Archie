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

includeModule("core")

includeModule("datagen")

includeModule("gametest")

includeModule("test")

fun includeModulePlatform(name: String, platform: String) {
	include("$name/$platform")
	project(":$name/$platform").name = "archie-$name-$platform"
}

fun includeModule(name: String) {
	includeModulePlatform(name, "common")
	includeModulePlatform(name, "fabric")
	includeModulePlatform(name, "neoforge")
}
