pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        maven { url = uri("https://artifacts.itemis.cloud/repository/maven-mps/") }
        mavenCentral()
    }
    dependencyResolutionManagement {
        repositories {
            mavenLocal()
            gradlePluginPortal()
            maven { url = uri("https://artifacts.itemis.cloud/repository/maven-mps/") }
            mavenCentral()
        }
        versionCatalogs {
            create("coreLibs") {
                val modelixCoreVersion = file("gradle/libs.versions.toml").readLines()
                    .first { it.startsWith("modelix-core = ") }
                    .substringAfter('"')
                    .substringBefore('"')
                from("org.modelix:core-version-catalog:$modelixCoreVersion")
            }
        }
    }
}

rootProject.name = "modelix.kubernetes"

include("integration-tests")
include("keycloak-extensions")

include("gitui")
include("workspace-client-plugin")
include("workspace-job")
include("workspace-manager")
include("workspaces")
