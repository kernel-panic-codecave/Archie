enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
pluginManagement {
	repositories {
		maven("https://maven.fabricmc.net/")
		maven("https://maven.architectury.dev/")
		maven("https://maven.minecraftforge.net/")
		maven("https://maven.neoforged.net/releases/")
		maven("https://maven.firstdarkdev.xyz/releases")
		maven {
			name = "kernelpanic"
			url = uri("https://repo.kernelpanicsoft.net/maven/releases")
		}
		gradlePluginPortal()
	}
//	includeBuild("plugins")
}

include("common", "fabric", "neoforge")

rootProject.name = "Archie"
