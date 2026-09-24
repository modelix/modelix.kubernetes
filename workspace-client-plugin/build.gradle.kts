
buildscript {
    dependencies {
        classpath(libs.modelix.mps.build.tools)
    }
}

plugins {
    id("org.jetbrains.kotlin.jvm")
    alias(libs.plugins.intellij.platform)
}

group = "org.modelix.mps"

kotlin {
    jvmToolchain(17)
}

repositories {
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    compileOnly(kotlin("stdlib"))
    fun ModuleDependency.excludedBundledLibraries() {
        exclude(group = "org.jetbrains.kotlin")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-jdk8")
        exclude("org.slf4j", "slf4j-api")
    }

    fun implementationWithoutBundled(dependencyNotation: Provider<*>) {
        implementation(dependencyNotation) {
            excludedBundledLibraries()
        }
    }
}

// copy and extract sync plugin
val syncPluginZip by configurations.creating {
    attributes {
        attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.ZIP_TYPE)
    }
}
dependencies {
    syncPluginZip(libs.modelix.syncPlugin3)
}
val pluginDependenciesDir = layout.buildDirectory.dir("plugin-dependencies")
sync {
    from(zipTree({ syncPluginZip.singleFile }))
    into(pluginDependenciesDir)
}
val syncPluginDir = pluginDependenciesDir.get().asFile.resolve("mps-sync-plugin3")

val supportedMPSVersions = project.properties["mpsMajorVersions"].toString().split(",").sorted()
fun String.toPlatformVersion(): String = replace(Regex("""20(\d\d)\.(\d+).*"""), "$1$2")

// The IntelliJ Platform Gradle Plugin 2.x supports 2022.3 as the oldest target platform.
// The plugin is still marked as compatible with older versions by the `sinceBuild` below.
val compileAgainstPlatformVersion = (supportedMPSVersions + "2022.3").filter { it >= "2022.3" }.min()

dependencies {
    intellijPlatform {
        intellijIdeaCommunity(compileAgainstPlatformVersion)
        localPlugin(syncPluginDir)
    }
}

// Configure IntelliJ Platform Gradle Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
intellijPlatform {
    instrumentCode = false
    buildSearchableOptions = false
    pluginConfiguration {
        ideaVersion {
            sinceBuild = supportedMPSVersions.first().toPlatformVersion()
            untilBuild = supportedMPSVersions.last().toPlatformVersion() + ".*"
        }
    }
}

// Consumed by the workspace-manager, which bundles the plugin zip.
val pluginZip by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
}
artifacts {
    add(pluginZip.name, tasks.buildPlugin)
}

tasks {
    runIde {
        systemProperty("idea.platform.prefix", "Idea")
    }
}
