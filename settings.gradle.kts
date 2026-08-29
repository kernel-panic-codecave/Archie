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

stonecutter {
	for (tree in listOf("core", "datagen", "gametest", "test")) create(tree) {
		branch("common") { versions("1.21.1") }
		branch("fabric") { versions("1.21.1") }
		branch("neoforge") { versions("1.21.1") }
	}
}

rootProject.name = "Archie"
