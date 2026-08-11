import net.fabricmc.loom.api.LoomGradleExtensionAPI
import org.jetbrains.kotlin.konan.properties.loadProperties

plugins {
	java
	alias(libs.plugins.architectury)
	id("net.kernelpanicsoft.actualizer") version "0.1.0" apply false
	alias(libs.plugins.architectury.loom) apply false
	alias(libs.plugins.kotlin.jvm)
	alias(libs.plugins.kotlin.serialization)
	alias(libs.plugins.kotlin.compose)
	alias(libs.plugins.compose)
	alias(libs.plugins.dokka.mkdocs)
}

architectury.minecraft = libs.versions.minecraft.get()

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

val String.localOrEnv: String?
	get() = System.getenv(this.uppercase())

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
	apply(plugin = "dev.opensavvy.dokka-mkdocs")
	apply(plugin = "architectury-plugin")

	version = "mod_version".prop ?: "0.0.1-SNAPSHOT"
	group = "mod_group".prop ?: "net.kernelpanicsoft"
	base.archivesName = "archie-core"

	tasks.withType<JavaCompile>().configureEach {
		options.encoding = "UTF-8"
		options.release.set(21)
	}

	kotlin {
		compilerOptions {
			freeCompilerArgs.add("-Xexpect-actual-classes")
		}
	}

	architectury {
		compileOnly()
	}

	dokka {
		dokkaGeneratorIsolation = ClassLoaderIsolation()
	}

	java.withSourcesJar()
}

dependencies {
	dokka(project(":archie-core-common")) { isTransitive = false }
	dokka(project(":archie-core-fabric")) { isTransitive = false }
	dokka(project(":archie-core-neoforge")) { isTransitive = false }
	dokka(project(":archie-datagen-common")) { isTransitive = false }
	dokka(project(":archie-datagen-fabric")) { isTransitive = false }
	dokka(project(":archie-datagen-neoforge")) { isTransitive = false }
	dokka(project(":archie-gametest-common")) { isTransitive = false }
	dokka(project(":archie-gametest-fabric")) { isTransitive = false }
	dokka(project(":archie-gametest-neoforge")) { isTransitive = false }
}

tasks {
	register<Exec>("publishDocs") {
		dependsOn(getByName("embedDokkaIntoMkDocs"))
		group = "publishing"
		val tag = rootProject.version.toString().substringBeforeLast(".")
		workingDir = rootDir
		commandLine("mike", "deploy", "--push", "--update-aliases", tag, "latest")
	}
	// modpublisher's changelog reads CHANGELOG.md straight off disk when a publish task runs - it
	// doesn't know about git tags or PRs. .github/workflows/release-notes.yaml (reactive, post-tag)
	// can't help here: by the time it would generate this release's entry, the publish task attached
	// to the tag has already read (and shipped) whatever was on disk before. This task closes that
	// gap by generating CHANGELOG.md synchronously - see .github/scripts/generate_release_notes.py's
	// module docstring for the two call shapes.
	register<Exec>("generateChangelog") {
		group = "publishing"
		workingDir = rootDir
		commandLine(
			"python3", ".github/scripts/generate_release_notes.py",
			"--repo", "mod_source".prop!!.removePrefix("https://github.com/"),
			"--new-tag", "v${project.version}",
			"--range-end", "HEAD",
			"--changelog-path", "CHANGELOG.md",
		)
	}
}
