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
        gradlePluginPortal()
        mavenLocal()
    }
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

includeBuild("../Archie") {
    dependencySubstitution {
        substitute(module("net.kernelpanicsoft:common")).using(project(":common"))
        substitute(module("net.kernelpanicsoft:fabric")).using(project(":fabric"))
        substitute(module("net.kernelpanicsoft:neoforge")).using(project(":neoforge"))
    }
}

include("common", "fabric", "neoforge")

rootProject.name = "Archie-Test"

project(":common").name = "common-test"
project(":fabric").name = "fabric-test"
project(":neoforge").name = "neoforge-test"

