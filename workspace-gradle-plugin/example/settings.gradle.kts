pluginManagement {
    // Uses the Modelix workspaces plugin from the sources of this repository instead of a published version
    includeBuild("../..")
    repositories {
        mavenLocal()
        gradlePluginPortal()
        maven { url = uri("https://artifacts.itemis.cloud/repository/maven-mps/") }
        mavenCentral()
    }
}

rootProject.name = "workspace-gradle-plugin-example"
