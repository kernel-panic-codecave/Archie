import net.fabricmc.loom.api.LoomGradleExtensionAPI
import org.jetbrains.kotlin.konan.properties.loadProperties
import java.util.Properties
import kotlin.collections.forEach

plugins {
    java
    alias(libs.plugins.architectury)
    id("net.kernelpanicsoft.actualizer") version "0.1.0" apply false
//    alias(libs.plugins.architectury.kotlin)
    alias(libs.plugins.architectury.loom) apply false
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose)
}

architectury.minecraft = libs.versions.minecraft.get()

val localProperties = kotlin.runCatching {
    val localPropsFile = rootDir.resolve("local.properties")
    val sharedPropsFile = rootDir.resolve("../local.properties")
    when {
        localPropsFile.exists() -> loadProperties(localPropsFile.path)
        sharedPropsFile.exists() -> loadProperties(sharedPropsFile.path)
        else -> null
    }
}.getOrNull()

val sharedProperties = kotlin.runCatching {
    val localPropsFile = rootDir.resolve("gradle.properties")
    val sharedPropsFile = rootDir.resolve("../gradle.properties")
    when {
        localPropsFile.exists() -> loadProperties(localPropsFile.path)
        sharedPropsFile.exists() -> loadProperties(sharedPropsFile.path)
        else -> null
    }
}.getOrNull()

val String.prop: String?
    get() = sharedProperties?.get(this)?.toString()

val String.local: String?
    get() = localProperties?.get(this)?.toString()

val String.env: String?
    get() = System.getenv(this)

val String.localOrEnv: String?
    get() = localProperties?.get(this)?.toString() ?: System.getenv(this.uppercase())

subprojects {
    apply(plugin = "dev.architectury.loom")
    apply(plugin = "net.kernelpanicsoft.actualizer")

    val loom = project.extensions.getByName<LoomGradleExtensionAPI>("loom")

    configure<LoomGradleExtensionAPI> {
        silentMojangMappingsLicense()
    }

    repositories {
        val githubUsername = "github_actor".localOrEnv
        val githubToken = "github_token".localOrEnv
        mavenCentral()
        mavenLocal()
        google {
            content {
                includeGroupByRegex("androidx\\..*")
                includeGroupByRegex("com\\.android.*")
            }
        }
        maven {
            name = "kernelpanic releases"
            url = uri("https://maven.kernelpanicsoft.net/releases")
        }
        maven {
            name = "kernelpanic snapshots"
            url = uri("https://maven.kernelpanicsoft.net/snapshots")
        }
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://maven.parchmentmc.org")
        maven("https://maven.fabricmc.net/")
        maven("https://maven.neoforged.net/releases/")
        maven("https://maven.terraformersmc.com/releases/")
        maven("https://repo.nyon.dev/releases")
        maven("https://maven.isxander.dev/releases") {
            name = "Xander Maven"
        }
        maven("https://maven.resourcefulbees.com/repository/maven-public/") {
            content {
                includeGroup("earth.terrarium.common_storage_lib")
            }
        }
        maven {
            url = uri("https://maven.pkg.github.com/MrCrayfish/Maven")
            credentials {
                username = githubUsername
                password = githubToken
            }
        }
        maven {
            url = uri("https://www.cursemaven.com")
            content {
                includeGroup("curse.maven")
            }
        }
    }

    @Suppress("UnstableApiUsage")
    dependencies {
        "minecraft"(rootProject.libs.minecraft)
        "mappings"(loom.layered {
            officialMojangMappings()
            parchment(rootProject.libs.parchment)
        })

        compileOnly("org.jetbrains:annotations:24.1.0")
    }
}

allprojects {
    apply(plugin = "java")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.plugin.serialization")
    apply(plugin = "org.jetbrains.kotlin.plugin.compose")
    apply(plugin = "org.jetbrains.compose")
    apply(plugin = "architectury-plugin")
    apply(plugin = "maven-publish")

    version = "mod_version".prop!!
    group = "mod_group".prop!!
    base.archivesName = "mod_id".prop!!.replace("_", "-")

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(21)
    }

    architectury {
        compileOnly()
    }

    java.withSourcesJar()
}

tasks {
    check {
        dependsOn(project(":common-test").tasks.check)
        dependsOn(project(":fabric-test").tasks.check)
        dependsOn(project(":neoforge-test").tasks.check)
    }
}

