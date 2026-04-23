plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation("com.diffplug.spotless:com.diffplug.spotless.gradle.plugin:6.25.0")
    implementation("io.quarkus:io.quarkus.gradle.plugin:3.9.0")
}
