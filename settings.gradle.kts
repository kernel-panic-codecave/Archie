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
			url = uri("https://maven.kernelpanicsoft.net/releases")
		}
		gradlePluginPortal()
	}
//	includeBuild("plugins")
}

rootProject.name = "Archie-Repo"

includeBuild("Archie")
includeBuild("Archie-Test")
