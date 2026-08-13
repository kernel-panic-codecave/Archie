import net.fabricmc.loom.api.LoomGradleExtensionAPI
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
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
	alias(libs.plugins.modfusioner)
	alias(libs.plugins.modpublisher)
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

// Not committed - local.properties (repo root) holds reposilite.username/reposilite.password for
// developer machines; CI supplies REPOSILITE_USERNAME/REPOSILITE_PASSWORD env vars instead.
val localProperties = kotlin.runCatching {
	val localPropsFile = rootDir.resolve("local.properties")
	if (localPropsFile.exists()) loadProperties(localPropsFile.path) else null
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

	// One MavenPublication per module, published to kernelpanicsoft.net's Reposilite - archie-core/
	// -datagen/-gametest are real consumable libraries; archie-test is a dev playground, never
	// published (matches fusioner/dokka's own product/test split above).
	if (!project.name.startsWith("archie-test-")) {
		// allprojects{} (below) is what normally applies these, but it's declared after this
		// subprojects{} block and hasn't run for this project yet - apply is idempotent, so
		// re-applying here just guarantees ordering for the components["java"]/publishing{} access
		// immediately below.
		apply(plugin = "java")
		apply(plugin = "maven-publish")

		extensions.configure<PublishingExtension>("publishing") {
			publications {
				create<MavenPublication>("maven") {
					artifactId = base.archivesName.get()
					from(components["java"])
				}
			}

			repositories {
				mavenLocal()
				maven {
					name = "Reposilite"
					val releasesUrl = "https://maven.kernelpanicsoft.net/releases"
					val snapshotsUrl = "https://maven.kernelpanicsoft.net/snapshots"

					url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsUrl else releasesUrl)

					credentials {
						username = localProperties?.getProperty("reposilite.username")
							?: System.getenv("REPOSILITE_USERNAME")
						password = localProperties?.getProperty("reposilite.password")
							?: System.getenv("REPOSILITE_PASSWORD")
					}
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

// Merges only archie-core's fabric+neoforge jars into one artifact - datagen/gametest/test each
// ship as their own separate mod and are never fused/published.
fusioner {
	packageGroup = project.group.toString()
	mergedJarName = "${project.base.archivesName.get()}-merged-${libs.versions.minecraft.get()}"
	jarVersion = project.version.toString()
	outputDirectory = "build/artifacts"

	fabric {
		projectName = "archie-core-fabric"
		inputTaskName = "remapJar"
	}

	neoforge {
		projectName = "archie-core-neoforge"
		inputTaskName = "remapJar"
	}
}

publisher {
	apiKeys {
		curseforge("curseforge_api_key".localOrEnv)
		modrinth("modrinth_api_key".localOrEnv)
		github("github_token".localOrEnv)
	}

	curseID = "1029738"
	modrinthID = "archie"
	githubRepo = "https://github.com/kernel-panic-codecave/Archie"

	projectVersion = "${libs.versions.minecraft.get()}-${project.version}"
	displayName = "Archie-Merged-${projectVersion.get()}"
	gameVersions = listOf("1.21.1")
	loaders = listOf("neoforge", "fabric")
	curseEnvironment = "both"
	versionType = "alpha"
	artifact = tasks.fusejars.get()
	javaVersions = listOf(JavaVersion.VERSION_21)

	// Just this release's own notes (generateChangelog writes it below), not the whole
	// ever-growing CHANGELOG.md - modpublisher submits this file's entire content as the
	// version body, and Modrinth's version_body has a length limit CHANGELOG.md's full history
	// eventually exceeds.
	changelog = file("build/latest-changelog.md")

	curseDepends {
		required = listOf("fabric-api", "fabric-language-kotlin", "kotlinlangforge", "architectury-api", "cloth-config")
	}

	modrinthDepends {
		required = listOf("fabric-api", "fabric-language-kotlin", "kotlin-lang-forge", "architectury-api", "cloth-config")
	}
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
	build {
		finalizedBy(fusejars)
	}
	assemble {
		finalizedBy(fusejars)
	}
	named("publish") {
		dependsOn(publishMod)
	}
	register<Exec>("publishDocs") {
		dependsOn(getByName("embedDokkaIntoMkDocs"))
		group = "publishing"
		val tag = rootProject.version.toString().substringBeforeLast(".")
		workingDir = rootDir
		// --alias-type redirect: mike's default ("symlink") writes the "latest" alias as an
		// actual symlink into the gh-pages branch, which GitHub's own automatic Pages
		// build-and-deploy (triggered whenever gh-pages is pushed, separate from this task)
		// rejects outright ("content does not contain any hard links, symlinks"). "redirect"
		// makes the alias a small HTML redirect page instead - no symlink, same effect for
		// visitors.
		commandLine("mike", "deploy", "--push", "--update-aliases", "--alias-type", "redirect", tag, "latest")
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
			"--latest-path", "build/latest-changelog.md",
		)
	}
	listOf("publishCurseforge", "publishModrinth", "publishGitHub", "publishMod").forEach {
		named(it) { dependsOn(getByName("generateChangelog")) }
	}
}
