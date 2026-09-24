description = "Gradle plugin for publishing the result of a CI build as a Modelix workspace"

plugins {
    `java-gradle-plugin`
    `maven-publish`
}

group = "org.modelix"

java {
    // Runs inside the builds of users. Stay compatible with older Java and Gradle versions.
    // Implemented in Java instead of Kotlin to avoid conflicts with the Kotlin version embedded in Gradle.
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
    withSourcesJar()
}

tasks.compileJava {
    options.release = 11
}

gradlePlugin {
    plugins {
        create("modelixWorkspaces") {
            id = "org.modelix.workspaces"
            implementationClass = "org.modelix.workspaces.gradle.ModelixWorkspacesPlugin"
            displayName = "Modelix Workspaces"
            description = "Packages the result of a CI build of an MPS project and uploads it to a Modelix workspace"
        }
    }
}

dependencies {
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(gradleTestKit())
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    repositories {
        if (project.hasProperty("artifacts.itemis.cloud.user")) {
            maven {
                name = "itemis"
                url = if (version.toString().contains("SNAPSHOT")) {
                    uri("https://artifacts.itemis.cloud/repository/maven-mps-snapshots/")
                } else {
                    uri("https://artifacts.itemis.cloud/repository/maven-mps-releases/")
                }
                credentials {
                    username = project.findProperty("artifacts.itemis.cloud.user").toString()
                    password = project.findProperty("artifacts.itemis.cloud.pw").toString()
                }
            }
        }
    }
}
