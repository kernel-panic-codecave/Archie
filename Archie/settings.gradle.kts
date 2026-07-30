import org.gradle.kotlin.dsl.maven

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

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

include("common", "fabric", "neoforge")

rootProject.name = "Archie"

