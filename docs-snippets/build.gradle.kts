plugins {
    kotlin("jvm")
    id("org.jlleitschuh.gradle.ktlint")
}

repositories {
    mavenCentral()
}

// Snippets are compiled, never published. They exist so the docs cannot rot:
// a snippet that stops compiling fails `./gradlew check`.
dependencies {
    implementation(project(":kapper"))
    implementation(project(":kapper-coroutines"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.hikari)
}
