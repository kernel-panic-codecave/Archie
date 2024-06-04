package utils

import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.attributes.Attribute
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderConvertible
import org.gradle.kotlin.dsl.DependencyHandlerScope

val patchedFMLModType = Attribute.of("patchedFMLModType", Boolean::class.javaObjectType)

fun <T : ModuleDependency>  DependencyHandlerScope.kotlinForgeRuntimeLibrary(dependency: T, dependencyConfiguration: T.() -> Unit = {}) {
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

fun <T : ModuleDependency> DependencyHandlerScope.kotlinFabricRuntimeLibrary(dependency: T, dependencyConfiguration: T.() -> Unit = {})
{
    "include"(dependency) {
        isTransitive = false
        dependencyConfiguration()
    }
    "implementation"(dependency) {
        dependencyConfiguration()
    }
}

fun DependencyHandlerScope.kotlinForgeRuntimeLibrary(group: String, name: String, version: String? = null, configuration: String? = null, classifier: String? = null, ext: String? = null, dependencyConfiguration: ExternalModuleDependency.() -> Unit = {}) {
    "include"(group, name, version, configuration, classifier, ext) {
        isTransitive = false
        dependencyConfiguration()
    }
    "implementation"(group, name, version, configuration, classifier, ext) {
        dependencyConfiguration()
    }
    "localRuntime"(group, name, version, configuration, classifier, ext) {
        isTransitive = false
        dependencyConfiguration()
        attributes {
            attribute(patchedFMLModType, true)
        }
    }
}

fun DependencyHandlerScope.kotlinFabricRuntimeLibrary(group: String, name: String, version: String? = null, configuration: String? = null, classifier: String? = null, ext: String? = null, dependencyConfiguration: ExternalModuleDependency.() -> Unit = {})
{
    "include"(group, name, version, configuration, classifier, ext) {
        isTransitive = false
        dependencyConfiguration()
    }
    "implementation"(group, name, version, configuration, classifier, ext) {
        dependencyConfiguration()
    }
}

fun DependencyHandlerScope.kotlinForgeRuntimeLibrary(dependencyNotation: String, dependencyConfiguration: ExternalModuleDependency.() -> Unit = {}) {
    "include"(dependencyNotation) {
        isTransitive = false
        dependencyConfiguration()
    }
    "implementation"(dependencyNotation) {
        dependencyConfiguration()
    }
    "localRuntime"(dependencyNotation) {
        isTransitive = false
        dependencyConfiguration()
        attributes {
            attribute(patchedFMLModType, true)
        }
    }
}

fun DependencyHandlerScope.kotlinFabricRuntimeLibrary(dependencyNotation: String, dependencyConfiguration: ExternalModuleDependency.() -> Unit = {})
{
    "include"(dependencyNotation) {
        isTransitive = false
        dependencyConfiguration()
    }
    "implementation"(dependencyNotation) {
        dependencyConfiguration()
    }
}

@Suppress("UnstableApiUsage")
fun <T : Any> DependencyHandlerScope.kotlinForgeRuntimeLibrary(dependency: Provider<T>, dependencyConfiguration: ExternalModuleDependency.() -> Unit = {}) {
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
fun <T : Any> DependencyHandlerScope.kotlinFabricRuntimeLibrary(dependency: Provider<T>, dependencyConfiguration: ExternalModuleDependency.() -> Unit = {})
{
    "include"(dependency) {
        isTransitive = false
        dependencyConfiguration()
    }
    "implementation"(dependency) {
        dependencyConfiguration()
    }
}

@Suppress("UnstableApiUsage")
fun <T : Any> DependencyHandlerScope.kotlinForgeRuntimeLibrary(dependency: ProviderConvertible<T>, dependencyConfiguration: ExternalModuleDependency.() -> Unit = {}) {
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
fun <T : Any> DependencyHandlerScope.kotlinFabricRuntimeLibrary(dependency: ProviderConvertible<T>, dependencyConfiguration: ExternalModuleDependency.() -> Unit = {})
{
    "include"(dependency) {
        isTransitive = false
        dependencyConfiguration()
    }
    "implementation"(dependency) {
        dependencyConfiguration()
    }
}
