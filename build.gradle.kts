plugins {
//    alias(libs.plugins.git.semver)
    kotlin("jvm")
    id("org.jetbrains.kotlinx.kover")
    alias(libs.plugins.sonar)
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

sonar {
    properties {
        property("sonar.projectKey", "driessamyn_kapper")
        property("sonar.organization", "driessamyn")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.coverage.jacoco.xmlReportPaths", "build/reports/kover/report.xml")
        property("sonar.junit.reportPaths", "**/build/test-results/*/")
    }
}
