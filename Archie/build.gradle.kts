import net.fabricmc.loom.api.LoomGradleExtensionAPI
import org.gradle.api.publish.PublishingExtension
import org.jetbrains.kotlin.konan.properties.loadProperties
import org.gradle.api.publish.maven.MavenPublication
import java.util.Properties

plugins {
	java
	alias(libs.plugins.architectury)
	id("net.kernelpanicsoft.actualizer") version "0.1.0" apply false
//	alias(libs.plugins.architectury.kotlin)
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
	apply(plugin = "maven-publish")
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
		maven {
			name = "kernelpanic"
			url = uri("https://maven.kernelpanicsoft.net/releases")
		}
		maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
		maven("https://maven.parchmentmc.org")
		maven("https://maven.fabricmc.net/")
		maven("https://maven.neoforged.net/releases/")
		maven("https://maven.terraformersmc.com/releases/")
//		maven("https://thedarkcolour.github.io/KotlinForForge/")
		maven("https://repo.nyon.dev/releases")
		maven("https://maven.isxander.dev/releases") {
			name = "Xander Maven"
		}
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

	extensions.configure<PublishingExtension>("publishing") {
		publications {
			create<MavenPublication>("mavenJava") {
				artifactId = "${"mod_id".prop}-${base.archivesName.get()}"
				from(components["java"])
			}
		}

		repositories {
			mavenLocal()
			maven {
				name = "kernelpanicReleases"
				url = uri("https://maven.kernelpanicsoft.net/releases")
				credentials {
					username = "repoLogin".localOrEnv
						?: "maven_username".localOrEnv
						?: "maven_user".localOrEnv
					password = "repoPassword".localOrEnv
						?: "maven_password".localOrEnv
						?: "maven_pass".localOrEnv
				}
			}
		}
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

	version = "mod_version".prop!!
	group = "mod_group".prop!!
	base.archivesName = "mod_id".prop!!

	tasks.withType<JavaCompile>().configureEach {
		options.encoding = "UTF-8"
		options.release.set(21)
	}

	kotlin {
		compilerOptions {
			freeCompilerArgs.add("-Xexpect-actual-classes")
			freeCompilerArgs.add("-Xcontext-parameters")
		}
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
	dokka(project(":common")) { isTransitive = false }
	dokka(project(":fabric")) { isTransitive = false }
	dokka(project(":neoforge")) { isTransitive = false }
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

tasks {
	build {
		finalizedBy(fusejars)
	}
	assemble {
		finalizedBy(fusejars)
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
	gameVersions = listOf("1.21.1")
	loaders = listOf("neoforge", "fabric")
	curseEnvironment = "both"
	versionType = "alpha"
	artifact = tasks.fusejars.get()
	javaVersions = listOf(JavaVersion.VERSION_21)

	changelog = file("CHANGELOG.md")

	curseDepends {
		required = listOf("fabric-api", "fabric-language-kotlin", "kotlin-for-forge", "architectury-api", "cloth-config")
	}

	modrinthDepends {
		required = listOf("fabric-api", "fabric-language-kotlin", "kotlin-for-forge", "architectury-api", "cloth-config")
	}
}

tasks {
	register<Exec>("publishDocs") {
		dependsOn(getByName("embedDokkaIntoMkDocs"))
		group = "publishing"
		val tag = rootProject.version.toString().substringBeforeLast(".")
		workingDir = rootDir
		commandLine("mike", "deploy", "--push", "--update-aliases", tag, "latest")
	}
}
