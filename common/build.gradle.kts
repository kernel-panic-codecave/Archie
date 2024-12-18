architectury {
	common("fabric", "neoforge")
	platformSetupLoomIde()
}

loom.accessWidenerPath.set(file("src/main/resources/${project.properties["mod_id"]}.accesswidener"))

sourceSets {
	main {
		kotlin {
			srcDir("src/main/gametest")
			srcDir("src/main/datagen")
		}
		java {
			srcDir("src/main/mixin")
		}
	}
}

dependencies {
	compileOnly(libs.kotlinx.serialization)
	compileOnly(libs.kotlinx.serialization.json)
	compileOnly(kotlin("reflect"))
	api(libs.kotlinx.serialization.nbt) { isTransitive = false }
	api(libs.kotlinx.serialization.toml) { isTransitive = false }
	api(libs.kotlinx.serialization.json5) { isTransitive = false }
	api(libs.kotlinx.serialization.cbor) { isTransitive = false }
	api(compose.runtime)
	// We depend on fabric loader here to use the fabric @Environment annotations and get the mixin dependencies
	// Do NOT use other classes from fabric loader
	modImplementation(libs.fabric.loader)

    modApi(libs.rei.common)
	modApi(libs.catalogue.common)
	modApi(libs.clothConfig.common)
	modApi(libs.architectury.common)
	modApi(libs.storage.common)
	modApi(libs.storage.resources.common)
}

tasks {
	base.archivesName.set(base.archivesName.get() + "-common")
}


publishing {
	publications.create<MavenPublication>("mavenCommon") {
		artifactId = base.archivesName.get()
		from(components["java"])
	}

	repositories {
		mavenLocal()
		maven {
			val releasesRepoUrl = "https://example.com/releases"
			val snapshotsRepoUrl = "https://example.com/snapshots"
			url = uri(
				if (project.version.toString().endsWith("SNAPSHOT") || project.version.toString()
						.startsWith("0")
				) snapshotsRepoUrl else releasesRepoUrl
			)
			name = "ExampleRepo"
			credentials {
				username = project.properties["repoLogin"]?.toString()
				password = project.properties["repoPassword"]?.toString()
			}
		}
	}
}
