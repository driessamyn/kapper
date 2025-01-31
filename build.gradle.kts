plugins {
    kotlin("jvm")
    id("org.jetbrains.kotlinx.kover")
    alias(libs.plugins.sonar)
    alias(libs.plugins.git.semver)
}

repositories {
    mavenCentral()
}

dependencies {
    kover(project(":kapper"))
    kover(project(":kapper-coroutines"))
}

semver {
    changeLogFormat = git.semver.plugin.changelog.ChangeLogFormat.defaultChangeLog
    releasePattern = "\\\\ABuild: release(?:\\\\([^()]+\\\\))?:"
}

val projectVersion = semver.version
subprojects {
    group = "net.samyn"
    version = projectVersion
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
