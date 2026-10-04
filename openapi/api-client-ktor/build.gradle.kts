import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.openapi.generator)
}

val generatorTask = tasks.register("generateKtorClient", GenerateTask::class) {
    dependsOn(":openapi:redocly:npm_run_bundle")
    group = "openapi tools"
    configOptions = mapOf(
        "library" to "jvm-ktor",
        "serializationLibrary" to "kotlinx_serialization",
    )
    gitUserId = "modelix"
    gitRepoId = "modelix.kubernetes"
    generatorName = "kotlin"
    inputSpec = project(":openapi:redocly").layout.buildDirectory.file("bundled/git-connector-v1.yaml")
    outputDir = layout.buildDirectory.dir("generated/ktor")
}

sourceSets.main {
    kotlin.srcDir(layout.buildDirectory.dir("generated/ktor/src/main/kotlin"))
}

dependencies {
    api(libs.ktor.client.core)
    implementation(libs.ktor.client.content.negotiation)
}

tasks.compileKotlin {
    dependsOn(generatorTask)
}
