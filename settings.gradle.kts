pluginManagement {
	repositories {
		maven("https://maven.fabricmc.net/")
		maven("https://maven.architectury.dev/")
		maven("https://maven.minecraftforge.net/")
		maven("https://maven.neoforged.net/releases/")
		maven("https://maven.firstdarkdev.xyz/releases")
		maven("https://maven.milosworks.xyz/releases")
		gradlePluginPortal()
	}
//	includeBuild("plugins")
}

plugins {
	id("org.danilopianini.gradle-pre-commit-git-hooks") version "2.0.7"
}

gitHooks {
	hook("prepare-commit-msg") {
		from {
			"""
                exec < /dev/tty && npx cz --hook || true
            """.trimIndent()
		}
	}
	createHooks()
}

include("common", "fabric", "neoforge")

rootProject.name = "Archie"
