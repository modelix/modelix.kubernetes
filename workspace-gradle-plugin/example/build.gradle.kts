// Example of a CI build that builds an MPS project and publishes the result to a Modelix workspace.
//
//   ./gradlew publishModelixWorkspaceArtifact -Pmodelix.workspaceId=<id>
//
// The access token is read from the environment variable MODELIX_ACCESS_TOKEN.
// See ../README.md for how to create one.

plugins {
    base
    id("org.modelix.mps.build-tools") version "2.1.0"
    id("org.modelix.workspaces")
}

repositories {
    mavenCentral()
    maven { url = uri("https://artifacts.itemis.cloud/repository/maven-mps/") }
}

val mpsReleaseVersion = "2024.1.1"

// Generates and compiles the modules of the MPS project in place (source_gen and classes_gen folders),
// like the build that runs inside the cluster for workspaces with the build mode INTERNAL.
mpsBuild {
    mpsVersion(mpsReleaseVersion)
    search("mps")
    publication("my-language") {
        module("my.language")
        module("my.solution")
    }
}

modelixWorkspace {
    serverUrl = providers.gradleProperty("modelix.serverUrl").orElse("http://localhost/")
    workspaceId = providers.gradleProperty("modelix.workspaceId")
    mpsVersion = mpsReleaseVersion.substringBeforeLast(".")

    mpsProject("git-import-test-repo", layout.projectDirectory.dir("mps"))
}

tasks.named("packageModelixWorkspaceArtifact") {
    dependsOn("assembleMpsModules")
}

tasks.named<Delete>("clean") {
    delete(fileTree("mps") { include("**/source_gen/**", "**/source_gen.caches/**", "**/classes_gen/**") })
}
