import org.gradle.api.internal.artifacts.DefaultModuleIdentifier
import org.gradle.api.internal.artifacts.dependencies.DefaultMinimalDependency
import org.gradle.api.internal.artifacts.dependencies.DefaultMutableVersionConstraint

plugins {
    `kotlin-dsl`
    `kotlin-dsl-precompiled-script-plugins`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
    maven { url = uri("https://maven.architectury.dev/") }
    maven { url = uri("https://maven.fabricmc.net/") }
    maven { url = uri("https://maven.minecraftforge.net/") }
    maven { url = uri("https://maven.blamejared.com/") }
}

dependencies {
    // plugins
    // https://github.com/gradle/gradle/issues/15383#issuecomment-1855984127
    implementation(libs.plugins.kotlin.jvm.toLibrary())
    implementation(libs.plugins.architectury.toLibrary())
    implementation(libs.plugins.architectury.loom.toLibrary())
    implementation(libs.plugins.shadow.toLibrary())
}

sourceSets

// support declaring dependencies on libs.plugins.*

fun ProviderConvertible<PluginDependency>.toLibrary() = asProvider().toLibrary()

fun Provider<PluginDependency>.toLibrary() = get().toLibrary()

fun PluginDependency.toLibrary() = DefaultMinimalDependency(
    DefaultModuleIdentifier.newId(pluginId, "$pluginId.gradle.plugin"),
    DefaultMutableVersionConstraint(version),
)
