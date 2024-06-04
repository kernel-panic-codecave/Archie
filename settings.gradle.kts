pluginManagement {
	repositories {
		maven("https://maven.fabricmc.net/")
		maven("https://maven.architectury.dev/")
		maven("https://maven.minecraftforge.net/")
		maven("https://maven.neoforged.net/releases/")
		maven("https://maven.firstdarkdev.xyz/releases")
		gradlePluginPortal()
	}
	includeBuild("plugins")
}

plugins {
	`gradle-enterprise`
	id("org.danilopianini.gradle-pre-commit-git-hooks") version "2.0.7"
}

gradleEnterprise.buildScan {
	termsOfServiceUrl = "https://gradle.com/terms-of-service"
	termsOfServiceAgree = "yes"
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
