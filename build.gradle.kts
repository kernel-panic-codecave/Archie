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
}
