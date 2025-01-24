import net.fabricmc.loom.api.LoomGradleExtensionAPI
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.konan.properties.loadProperties

plugins {
	java
	alias(libs.plugins.architectury)
	alias(libs.plugins.architectury.kotlin)
	alias(libs.plugins.architectury.loom) apply false
	alias(libs.plugins.kotlin.jvm)
	alias(libs.plugins.kotlin.serialization)
	alias(libs.plugins.kotlin.compose)
	alias(libs.plugins.compose)
	alias(libs.plugins.dokka.mkdocs)
	alias(libs.plugins.modfusioner)
	alias(libs.plugins.modpublisher)
}

architectury.minecraft = libs.versions.minecraft.get()

val localProperties = kotlin.runCatching { loadProperties("$rootDir/local.properties") }.getOrNull()

val String.prop: String?
	get() = rootProject.properties[this]?.toString()

val String.local: String?
	get() = localProperties?.get(this)?.toString()

val String.env: String?
	get() = System.getenv(this)

val String.localOrEnv: String?
	get() = localProperties?.get(this)?.toString() ?: System.getenv(this.uppercase())

subprojects {
	apply(plugin = "dev.architectury.loom")

	val loom = project.extensions.getByName<LoomGradleExtensionAPI>("loom")

	configure<LoomGradleExtensionAPI> {
		silentMojangMappingsLicense()
	}

	repositories {
		val githubUsername = "github_actor".localOrEnv
		val githubToken = "github_token".localOrEnv
		mavenCentral()
		mavenLocal()
		maven {
			name = "kernelpanic"
			url = uri("https://repo.kernelpanicsoft.net/maven/snapshots")
		}
		maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
		maven("https://maven.parchmentmc.org")
		maven("https://maven.fabricmc.net/")
		maven("https://maven.neoforged.net/releases/")
		maven("https://maven.terraformersmc.com/releases/")
		maven("https://thedarkcolour.github.io/KotlinForForge/")
		maven("https://maven.resourcefulbees.com/repository/maven-public/")
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
	apply(plugin = "dev.opensavvy.dokka-mkdocs")
	apply(plugin = "architectury-plugin")
	apply(plugin = "maven-publish")

	version = "mod_version".prop
	group = "mod_group".prop
	base.archivesName = "mod_id".prop

	tasks.withType<JavaCompile>().configureEach {
		options.encoding = "UTF-8"
		options.release.set(21)
	}

	architectury {
		compileOnly()
	}

	dokka {
		dokkaGeneratorIsolation = ClassLoaderIsolation()
//		pluginsConfiguration.html {
//			footerMessage = "(c) 2025 Kernel Panic"
//		}
	}

	java.withSourcesJar()
}

dependencies {
	dokka(projects.common) {isTransitive = false}
	dokka(projects.fabric) {isTransitive = false}
	dokka(projects.neoforge) {isTransitive = false}
}

fusioner {
	packageGroup = project.group.toString()
	mergedJarName = "${project.base.archivesName.get()}-merged-${libs.versions.minecraft.get()}"
	jarVersion = project.version.toString()
	outputDirectory = "build/artifacts"

	fabric {
		inputTaskName = "remapJar"
	}

	neoforge {
		inputTaskName = "remapJar"
	}
}

publisher {
	apiKeys {
		curseforge("curseforge_api_key".localOrEnv)
		modrinth("modrinth_api_key".localOrEnv)
	}

	debug = true

	curseID = "1029738"
	modrinthID = "archie"

	projectVersion = "${libs.versions.minecraft.get()}-${project.version}"
	displayName = "Archie-Merged-${projectVersion.get()}"
	gameVersions = buildList {
		add("1.20.6")
	}
	loaders = buildList {
		add("neoforge")
		add("fabric")
	}
	curseEnvironment = "both"
	versionType = "alpha"
	artifact = tasks.fusejars.get()
	javaVersions = buildList {
		add(JavaVersion.VERSION_21)
	}

	changelog = file("CHANGELOG.md")

	curseDepends {
		required = buildList {
			// fabric
			add("fabric-api")
			add("fabric-language-kotlin")
			// neoforge
			add("kotlin-for-forge")
			// common
			add("architectury-api")
			add("cloth-config")
		}
	}

	modrinthDepends {
		required = buildList {
			// fabric
			add("fabric-api")
			add("fabric-language-kotlin")
			// neoforge
			add("kotlin-for-forge")
			// common
			add("architectury-api")
			add("cloth-config")
		}
	}
}


tasks {
	build {
		finalizedBy(fusejars)
	}
	assemble {
		finalizedBy(fusejars)
	}
}

