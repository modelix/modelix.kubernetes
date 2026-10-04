plugins {
    base
    alias(libs.plugins.node)
}

val bundle = tasks.named("npm_run_bundle") {
    inputs.dir(layout.projectDirectory.dir("../specifications"))
    inputs.dir(layout.projectDirectory.dir("redocly-plugins"))
    inputs.file(layout.projectDirectory.file("redocly.yaml"))
    outputs.dir(layout.buildDirectory.dir("bundled"))
}

tasks.assemble {
    dependsOn(bundle)
}
