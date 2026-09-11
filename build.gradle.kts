import com.hypherionmc.modfusioner.plugin.FusionerExtension
import net.fabricmc.loom.api.LoomGradleExtensionAPI
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.AbstractArchiveTask
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
	get() = localProperties?.getProperty(this) ?: System.getenv(this.uppercase())

version = "mod_version".prop ?: "0.0.1-SNAPSHOT"
group = "mod_group".prop ?: "net.kernelpanicsoft"

subprojects {
	// Stonecutter's tree/branch anchors (e.g. `:core`) are synthetic container projects with real
	// leaf projects nested under them - they must not get build plugins applied to them directly.
	if (subprojects.isNotEmpty()) return@subprojects

	apply(plugin = "dev.architectury.loom")
	apply(plugin = "net.kernelpanicsoft.actualizer")

	version = rootProject.version
	group = rootProject.group

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

		// Gradle 9 stopped bundling its own copy of the JUnit Platform launcher for
		// useJUnitPlatform() - every module needs this on the test runtime classpath now.
		"testRuntimeOnly"(rootProject.libs.junit.platform.launcher)
	}

	// One MavenPublication per module, published to kernelpanicsoft.net's Reposilite - archie-core/
	// -datagen/-gametest are real consumable libraries; archie-test is a dev playground, never
	// published (matches fusioner/dokka's own product/test split above).
	//
	// Under Stonecutter, `project.name` is just the version segment ("1.21.1") for every leaf in
	// every tree - it no longer distinguishes "test" from the rest. `project.path` still does
	// (":test:common:1.21.1" etc.), since Stonecutter nests leaves under their tree name.
	if (!project.path.startsWith(":test:")) {
		// allprojects{} (below) is what normally applies these, but it's declared after this
		// subprojects{} block and hasn't run for this project yet - apply is idempotent, so
		// re-applying here just guarantees ordering for the components["java"]/publishing{} access
		// immediately below.
		apply(plugin = "java")
		apply(plugin = "maven-publish")

		// base.archivesName only reaches its final "archie-core-fabric"-style value once this leaf's
		// own build.gradle.kts runs (module scripts execute after this subprojects{} block, and
		// allprojects{} - which seeds the "archie-core" prefix - runs after it too) - reading it here
		// would still see the base plugin's raw default ("1.21.1", from project.name). Defer until
		// this project has finished configuring.
		afterEvaluate {
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
}

allprojects {
	// Stonecutter's tree/branch anchors (e.g. `:core`) are synthetic container projects with real
	// leaf projects nested under them - they must not get build plugins applied to them directly.
	// The true root project still needs this block (e.g. for its own `publish` task).
	if (this != rootProject && subprojects.isNotEmpty()) return@allprojects

	apply(plugin = "java")
	apply(plugin = "org.jetbrains.kotlin.jvm")
	apply(plugin = "org.jetbrains.kotlin.plugin.serialization")
	apply(plugin = "org.jetbrains.kotlin.plugin.compose")
	apply(plugin = "org.jetbrains.compose")
	apply(plugin = "dev.opensavvy.dokka-mkdocs")
	apply(plugin = "architectury-plugin")
	apply(plugin = "maven-publish")


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

	// modfusioner finds each side's source project by bare Project.name (case-insensitive), searched
	// across the *entire* build - but under Stonecutter every tree (core/datagen/gametest/test) has a
	// leaf literally named "fabric" and one named "neoforge", so no name resolves uniquely to core's.
	// ":core" is an existing, globally-unique container project name - point both sides at it just to
	// satisfy modfusioner's "did we find >= 2 projects" check; `inputFile` (set below, once every
	// project has finished configuring) overrides where the actual jar is read from, resolved relative
	// to that anchor project's directory.
	fabric {
		projectName = "core"
	}

	neoforge {
		projectName = "core"
	}
}

// modfusioner reads `inputFile` as `File(<projectName's projectDir>, inputFile)` - `projectName` above
// is just an anchor, so compute the real remapJar output path here (deferred to gradle.projectsEvaluated
// so base.archivesName - and therefore the jar's real filename - has reached its final value) and
// express it relative to :core's directory.
gradle.projectsEvaluated {
	val mcVersion = libs.versions.minecraft.get()
	val coreDir = project(":core").projectDir

	fun remapJarFile(path: String) =
		(project(path).tasks.named("remapJar").get() as AbstractArchiveTask).archiveFile.get().asFile

	project.extensions.getByType<FusionerExtension>().let { fusionerExtension ->
		fusionerExtension.fabricConfiguration.inputFile =
			remapJarFile(":core:fabric:$mcVersion").relativeTo(coreDir).path
		fusionerExtension.neoforgeConfiguration.inputFile =
			remapJarFile(":core:neoforge:$mcVersion").relativeTo(coreDir).path
	}

	// The jars it reads are named through `inputFile` as bare paths, so nothing tells Gradle they
	// are this task's inputs - on a fresh checkout `fusejars` therefore runs with neither side built
	// and produces a merged jar with no loader metadata in it, which CurseForge rejects for having
	// no neoforge.mods.toml. Locally it is masked by a previous build having left the jars behind.
	tasks.named("fusejars") {
		dependsOn(":core:fabric:$mcVersion:remapJar", ":core:neoforge:$mcVersion:remapJar")
	}
}

// A `-SNAPSHOT` version publishes as an **alpha**: the suffix is dropped for `-alpha`, and the
// upload is marked alpha on both platforms. Anything else publishes as a release. So the version in
// gradle.properties stays a snapshot while a cycle is in progress and the release type follows from
// it, instead of being a second thing to remember to change at release time.
val isSnapshot = project.version.toString().endsWith("-SNAPSHOT")
val baseVersion = project.version.toString().removeSuffix("-SNAPSHOT")

/** [args] run as `git` in the repo root, as lines; empty when git is unavailable or the call fails. */
fun git(vararg args: String): List<String> = runCatching {
	val process = ProcessBuilder(listOf("git") + args).directory(rootDir).redirectErrorStream(false).start()
	process.inputStream.bufferedReader().readLines().also { process.waitFor() }
}.getOrDefault(emptyList())

/**
 * The alpha number this release gets: one past the highest already cut for [baseVersion].
 *
 * Counted from the tags rather than kept in a file, so it resets on its own the moment the base
 * version changes - `0.2.0-SNAPSHOT` starts again at `alpha1` without anyone remembering to. An
 * unnumbered `-alpha` tag is alpha 0, so the first numbered release after one is `alpha1`.
 *
 * Tags on the current commit do not count, so re-cutting a release that has not shipped yet
 * republishes the same number instead of burning one per attempt.
 */
val alphaNumber: Int by lazy {
	val head = git("rev-list", "-n", "1", "HEAD").firstOrNull()
	git("tag", "--list", "v$baseVersion-alpha*")
		.filter { tag -> head == null || git("rev-list", "-n", "1", tag).firstOrNull() != head }
		// An unnumbered `-alpha` counts as alpha 0 - the one cut before the numbering existed - so
		// the release after it is alpha1 rather than the count starting over on top of it.
		.mapNotNull { tag -> tag.substringAfterLast("-alpha").let { if (it.isEmpty()) 0 else it.toIntOrNull() } }
		.maxOrNull()?.plus(1) ?: 1
}

val releaseVersion = if (isSnapshot) "$baseVersion-alpha$alphaNumber" else baseVersion

publisher {
	apiKeys {
		curseforge("curseforge_api_key".localOrEnv)
		modrinth("modrinth_api_key".localOrEnv)
		github("github_token".localOrEnv)
	}

	curseID = "1029738"
	modrinthID = "archie"
	githubRepo = "https://github.com/kernel-panic-codecave/Archie"

	projectVersion = "${libs.versions.minecraft.get()}-$releaseVersion"
	displayName = "Archie-Merged-${projectVersion.get()}"
	gameVersions = listOf(libs.versions.minecraft.get())
	loaders = listOf("neoforge", "fabric")
	curseEnvironment = "both"
	versionType = if (isSnapshot) "alpha" else "release"
	artifact = tasks.fusejars.get()
	javaVersions = listOf(JavaVersion.VERSION_21)

	changelog = file("build/latest-changelog.md")

	curseDepends {
		required = listOf("fabric-api", "fabric-language-kotlin", "kotlinlangforge", "architectury-api", "cloth-config")
	}

	modrinthDepends {
		required = listOf("fabric-api", "fabric-language-kotlin", "kotlin-lang-forge", "architectury-api", "cloth-config")
	}
}

dependencies {
	val mcVersion = libs.versions.minecraft.get()
	listOf("core", "datagen", "gametest").forEach { tree ->
		listOf("common", "fabric", "neoforge").forEach { branch ->
			dokka(project(":$tree:$branch:$mcVersion")) { isTransitive = false }
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
	named("publish") {
		dependsOn(publishMod)
	}
	register<Exec>("publishDocs") {
		dependsOn(getByName("embedDokkaIntoMkDocs"))
		group = "publishing"
		val tag = rootProject.version.toString().substringBeforeLast(".")
		workingDir = rootDir
		commandLine("mike", "deploy", "--push", "--update-aliases", "--alias-type", "redirect", tag, "latest")
	}
	register<Exec>("generateChangelog") {
		group = "publishing"
		workingDir = rootDir
		val latestChangelog = rootDir.resolve("build/latest-changelog.md")
		doFirst { latestChangelog.parentFile.mkdirs() }
		// The script writes nothing at all when the range comes out empty - a tag sitting on the
		// commit it was cut from, most obviously - and the publisher then fails on a changelog file
		// that does not exist. A release with nothing to report is still a release.
		doLast { if (!latestChangelog.exists()) latestChangelog.writeText("No changes recorded for this release.\n") }
		commandLine(
			"python3", ".github/scripts/generate_release_notes.py",
			"--repo", "mod_source".prop!!.removePrefix("https://github.com/"),
			"--new-tag", "v$releaseVersion",
			"--range-end", "HEAD",
			"--changelog-path", "CHANGELOG.md",
			"--latest-path", "build/latest-changelog.md",
		)
	}
	listOf("publishCurseforge", "publishModrinth", "publishGitHub", "publishMod").forEach {
		named(it) { dependsOn(getByName("generateChangelog")) }
	}
}
