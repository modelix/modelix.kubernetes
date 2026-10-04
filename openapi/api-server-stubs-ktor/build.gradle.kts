import ch.acanda.gradle.fabrikt.FabriktGenerateTask

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.fabrikt)
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.data.conversion)
}

val bundledSpecs = project(":openapi:redocly").layout.buildDirectory.dir("bundled")

fabrikt {
    defaults {
        validationLibrary = NoValidation
        model {
            generate = enabled
            serializationLibrary = Kotlin
            includeCompanionObject = enabled
            // ignoreUnknownProperties = enabled
            extensibleEnums = enabled
        }
        controller {
            generate = enabled
            target = Ktor
            authentication = enabled
            suspendModifier = enabled
            completionStage = enabled
        }
        typeOverrides {
            uuid = String
        }
    }
    generate("mavenConnector") {
        apiFile = bundledSpecs.map { it.file("maven-connector-v1.yaml") }
        basePackage = "org.modelix.services.mavenconnector.stubs"
    }
    generate("gitConnector") {
        apiFile = bundledSpecs.map { it.file("git-connector-v1.yaml") }
        basePackage = "org.modelix.services.gitconnector.stubs"
    }
    generate("repository") {
        apiFile = bundledSpecs.map { it.file("repository-v3.yaml") }
        basePackage = "org.modelix.services.repository.stubs"
    }
    generate("workspaces") {
        apiFile = bundledSpecs.map { it.file("workspaces-v1.yaml") }
        basePackage = "org.modelix.services.workspaces.stubs"
    }
}

tasks.withType<FabriktGenerateTask> {
    dependsOn(":openapi:redocly:npm_run_bundle")
}

sourceSets.main {
    kotlin.srcDir(layout.buildDirectory.dir("generated/sources/fabrikt/src/main/kotlin"))
}

tasks.processResources {
    dependsOn(tasks.fabriktGenerate)
}
tasks.compileKotlin {
    dependsOn(tasks.fabriktGenerate)
}
