import groovy.lang.Closure
import net.fabricmc.loom.api.LoomGradleExtensionAPI
import org.jetbrains.kotlin.konan.properties.loadProperties

plugins {
	alias(libs.plugins.architectury)
	alias(libs.plugins.architectury.loom) apply false
	java
	alias(libs.plugins.kotlin.jvm)
	alias(libs.plugins.kotlin.serialization)
	id("com.hypherionmc.modutils.modfusioner") version "1.0.+"
}

architectury.minecraft = libs.versions.minecraft.get()

val localProperties = loadProperties("$rootDir/local.properties")

subprojects {
	apply(plugin = "dev.architectury.loom")

	val loom = project.extensions.getByName<LoomGradleExtensionAPI>("loom")
	loom.silentMojangMappingsLicense()

	repositories {
		val github_username: String by localProperties
		val github_token: String by localProperties
		mavenCentral()
		mavenLocal()
		maven("https://maven.parchmentmc.org")
		maven("https://maven.fabricmc.net/")
		maven("https://maven.neoforged.net/releases/")
		maven("https://maven.terraformersmc.com/releases/")
		maven("https://thedarkcolour.github.io/KotlinForForge/")
		maven("https://maven.resourcefulbees.com/repository/maven-public/")
		maven {
			url = uri("https://maven.pkg.github.com/MrCrayfish/Maven")
			credentials {
				username = github_username
				password = github_token
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
	apply(plugin = "architectury-plugin")
	apply(plugin = "maven-publish")

	version = project.properties["mod_version"] as String
	group = project.properties["maven_group"] as String
	base.archivesName.set(project.properties["archives_base_name"] as String)

	tasks.withType<JavaCompile>().configureEach {
		options.encoding = "UTF-8"
		options.release.set(21)
	}

	java.withSourcesJar()
}

fun <T : Any> closureOf1(action: T.() -> Unit): Closure<T?> = KotlinClosure1<T, T>({
	action(this)
	this
}, this, this)

fusioner {

	packageGroup = project.group.toString()
	mergedJarName = "${project.base.archivesName.get()}-merged-${libs.versions.minecraft.get()}"
	outputDirectory = "build/artifacts"

	fabric(closureOf1 {
		projectName = "fabric"
		inputTaskName = "remapJar"
	})

	custom(closureOf1 {
		projectName = "neoforge"
		inputTaskName = "remapJar"
	})
}


tasks {
	build {
		finalizedBy(fusejars)
	}
	assemble {
		finalizedBy(fusejars)
	}
}

