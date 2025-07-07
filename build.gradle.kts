plugins {
    base
    alias(libs.plugins.gitVersion)
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

version = computeVersion()
description = "Helm Chart to run MPS and Modelix components in the cloud with Kubernetes"

fun computeVersion(): Any {
    val versionFile = file("version.txt")
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

allprojects {
    repositories {
        mavenCentral()
        maven { url = uri("https://artifacts.itemis.cloud/repository/maven-mps/") }
        mavenLocal()
    }
}

subprojects {
    version = rootProject.version

    repositories {
        mavenLocal()
        maven { url = uri("https://artifacts.itemis.cloud/repository/maven-mps/") }
        mavenCentral()
    }
}

val dashboard by configurations.creating
dependencies {
    dashboard(libs.modelix.dashboard)
}

val copyDashboard by tasks.registering(Sync::class) {
    from(zipTree({ dashboard.singleFile }))
    into(layout.projectDirectory.dir("proxy/dashboard"))
}

tasks.assemble {
    dependsOn(copyDashboard)
}
