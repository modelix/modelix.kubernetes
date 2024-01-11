plugins {
    alias(libs.plugins.gitVersion)
}

version = computeVersion()
description = "Helm Chart to run MPS and Modelix components in the cloud with Kubernetes"

fun computeVersion(): Any {
    val versionFile = file("helm-chart-version.txt")
    return if (versionFile.exists()) {
        versionFile.readText().trim()
    } else {
        val gitVersion: groovy.lang.Closure<String> by extra
        val version = gitVersion()
        // Make versions like 0.0.1.dirty valid semantic versions by changing them to 0.0.1-dirty
        val semanticVersion = version.replace("""\.dirty$""".toRegex(), "-dirty")
        versionFile.writeText(semanticVersion)
    }
}

subprojects {
    repositories {
        // It is useful to have the central maven repo before the Itemis's one as it is more reliable
        mavenCentral()
        maven { url = uri("https://artifacts.itemis.cloud/repository/maven-mps/") }
    }
}