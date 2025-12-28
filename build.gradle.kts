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

tasks.check {
    dependsOn(":koverHtmlReport")
    dependsOn(":koverXmlReport")
}

// Generate all API documentation for the docs website
tasks.register("generateApiDocs") {
    group = "documentation"
    description = "Generate API documentation for all modules"
    
    dependsOn(":kapper:generateMarkdownDocs", ":kapper-coroutines:generateMarkdownDocs")
    
    doLast {
        val apiIndexFile = file("docs/api/index.md")
        apiIndexFile.writeText("""
# API Reference

## Modules

- [**kapper**](./kapper/) - Main ORM functionality
- [**kapper-coroutines**](./kapper-coroutines/) - Kotlin coroutines support

## External Links

- [GitHub Repository](https://github.com/driessamyn/kapper)
- [Maven Central](https://central.sonatype.com/artifact/net.samyn/kapper)
        """.trimIndent())
    }
}

sonar {
    properties {
        property("sonar.projectKey", "driessamyn_kapper")
        property("sonar.organization", "driessamyn")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.coverage.jacoco.xmlReportPaths", "build/reports/kover/report.xml")
        property("sonar.coverage.exclusions", "benchmark/**,examples/**")
        property("sonar.junit.reportPaths", "**/build/test-results/*/")
    }
}
