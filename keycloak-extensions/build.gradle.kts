
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
