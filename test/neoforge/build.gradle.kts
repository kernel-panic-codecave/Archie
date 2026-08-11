import net.kernelpanicsoft.archie.plugin.bundleMod

plugins {
	alias(libs.plugins.shadow)
	alias(libs.plugins.archie)
}

architectury {
	platformSetupLoomIde()
	neoForge()
}

actualizer {
	actualizes(project(":archie-test-common"))
}

configurations {
	create("common")
	create("shadowCommon")
	configureEach {
		// Keep NeoForge Kotlin runtime provided by KotlinLangForge only.
		exclude(group = "thedarkcolour", module = "kotlinforforge-neoforge")
		exclude(group = "remapped.thedarkcolour", module = "kotlinforforge-neoforge-1d1bcbf2")
	}
	compileClasspath.get().extendsFrom(configurations["common"])
	runtimeClasspath.get().extendsFrom(configurations["common"])
	testCompileClasspath.get().extendsFrom(compileClasspath.get())
	testRuntimeClasspath.get().extendsFrom(runtimeClasspath.get())
}

loom {
	log4jConfigs.from(project(":archie-test-common").loom.log4jConfigs)
	accessWidenerPath.set(project(":archie-test-common").loom.accessWidenerPath)

	mods {
		maybeCreate("main").apply {
			sourceSet(sourceSets.main.get())
		}
	}

	runs {
		getByName("client") {
			name = "Minecraft Client"
			source(sourceSets.main.get())
			vmArgs("-XX:+AllowEnhancedClassRedefinition")
			property("kotlinx.coroutines.debug", "off")
		}
		getByName("server") {
			name = "Minecraft Server"
			source(sourceSets.main.get())
			property("kotlinx.coroutines.debug", "off")
			vmArgs("-XX:+AllowEnhancedClassRedefinition")
		}
		create("datagen") {
			data()
			name = "Minecraft Datagen"
			property("archie.datagen", "true")
			property("archie.datagen.client", providers.gradleProperty("client_datagen").orElse("true").get())
			property("archie.datagen.server", providers.gradleProperty("server_datagen").orElse("true").get())
			property("kotlinx.coroutines.debug", "off")
			programArgs("--all", "--mod", "archie_test")
			programArgs("--output", file("src/main/generated").absolutePath)
		}

		create("gametest") {
			server()
			name = "Minecraft GameTest"
			property("neoforge.enableGameTest", "true")
			property("neoforge.gameTestServer", "true")
			property("archie.gametest", "true")
			property("archie.gametest.modid", "archie_test")
			property("kotlinx.coroutines.debug", "off")
			providers.gradleProperty("archie.junit.gametest.function").orNull?.let { property("archie.junit.gametest.function", it) }
		}

		create("gametestClient") {
			client()
			name = "Minecraft GameTest Client"
			property("neoforge.enableGameTest", "true")
			property("archie.gametest.side", "client")
			property("archie.gametest", "true")
			property("archie.gametest.modid", "archie_test")
			property("kotlinx.coroutines.debug", "off")
			providers.gradleProperty("archie.junit.gametest.function").orNull?.let { property("archie.junit.gametest.function", it) }
		}
	}
}

sourceSets {
	main {
		resources {
			srcDir("src/main/generated")
		}
	}
}

dependencies {
	"neoForge"(libs.neoforge)
	modApi(libs.architectury.neoforge)
	implementation(libs.kotlin.neoforge)
	modRuntimeOnly(libs.rei.neoforge)
	modRuntimeOnly(libs.catalogue.neoforge)
	modRuntimeOnly(libs.clothConfig.neoforge)
	bundleMod(libs.storage.neoforge) { exclude(group = "curse.maven") }

	implementation(libs.junit.jupiter.api)
	testImplementation(libs.junit.jupiter.api)
	testRuntimeOnly(libs.junit.jupiter.engine)

	"common"(project(":archie-test-common", "namedElements")) { isTransitive = false }
	"shadowCommon"(project(":archie-test-common", "transformProductionNeoForge")) { isTransitive = false }
	modApi(project(":archie-core-neoforge"))
	modApi(project(":archie-datagen-neoforge"))
	modApi(project(":archie-gametest-neoforge"))
}

modResources {
	filesMatching.add("META-INF/neoforge.mods.toml")
}

tasks {
	base.archivesName.set(base.archivesName.get() + "-test-neoforge")

	test {
		useJUnitPlatform()
	}

	processResources {
		from(project(":archie-test-common").sourceSets.main.get().resources) {
			include("assets/archie_test/**")
			include("data/archie_test/**")
			include("archie_test.common.json")
			include("archie_test.accesswidener")
		}
		dependsOn(processTestResources)
	}

	processTestResources {
	}

	classes {
		finalizedBy(testClasses)
	}

	shadowJar {
		exclude("fabric.mod.json")
		configurations =
			listOf(project.configurations.getByName("shadowCommon"), project.configurations.getByName("shadow"))
		archiveClassifier.set("dev-shadow")
	}

	remapJar {
		inputFile.set(shadowJar.get().archiveFile)
		atAccessWideners.set(setOf(loom.accessWidenerPath.get().asFile.name))
		dependsOn(shadowJar)
	}

	jar.get().archiveClassifier.set("dev")

	jar {
		duplicatesStrategy = DuplicatesStrategy.EXCLUDE
		from(project(":archie-test-common").sourceSets.main.get().output)
	}

	sourcesJar {
		val commonSources = project(":archie-test-common").tasks.sourcesJar
		dependsOn(commonSources)
		duplicatesStrategy = DuplicatesStrategy.EXCLUDE
		from(commonSources.get().archiveFile.map { zipTree(it) })
	}
}
