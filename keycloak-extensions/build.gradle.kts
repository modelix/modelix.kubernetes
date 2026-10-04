
plugins {
    java
}

dependencies {
    implementation(libs.keycloak.core)
    implementation(libs.keycloak.server.spi)
    implementation(libs.keycloak.server.spi.private)
}

tasks.withType<Jar> {
    archiveFileName.set("keycloak-extensions.jar")
}

tasks.withType<JavaCompile> {
    // Compiled with the JDK running Gradle, which has to be at least 21 for other projects in this build.
    options.release.set(17)
}
