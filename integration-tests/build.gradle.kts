
plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation(libs.modelix.model.client)
    testImplementation(libs.kotlin.coroutines.test)
    testImplementation(libs.jgit)
    testImplementation(libs.jgit.server)
    testImplementation(libs.jetty.server)
    testImplementation(libs.jetty.servlet)
    testImplementation(libs.jakarta.servlet.api)
    testImplementation(libs.modelix.api.client.ktor)
    testImplementation(libs.modelix.authorization)
    testImplementation(libs.ktor.client.cio)
    testImplementation(libs.ktor.client.content.negotiation)
    testImplementation(libs.ktor.client.logging)
    testImplementation(libs.ktor.serialization.json)
    testImplementation(libs.kotlin.serialization.core)
    testImplementation(libs.slf4j.simple)
}

tasks {
    val exportWorkspaceManagerPrivateKey by registering(Exec::class) {
        commandLine("sh", "./export-private-key.sh")
    }
    test {
        dependsOn(exportWorkspaceManagerPrivateKey)
        project.findProperty("modelix.baseurl")?.let {
            environment("MODELIX_BASE_URL", it.toString().trimEnd('/'))
        }
    }
}
