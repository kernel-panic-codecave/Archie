plugins {
    base
}

allprojects {
    repositories {
        mavenCentral()
        mavenLocal()
    }
}

tasks {
    check {
        dependsOn(gradle.includedBuild("Archie").task(":check"))
        dependsOn(gradle.includedBuild("Archie-Test").task(":check"))
    }

    val syncRunConfigurations by registering {
        group = "ide"
        description = "Regenerates each included build's run configurations, then copies them up to the composite root's .idea folder."

        dependsOn(gradle.includedBuild("Archie").task(":fabric:ideaSyncTask"))
        dependsOn(gradle.includedBuild("Archie").task(":neoforge:ideaSyncTask"))
        dependsOn(gradle.includedBuild("Archie-Test").task(":fabric-test:ideaSyncTask"))
        dependsOn(gradle.includedBuild("Archie-Test").task(":neoforge-test:ideaSyncTask"))

        doLast {
            val targetDir = file(".idea/runConfigurations").apply { mkdirs() }
            targetDir.listFiles { f -> f.extension == "xml" }?.forEach { it.delete() }

            val nameAttr = Regex("""name="([^"]*)" type="Application"""")
            listOf("Archie", "Archie-Test").forEach { includedBuildName ->
                val sourceDir = file("$includedBuildName/.idea/runConfigurations")
                if (!sourceDir.isDirectory) return@forEach
                sourceDir.listFiles { f -> f.extension == "xml" }?.forEach { source ->
                    val rewritten = source.readText()
                        .replace("\$PROJECT_DIR\$/", "\$PROJECT_DIR\$/$includedBuildName/")
                        .replace("name=\"Minecraft ", "name=\"$includedBuildName ")

                    val displayName = nameAttr.find(rewritten)?.groupValues?.get(1) ?: source.nameWithoutExtension
                    val sanitized = displayName.map { if (it.isLetterOrDigit()) it else '_' }.joinToString("")

                    file("$targetDir/$sanitized.xml").writeText(rewritten)
                }
            }
        }
    }

    task("ideaSyncTask") {
        dependsOn(syncRunConfigurations)
    }
}
