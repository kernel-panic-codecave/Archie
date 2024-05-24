package utils

import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.attributes.Attribute
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.DependencyHandlerScope

val patchedFMLModType = Attribute.of("patchedFMLModType", Boolean::class.javaObjectType)

@Suppress("UnstableApiUsage")
fun DependencyHandlerScope.kotlinForgeRuntimeLibrary(dependency: Provider<*>, dependencyConfiguration: ExternalModuleDependency.() -> Unit = {}) {
    "include"(dependency) {
        isTransitive = false
        dependencyConfiguration()
    }
    "implementation"(dependency) {
        dependencyConfiguration()
    }
    "localRuntime"(dependency) {
        isTransitive = false
        dependencyConfiguration()
        attributes {
            attribute(patchedFMLModType, true)
        }
    }
}

@Suppress("UnstableApiUsage")
fun DependencyHandlerScope.kotlinFabricRuntimeLibrary(dependency: Provider<*>, dependencyConfiguration: ExternalModuleDependency.() -> Unit = {})
{
    "include"(dependency) {
        isTransitive = false
        dependencyConfiguration()
    }
    "implementation"(dependency) {
        dependencyConfiguration()
    }
}
