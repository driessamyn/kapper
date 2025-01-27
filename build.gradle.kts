plugins {
//    alias(libs.plugins.git.semver)
    kotlin("jvm")
    id("org.jetbrains.kotlinx.kover")
}

repositories {
    mavenCentral()
}

dependencies {
    kover(project(":kapper"))
    kover(project(":kapper-coroutines"))
}

tasks.build {
    dependsOn(":koverHtmlReport")
    dependsOn(":koverXmlReport")
}