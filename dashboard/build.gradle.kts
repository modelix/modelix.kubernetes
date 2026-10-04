import com.github.gradle.node.pnpm.task.PnpmTask

plugins {
    base
    alias(libs.plugins.node)
}

val generateApis by tasks.registering {
    group = "openapi tools"
}
for ((taskName, scriptName) in listOf(
    "generateWorkspacesApi" to "generate-openapi-workspaces",
    "generateMavenConnectorApi" to "generate-openapi-maven-connector",
    "generateGitConnectorApi" to "generate-openapi-git-connector",
)) {
    val task = tasks.register<PnpmTask>(taskName) {
        dependsOn(tasks.pnpmInstall)
        dependsOn(":openapi:redocly:npm_run_bundle")
        pnpmCommand = listOf("run", scriptName)
    }
    generateApis { dependsOn(task) }
}

val pnpmRunBuild = tasks.register<PnpmTask>("pnpm_run_build") {
    dependsOn(generateApis)
    pnpmCommand = listOf("run", "build")
    inputs.dir("src")
    inputs.files("index.html", "package.json", "pnpm-lock.yaml", "vite.config.ts", ".env")
    inputs.files("tsconfig.json", "tsconfig.app.json", "tsconfig.node.json")
    outputs.dir("dist")
}

tasks.assemble {
    dependsOn(pnpmRunBuild)
}
